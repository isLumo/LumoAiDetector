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
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
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
    private final Map<UUID, PlayerSampleState> states = new ConcurrentHashMap<UUID, PlayerSampleState>();

    public DetectionService(PluginSettings settings, MessageService messages, Platform platform, RecordingService recordingService, ModelService modelService, StatsService statsService) {
        this.settings = settings;
        this.messages = messages;
        this.platform = platform;
        this.recordingService = recordingService;
        this.modelService = modelService;
        this.statsService = statsService;
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
        PlayerSampleState state = state(player);
        double[] features;
        synchronized (state) {
            features = sample(player, event.getTo(), state, localSettings);
        }
        if (features == null) {
            return;
        }
        recordingService.record(player, features);
        if (localSettings.detectorEnabled) {
            PredictionResult prediction = modelService.predict(features);
            if (prediction.available()) {
                handlePrediction(player, state, prediction, localSettings);
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

    private void handlePrediction(final Player player, PlayerSampleState state, PredictionResult prediction, PluginSettings localSettings) {
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
                statsService.markDefinite(player.getUniqueId(), player.getName());
            }
        }
        statsService.incrementAnalyzed();
        if (alert) {
            statsService.incrementAlerts();
            platform.broadcastPermission("LumoAiDetector.alert", messages.get("alert.chat", messages.placeholders("player", player.getName(), "percent", Formats.percent(percent), "confidence", Formats.percent(prediction.confidencePercent()), "model", prediction.modelName())));
        }
        if (punish) {
            for (String command : localSettings.punishmentCommands) {
                platform.dispatchConsoleCommand(replacePunishment(command, player, percent, prediction));
            }
        }
    }

    private String replacePunishment(String command, Player player, double percent, PredictionResult prediction) {
        return command
                .replace("{player}", player.getName())
                .replace("{uuid}", player.getUniqueId().toString())
                .replace("{percent}", Formats.percent(percent))
                .replace("{confidence}", Formats.percent(prediction.confidencePercent()))
                .replace("{model}", prediction.modelName());
    }

    private PlayerSampleState state(Player player) {
        if (states.size() > settings.maxPlayerStates) {
            states.clear();
        }
        PlayerSampleState state = states.get(player.getUniqueId());
        if (state == null) {
            state = new PlayerSampleState();
            states.put(player.getUniqueId(), state);
        }
        return state;
    }

    private TargetInfo target(Player player, double radius) {
        Location eye = player.getEyeLocation();
        boolean found = false;
        double best = 180.0D;
        for (Entity entity : player.getNearbyEntities(radius, radius, radius)) {
            if (!(entity instanceof LivingEntity) || entity.equals(player) || entity.isDead()) {
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
            Vector toTarget = target.subtract(eye).toVector();
            if (toTarget.lengthSquared() <= 0.000001D) {
                return 180.0D;
            }
            return Math.toDegrees(direction.angle(toTarget));
        } catch (Throwable ignored) {
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
}
