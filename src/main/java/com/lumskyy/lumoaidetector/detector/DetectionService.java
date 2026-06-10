package com.lumskyy.lumoaidetector.detector;

import com.lumskyy.lumoaidetector.config.PluginSettings;
import com.lumskyy.lumoaidetector.dataset.DatasetCsv;
import com.lumskyy.lumoaidetector.dataset.RecordingService;
import com.lumskyy.lumoaidetector.message.MessageService;
import com.lumskyy.lumoaidetector.ml.ModelService;
import com.lumskyy.lumoaidetector.ml.PredictionResult;
import com.lumskyy.lumoaidetector.platform.Platform;
import com.lumskyy.lumoaidetector.storage.StatsService;
import com.lumskyy.lumoaidetector.util.Formats;
import java.lang.reflect.Method;
import java.util.ArrayDeque;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.util.Vector;

public final class DetectionService {
    private volatile PluginSettings settings;
    private final MessageService messages;
    private final Platform platform;
    private final RecordingService recordingService;
    private final ModelService modelService;
    private final StatsService statsService;
    private final ExecutorService predictExecutor;
    private final Map<UUID, PlayerSampleState> states = new ConcurrentHashMap<UUID, PlayerSampleState>();
    private final Map<UUID, ArrayDeque<String>> alertHistory = new ConcurrentHashMap<UUID, ArrayDeque<String>>();
    private static final int MAX_ALERT_HISTORY = 50;
    private volatile long lastPruneTime = 0L;
    private static final long PRUNE_INTERVAL_MS = 5000L;
    private volatile Method getPingMethod;
    private volatile boolean getPingResolved = false;

    public DetectionService(PluginSettings settings, MessageService messages, Platform platform, RecordingService recordingService, ModelService modelService, StatsService statsService, ExecutorService predictExecutor) {
        this.settings = settings;
        this.messages = messages;
        this.platform = platform;
        this.recordingService = recordingService;
        this.modelService = modelService;
        this.statsService = statsService;
        this.predictExecutor = predictExecutor;
    }

    public void updateSettings(PluginSettings settings) {
        this.settings = settings;
    }

    public void markCombat(Player player) {
        PlayerSampleState state = state(player);
        synchronized (state) {
            state.markCombat(System.currentTimeMillis());
        }
    }

    public void quit(Player player) {
        states.remove(player.getUniqueId());
        alertHistory.remove(player.getUniqueId());
    }

    public CheckResult check(Player player) {
        PlayerSampleState state = states.get(player.getUniqueId());
        if (state == null) {
            return new CheckResult(false, 0.0D, 0, 0.0D, modelService.activeModelName());
        }
        synchronized (state) {
            if (state.tracker().windows() <= 0) {
                return new CheckResult(false, 0.0D, 0, state.lastConfidence, state.lastModel);
            }
            return new CheckResult(true, state.lastPercent, state.tracker().windows(), state.lastConfidence, state.lastModel);
        }
    }

    public ArrayDeque<String> alertHistory(Player player) {
        ArrayDeque<String> history = alertHistory.get(player.getUniqueId());
        if (history == null) {
            return new ArrayDeque<String>();
        }
        synchronized (history) {
            return new ArrayDeque<String>(history);
        }
    }

    public void handleMove(PlayerMoveEvent event) {
        PluginSettings localSettings = settings;
        if (!localSettings.detectorEnabled && recordingService.activeCount() <= 0) {
            return;
        }
        if (event.getTo() == null) {
            return;
        }
        if (event.getFrom().getYaw() == event.getTo().getYaw() && event.getFrom().getPitch() == event.getTo().getPitch()) {
            return;
        }
        if (localSettings.windowSize != DatasetCsv.WINDOW_SIZE) {
            return;
        }
        Player player = event.getPlayer();
        if (player.hasPermission("LumoAiDetector.bypass")) {
            return;
        }
        if (localSettings.disabledWorlds.contains(player.getWorld().getName())) {
            return;
        }
        if (localSettings.whitelistedUuids.contains(player.getUniqueId())) {
            return;
        }
        PlayerSampleState state = state(player);
        double[] features;
        synchronized (state) {
            features = sample(player, event.getTo(), state, localSettings);
        }
        if (features == null) {
            return;
        }
        if (localSettings.minPingMs >= 0 || localSettings.maxPingMs >= 0) {
            int ping = pingValue(player);
            if (ping >= 0) {
                if (localSettings.minPingMs >= 0 && ping < localSettings.minPingMs) {
                    return;
                }
                if (localSettings.maxPingMs >= 0 && ping > localSettings.maxPingMs) {
                    return;
                }
            }
        }
        recordingService.record(player, features);
        if (localSettings.detectorEnabled) {
            // Capture all Bukkit-derived state on the main/region thread before
            // dispatching, so the prediction thread only does pure math.
            final PlayerContext context = new PlayerContext(player.getUniqueId(), player.getName(), player.getWorld().getName(), getPing(player));
            if (localSettings.asyncPrediction) {
                final double[] featFinal = features;
                try {
                    predictExecutor.submit(new Runnable() {
                        @Override
                        public void run() {
                            PredictionResult prediction = modelService.predict(featFinal);
                            if (prediction.available()) {
                                handlePrediction(player, context, state, prediction, localSettings);
                            }
                        }
                    });
                } catch (RejectedExecutionException ignored) {
                    // Prediction backlog is full; drop this window rather than block.
                }
            } else {
                PredictionResult prediction = modelService.predict(features);
                if (prediction.available()) {
                    handlePrediction(player, context, state, prediction, localSettings);
                }
            }
        }
    }

