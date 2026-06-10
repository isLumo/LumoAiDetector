package com.lumskyy.lumoaidetector.config;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import org.bukkit.configuration.file.FileConfiguration;

public final class PluginSettings {
    public final boolean debug;
    public final boolean detectorEnabled;
    public final int windowSize;
    public final long combatGateMillis;
    public final double targetRadius;
    public final double maxTargetAngle;
    public final double movementThreshold;
    public final double dtMinMillis;
    public final double dtMaxMillis;
    public final double hardLagMillis;
    public final double gcdScale;
    public final int gcdHistorySize;
    public final double gcdMinMultiplier;
    public final double defaultMultiplier;
    public final double maxNormalizedStep;
    public final boolean alertsEnabled;
    public final double alertThresholdPercent;
    public final long alertCooldownMillis;
    public final boolean includeConfidence;
    public final boolean punishmentEnabled;
    public final double punishmentThresholdPercent;
    public final int punishmentSampleSize;
    public final int punishmentRequiredCheatWindows;
    public final long punishmentCooldownMillis;
    public final boolean markOneHundredPercent;
    public final String[] punishmentCommands;
    public final boolean notifyPlayer;
    public final String notifyPlayerMessage;
    public final boolean writeDatasetHeader;
    public final String datasetPath;
    public final int maxDatasetRows;
    public final int minTotalRows;
    public final int minLegitRows;
    public final int minCheaterRows;
    public final int validationPercent;
    public final int randomForestTrees;
    public final int randomFeatures;
    public final int maxNodes;
    public final int nodeSize;
    public final long trainingSeed;
    public final boolean balanceClasses;
    public final double classWeightCap;
    public final int progressIntervalSeconds;
    public final boolean autoActivateAfterTraining;
    public final String configActiveModel;
    public final long backupRetentionHours;
    public final int maxBackups;
    public final int saveStatsIntervalSeconds;
    public final int maxPlayerStates;
    public final Set<String> disabledWorlds;
    public final Set<java.util.UUID> whitelistedUuids;
    public final boolean asyncPrediction;
    public final int predictionThreads;
    public final int minPingMs;
    public final int maxPingMs;

