package com.lumskyy.lumoaidetector.ml;

import com.lumskyy.lumoaidetector.util.Formats;
import java.io.File;
import java.io.IOException;
import org.bukkit.configuration.file.YamlConfiguration;

public final class ModelMetadata {
    private final String name;
    private final String fileName;
    private final long createdAt;
    private final long trainingMillis;
    private final int rows;
    private final int legitRows;
    private final int cheaterRows;
    private final int validationRows;
    private final double accuracy;
    private final double precision;
    private final double recall;
    private final double f1;
    private final double falsePositiveRate;
    private final int treeCount;
    private final int featureCount;
    private final String pluginVersion;
    private final String serverVersion;

    public ModelMetadata(String name, String fileName, long createdAt, long trainingMillis, int rows, int legitRows, int cheaterRows, int validationRows, double accuracy, double precision, double recall, double f1, double falsePositiveRate, int treeCount, int featureCount, String pluginVersion, String serverVersion) {
        this.name = name;
        this.fileName = fileName;
        this.createdAt = createdAt;
        this.trainingMillis = trainingMillis;
        this.rows = rows;
        this.legitRows = legitRows;
        this.cheaterRows = cheaterRows;
        this.validationRows = validationRows;
        this.accuracy = accuracy;
        this.precision = precision;
        this.recall = recall;
        this.f1 = f1;
        this.falsePositiveRate = falsePositiveRate;
        this.treeCount = treeCount;
        this.featureCount = featureCount;
        this.pluginVersion = pluginVersion;
        this.serverVersion = serverVersion;
    }

    public static ModelMetadata minimal(String name, File file) {
        return new ModelMetadata(name, file.getName(), file.lastModified(), 0L, 0, 0, 0, 0, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0, 120, "unknown", "unknown");
    }

    public static ModelMetadata load(File file, File modelFile) {
        if (!file.exists()) {
            return minimal(baseName(modelFile), modelFile);
        }
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        return new ModelMetadata(
                config.getString("name", baseName(modelFile)),
                config.getString("file", modelFile.getName()),
                config.getLong("created-at", modelFile.lastModified()),
                config.getLong("training-ms", 0L),
                config.getInt("rows.total", 0),
                config.getInt("rows.legit", 0),
                config.getInt("rows.cheater", 0),
                config.getInt("rows.validation", 0),
                config.getDouble("metrics.accuracy", 0.0D),
                config.getDouble("metrics.precision", 0.0D),
                config.getDouble("metrics.recall", 0.0D),
                config.getDouble("metrics.f1", 0.0D),
                config.getDouble("metrics.false-positive-rate", 0.0D),
                config.getInt("forest.trees", 0),
                config.getInt("forest.features", 120),
                config.getString("plugin-version", "unknown"),
                config.getString("server-version", "unknown")
        );
    }

    public void save(File file) throws IOException {
        saveWithSha256(file, null);
    }

    public void saveWithSha256(File file, String sha256) throws IOException {
        YamlConfiguration config = new YamlConfiguration();
        config.set("name", name);
        config.set("file", fileName);
        config.set("created-at", createdAt);
        config.set("created-readable", Formats.date(createdAt));
        config.set("training-ms", trainingMillis);
        config.set("training-readable", Formats.duration(trainingMillis));
        config.set("rows.total", rows);
        config.set("rows.legit", legitRows);
        config.set("rows.cheater", cheaterRows);
        config.set("rows.validation", validationRows);
        config.set("metrics.accuracy", accuracy);
        config.set("metrics.precision", precision);
        config.set("metrics.recall", recall);
        config.set("metrics.f1", f1);
        config.set("metrics.false-positive-rate", falsePositiveRate);
        config.set("forest.trees", treeCount);
        config.set("forest.features", featureCount);
        config.set("plugin-version", pluginVersion);
        config.set("server-version", serverVersion);
        if (sha256 != null && !sha256.isEmpty()) {
            config.set("sha256", sha256);
        }
        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }
        config.save(file);
    }

    public String name() {
        return name;
    }

    public String fileName() {
        return fileName;
    }

    public long createdAt() {
        return createdAt;
    }

    public long trainingMillis() {
        return trainingMillis;
    }

    public int rows() {
        return rows;
    }

    public int legitRows() {
        return legitRows;
    }

    public int cheaterRows() {
        return cheaterRows;
    }

    public int validationRows() {
        return validationRows;
    }

    public double accuracy() {
        return accuracy;
    }

    public double precision() {
        return precision;
    }

    public double recall() {
        return recall;
    }

    public double f1() {
        return f1;
    }

    public double falsePositiveRate() {
        return falsePositiveRate;
    }

    public int treeCount() {
        return treeCount;
    }

    public int featureCount() {
        return featureCount;
    }

    public String pluginVersion() {
        return pluginVersion;
    }

    public String serverVersion() {
        return serverVersion;
    }

    private static String baseName(File file) {
        String name = file.getName();
        if (name.endsWith(".bin")) {
            return name.substring(0, name.length() - 4);
        }
        return name;
    }
}