    private double[] sample(Player player, Location current, PlayerSampleState state, PluginSettings localSettings) {
        long now = System.currentTimeMillis();
        double yaw = current.getYaw();
        double pitch = current.getPitch();
        if (!state.initialized) {
            state.initialized = true;
            state.lastYaw = yaw;
            state.lastPitch = pitch;
            state.lastTime = now;
            state.lastErr = 180.0D;
            return null;
        }
        double dYaw = normalizeYaw(yaw - state.lastYaw);
        double dPitch = pitch - state.lastPitch;
        double multiplier = state.multiplier(dYaw, dPitch, localSettings);
        double dx = clamp(Math.round(dYaw / multiplier), localSettings.maxNormalizedStep);
        double dy = clamp(Math.round(dPitch / multiplier), localSettings.maxNormalizedStep);
        double dt = Math.max(1.0D, now - state.lastTime);
        TargetInfo target = target(player, localSettings.targetRadius);
        double err = target == null ? 180.0D : target.error;
        double v = Math.sqrt(dx * dx + dy * dy) / dt;
        double a = Math.abs(v - state.lastV) / dt;
        double j = Math.abs(a - state.lastA) / dt;
        double derr = Math.abs(err - state.lastErr) / dt;
        state.lastYaw = yaw;
        state.lastPitch = pitch;
        state.lastV = v;
        state.lastA = a;
        state.lastErr = err;
        state.lastTime = now;
        if (dt > localSettings.hardLagMillis || now - state.lastCombatTime > localSettings.combatGateMillis || target == null || err >= localSettings.maxTargetAngle || dt < localSettings.dtMinMillis || dt > localSettings.dtMaxMillis) {
            state.clearWindow();
            return null;
        }
        state.addSample(new RotationSample(dx, dy, dt, v, a, j, err, derr), localSettings.windowSize);
        if (!state.full(localSettings.windowSize)) {
            return null;
        }
        if (state.movementSum() < localSettings.movementThreshold || !state.timeStable(localSettings.dtMinMillis, localSettings.dtMaxMillis)) {
            state.clearWindow();
            return null;
        }
        return state.features();
    }

    private void handlePrediction(final Player player, PlayerContext context, PlayerSampleState state, PredictionResult prediction, PluginSettings localSettings) {
        double percent;
        int windows;
        boolean alert;
        boolean punish;
        synchronized (state) {
            state.tracker().add(prediction.cheater(), localSettings.punishmentSampleSize);
            state.lastPercent = state.tracker().percent();
            state.lastConfidence = prediction.confidencePercent();
            state.lastModel = prediction.modelName();
            percent = state.lastPercent;
            windows = state.tracker().windows();
            long now = System.currentTimeMillis();
            alert = localSettings.alertsEnabled && percent >= localSettings.alertThresholdPercent && now - state.lastAlertTime >= localSettings.alertCooldownMillis;
            punish = localSettings.punishmentEnabled && windows >= localSettings.punishmentSampleSize && percent >= localSettings.punishmentThresholdPercent && state.tracker().cheatWindows() >= localSettings.punishmentRequiredCheatWindows && now - state.lastPunishTime >= localSettings.punishmentCooldownMillis;
            if (alert) {
                state.lastAlertTime = now;
            }
            if (punish) {
                state.lastPunishTime = now;
            }
            if (localSettings.markOneHundredPercent && percent >= 100.0D && windows >= localSettings.punishmentSampleSize) {
                statsService.markDefinite(context.uuid, context.name);
            }
        }
        statsService.incrementAnalyzed();
        if (alert) {
            statsService.incrementAlerts();
            String alertMsg = messages.get("alert.chat", messages.placeholders("player", context.name, "percent", Formats.percent(percent), "confidence", Formats.percent(prediction.confidencePercent()), "model", prediction.modelName()));
            platform.broadcastPermission("LumoAiDetector.alert", alertMsg);
            addAlertEntry(context.uuid, alertMsg);
        }
        if (punish) {
            for (String command : localSettings.punishmentCommands) {
                platform.dispatchConsoleCommand(replacePunishment(command, context, percent, prediction));
            }
            if (localSettings.notifyPlayer) {
                platform.sendPlayer(player, ChatColor.translateAlternateColorCodes('&', localSettings.notifyPlayerMessage));
            }
        }
    }