    private PluginSettings(FileConfiguration config) {
        this.debug = config.getBoolean("settings.debug", false);
        this.detectorEnabled = config.getBoolean("detector.enabled", true);
        this.windowSize = Math.max(1, config.getInt("detector.window-size", 15));
        this.combatGateMillis = Math.max(1L, config.getLong("detector.combat-gate-ms", 1000L));
        this.targetRadius = Math.max(0.1D, config.getDouble("detector.target-radius", 6.0D));
        this.maxTargetAngle = Math.max(0.1D, config.getDouble("detector.max-target-angle", 60.0D));
        this.movementThreshold = Math.max(0.0D, config.getDouble("detector.movement-threshold", 15.0D));
        this.dtMinMillis = Math.max(0.0D, config.getDouble("detector.dt-min-ms", 25.0D));
        this.dtMaxMillis = Math.max(this.dtMinMillis, config.getDouble("detector.dt-max-ms", 150.0D));
        this.hardLagMillis = Math.max(this.dtMaxMillis, config.getDouble("detector.hard-lag-ms", 200.0D));
        this.gcdScale = Math.max(1.0D, config.getDouble("detector.gcd-scale", 100000.0D));
        this.gcdHistorySize = Math.max(2, config.getInt("detector.gcd-history-size", 32));
        this.gcdMinMultiplier = Math.max(0.000001D, config.getDouble("detector.gcd-min-multiplier", 0.0001D));
        this.defaultMultiplier = Math.max(this.gcdMinMultiplier, config.getDouble("detector.default-multiplier", 0.01D));
        this.maxNormalizedStep = Math.max(1.0D, config.getDouble("detector.max-normalized-step", 10000.0D));
        this.alertsEnabled = config.getBoolean("alert.enabled", true);
        this.alertThresholdPercent = clampPercent(config.getDouble("alert.threshold-percent", 60.0D));
        this.alertCooldownMillis = Math.max(0L, config.getLong("alert.cooldown-ms", 10000L));
        this.includeConfidence = config.getBoolean("alert.include-confidence", true);
        this.punishmentEnabled = config.getBoolean("punishment.enabled", false);
        this.punishmentThresholdPercent = clampPercent(config.getDouble("punishment.threshold-percent", 80.0D));
        this.punishmentSampleSize = Math.max(1, config.getInt("punishment.sample-size", 10));
        this.punishmentRequiredCheatWindows = Math.max(1, Math.min(this.punishmentSampleSize, config.getInt("punishment.required-cheat-windows", 8)));
        this.punishmentCooldownMillis = Math.max(0L, config.getLong("punishment.cooldown-ms", 60000L));
        this.markOneHundredPercent = config.getBoolean("punishment.mark-100-percent", true);
        this.punishmentCommands = config.getStringList("punishment.commands").toArray(new String[0]);
        this.notifyPlayer = config.getBoolean("punishment.notify-player", false);
        this.notifyPlayerMessage = config.getString("punishment.notify-player-message", "&cAnticheat flagged your combat rotation.");
        this.writeDatasetHeader = config.getBoolean("recording.write-header-if-missing", true);
        this.datasetPath = cleanRelativePath(config.getString("recording.dataset-path", "data/dataset.csv"));
        this.maxDatasetRows = Math.max(0, config.getInt("recording.max-dataset-rows", 100000));
        this.minTotalRows = Math.max(1, config.getInt("training.min-total-rows", 100));
        this.minLegitRows = Math.max(1, config.getInt("training.min-legit-rows", 25));
        this.minCheaterRows = Math.max(1, config.getInt("training.min-cheater-rows", 25));
        this.validationPercent = Math.max(0, Math.min(80, config.getInt("training.validation-percent", 20)));
        this.randomForestTrees = Math.max(1, config.getInt("training.random-forest-trees", 100));
        this.randomFeatures = Math.max(0, config.getInt("training.random-features", 0));
        this.maxNodes = Math.max(2, config.getInt("training.max-nodes", 100));
        this.nodeSize = Math.max(1, config.getInt("training.node-size", 5));
        this.trainingSeed = config.getLong("training.seed", 0L);
        this.balanceClasses = config.getBoolean("training.balance-classes", true);
        this.classWeightCap = Math.max(1.0D, config.getDouble("training.class-weight-cap", 10.0D));
        this.progressIntervalSeconds = Math.max(1, config.getInt("training.progress-interval-seconds", 5));
        this.autoActivateAfterTraining = config.getBoolean("models.auto-activate-after-training", true);
        this.configActiveModel = config.getString("models.active-model", "");
        this.backupRetentionHours = Math.max(1L, config.getLong("models.backup-retention-hours", 72L));
        this.maxBackups = Math.max(1, config.getInt("models.max-backups", 20));
        this.saveStatsIntervalSeconds = Math.max(5, config.getInt("performance.save-stats-interval-seconds", 60));
        this.maxPlayerStates = Math.max(10, config.getInt("performance.max-player-states", 500));
        java.util.Set<String> worlds = new java.util.HashSet<String>();
        for (String s : config.getStringList("detector.disabled-worlds")) {
            worlds.add(s.trim());
        }
        this.disabledWorlds = java.util.Collections.unmodifiableSet(worlds);
        java.util.Set<java.util.UUID> uuids = new java.util.HashSet<java.util.UUID>();
        for (String s : config.getStringList("detector.whitelisted-uuids")) {
            try {
                uuids.add(java.util.UUID.fromString(s.trim()));
            } catch (IllegalArgumentException ignored) {
            }
        }
        this.whitelistedUuids = java.util.Collections.unmodifiableSet(uuids);
        this.asyncPrediction = config.getBoolean("performance.async-prediction", false);
        this.predictionThreads = Math.max(1, config.getInt("performance.prediction-threads", 1));
        this.minPingMs = config.getInt("detector.min-ping-ms", -1);
        this.maxPingMs = config.getInt("detector.max-ping-ms", -1);
    }

    public static PluginSettings from(FileConfiguration config) {
        return new PluginSettings(config);
    }

    private static double clampPercent(double value) {
        if (value < 0.0D) {
            return 0.0D;
        }
        if (value > 100.0D) {
            return 100.0D;
        }
        return value;
    }

    static String cleanRelativePath(String value) {
        if (value == null || value.trim().isEmpty()) {
            return "data/dataset.csv";
        }
        String v = value.trim();
        if (v.contains("..") || v.startsWith("/") || v.startsWith("\\")
                || (v.length() >= 2 && v.charAt(1) == ':')) {
            return "data/dataset.csv";
        }
        return v.replace('\\', '/');
    }
}
