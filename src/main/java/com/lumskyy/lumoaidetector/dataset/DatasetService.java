package com.lumskyy.lumoaidetector.dataset;

import com.lumskyy.lumoaidetector.LumoAiDetectorPlugin;
import com.lumskyy.lumoaidetector.config.PluginSettings;
import com.lumskyy.lumoaidetector.storage.StatsService;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public final class DatasetService {
    private final LumoAiDetectorPlugin plugin;
    private final ExecutorService executor;
    private final StatsService statsService;
    private volatile PluginSettings settings;

    public DatasetService(LumoAiDetectorPlugin plugin, PluginSettings settings, ExecutorService executor, StatsService statsService) {
        this.plugin = plugin;
        this.settings = settings;
        this.executor = executor;
        this.statsService = statsService;
        ensureFile();
    }

    public void updateSettings(PluginSettings settings) {
        this.settings = settings;
        ensureFile();
    }

    public File file() {
        return new File(plugin.getDataFolder(), settings.datasetPath);
    }

    public void appendWindow(final RecordLabel label, double[] features) {
        if (features.length != DatasetCsv.FEATURE_COUNT) {
            return;
        }
        final double[] copy = Arrays.copyOf(features, features.length);
        executor.submit(new Runnable() {
            @Override
            public void run() {
                write(label, copy);
            }
        });
    }

    public DatasetSnapshot snapshot() throws IOException {
        flush();
        return DatasetCsv.read(file());
    }

    public void shutdown() {
        flush();
    }

    private void write(RecordLabel label, double[] features) {
        ensureFile();
        BufferedWriter writer = null;
        try {
            writer = new BufferedWriter(new FileWriter(file(), true));
            writer.write(DatasetCsv.row(features, label));
            writer.newLine();
            statsService.incrementRecorded(label);
        } catch (IOException exception) {
            plugin.getLogger().warning("Cannot write dataset row: " + exception.getMessage());
        } finally {
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException ignored) {
                }
            }
        }
    }

    private void ensureFile() {
        File file = file();
        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }
        if (!file.exists() && settings.writeDatasetHeader) {
            BufferedWriter writer = null;
            try {
                writer = new BufferedWriter(new FileWriter(file));
                writer.write(DatasetCsv.header());
                writer.newLine();
            } catch (IOException exception) {
                plugin.getLogger().warning("Cannot create dataset file: " + exception.getMessage());
            } finally {
                if (writer != null) {
                    try {
                        writer.close();
                    } catch (IOException ignored) {
                    }
                }
            }
        }
    }

    private void flush() {
        try {
            Future<?> future = executor.submit(new Runnable() {
                @Override
                public void run() {
                }
            });
            future.get(10L, TimeUnit.SECONDS);
        } catch (Exception ignored) {
        }
    }
}