    private void addAlertEntry(UUID uuid, String message) {
        ArrayDeque<String> history = alertHistory.computeIfAbsent(uuid, new java.util.function.Function<UUID, ArrayDeque<String>>() {
            @Override
            public ArrayDeque<String> apply(UUID key) {
                return new ArrayDeque<String>();
            }
        });
        synchronized (history) {
            history.addLast(message);
            while (history.size() > MAX_ALERT_HISTORY) {
                history.removeFirst();
            }
        }
    }

    private String replacePunishment(String command, PlayerContext context, double percent, PredictionResult prediction) {
        return command
                .replace("{player}", context.name)
                .replace("{uuid}", context.uuid.toString())
                .replace("{percent}", Formats.percent(percent))
                .replace("{confidence}", Formats.percent(prediction.confidencePercent()))
                .replace("{model}", prediction.modelName())
                .replace("{world}", context.worldName)
                .replace("{ping}", context.ping);
    }

    private String getPing(Player player) {
        int ping = pingValue(player);
        return ping < 0 ? "?" : String.valueOf(ping);
    }

    private int pingValue(Player player) {
        Method method = resolveGetPing(player);
        if (method != null) {
            try {
                Object result = method.invoke(player);
                if (result instanceof Integer) {
                    return ((Integer) result).intValue();
                }
            } catch (Exception ignored) {
            }
        }
        try {
            Object entityPlayer = player.getClass().getMethod("getHandle").invoke(player);
            return entityPlayer.getClass().getField("ping").getInt(entityPlayer);
        } catch (Exception ignored) {
            return -1;
        }
    }

    private Method resolveGetPing(Player player) {
        if (getPingResolved) {
            return getPingMethod;
        }
        synchronized (this) {
            if (getPingResolved) {
                return getPingMethod;
            }
            try {
                getPingMethod = Player.class.getMethod("getPing");
            } catch (NoSuchMethodException ignored) {
                getPingMethod = null;
            }
            getPingResolved = true;
            return getPingMethod;
        }
    }

    private PlayerSampleState state(Player player) {
        pruneStatesIfNeeded();
        return states.computeIfAbsent(player.getUniqueId(), new java.util.function.Function<UUID, PlayerSampleState>() {
            @Override
            public PlayerSampleState apply(UUID uuid) {
                return new PlayerSampleState();
            }
        });
    }

    private void pruneStatesIfNeeded() {
        if (states.size() <= settings.maxPlayerStates) {
            return;
        }
        long now = System.currentTimeMillis();
        if (now - lastPruneTime < PRUNE_INTERVAL_MS) {
            return;
        }
        lastPruneTime = now;
        states.keySet().removeIf(new java.util.function.Predicate<UUID>() {
            @Override
            public boolean test(UUID uuid) {
                return Bukkit.getPlayer(uuid) == null;
            }
        });
    }

    private TargetInfo target(Player player, double radius) {
        Location eye = player.getEyeLocation();
        double radiusSq = radius * radius;
        boolean found = false;
        double best = 180.0D;
        for (Entity entity : player.getNearbyEntities(radius, radius, radius)) {
            if (!(entity instanceof LivingEntity) || entity.equals(player) || entity.isDead()) {
                continue;
            }
            if (entity.getLocation().distanceSquared(eye) > radiusSq) {
                continue;
            }
            LivingEntity living = (LivingEntity) entity;
            double error = errorAngle(eye, living.getEyeLocation());
            if (error < best) {
                best = error;
                found = true;
            }
        }
        return found ? new TargetInfo(best) : null;
    }

    private double errorAngle(Location eye, Location target) {
        try {
            Vector direction = eye.getDirection();
            Vector toTarget = target.clone().subtract(eye).toVector();
            if (toTarget.lengthSquared() <= 0.000001D) {
                return 180.0D;
            }
            return Math.toDegrees(direction.angle(toTarget));
        } catch (Exception ignored) {
            return 180.0D;
        }
    }

    private double normalizeYaw(double yaw) {
        double value = yaw;
        while (value > 180.0D) {
            value -= 360.0D;
        }
        while (value < -180.0D) {
            value += 360.0D;
        }
        return value;
    }

    private double clamp(double value, double max) {
        if (value > max) {
            return max;
        }
        if (value < -max) {
            return -max;
        }
        return value;
    }

    private static final class TargetInfo {
        private final double error;

        private TargetInfo(double error) {
            this.error = error;
        }
    }

    private static final class PlayerContext {
        private final UUID uuid;
        private final String name;
        private final String worldName;
        private final String ping;

        private PlayerContext(UUID uuid, String name, String worldName, String ping) {
            this.uuid = uuid;
            this.name = name;
            this.worldName = worldName;
            this.ping = ping;
        }
    }
}
