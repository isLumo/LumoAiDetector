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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;

public final class DatasetService {
    private final LumoAiDetectorPlugin plugin;
    private final ExecutorService executor;
    private final StatsService statsService;
    private final AtomicInteger writtenRows = new AtomicInteger();
    private volatile PluginSettings settings;
    private volatile BufferedWriter writer;

    public DatasetService(LumoAiDetectorPlugin plugin, PluginSettings settings, ExecutorService executor, StatsService statsService) {
        this.plugin = plugin;
        this.settings = settings;
        this.executor = executor;
        this.statsService = statsService;
    }

    public void updateSettings(PluginSettings settings) {
        this.settings = settings;
    }

    public File file() {
        return new File(plugin.getDataFolder(), settings.datasetPath);
    }

    public int writtenRowCount() {
        return writtenRows.get();
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
        return DatasetCsv.read(file(), settings.maxDatasetRows);
    }

    public void flushPending() {
        try {
            java.util.concurrent.Future<?> future = executor.submit(new Runnable() {
                @Override
                public void run() {
                    try {
                        BufferedWriter w = writer;
                        if (w != null) {
                            w.flush();
                        }
                    } catch (IOException ignored) {
                    }
                }
            });
            future.get(30L, java.util.concurrent.TimeUnit.SECONDS);
        } catch (Exception ignored) {
        }
    }

    public void shutdown() {
        closeWriter();
    }

    private void write(RecordLabel label, double[] features) {
        try {
            BufferedWriter w = writer();
            w.write(DatasetCsv.row(features, label));
            w.newLine();
            int count = writtenRows.incrementAndGet();
            statsService.incrementRecorded(label);
            if (settings.maxDatasetRows > 0 && count >= settings.maxDatasetRows) {
                plugin.getLogger().warning("Dataset has reached " + count + " rows (limit: " + settings.maxDatasetRows + "). Consider trimming or increasing the limit.");
            }
        } catch (IOException exception) {
            plugin.getLogger().log(Level.WARNING, "Cannot write dataset row", exception);
        }
    }

    private BufferedWriter writer() throws IOException {
        BufferedWriter w = writer;
        if (w != null) {
            return w;
        }
        synchronized (this) {
            w = writer;
            if (w != null) {
                return w;
            }
            ensureFile();
            w = new BufferedWriter(new FileWriter(file(), true));
            writer = w;
            return w;
        }
    }

    private void ensureFile() {
        File file = file();
        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }
        if (!file.exists() && settings.writeDatasetHeader) {
            try (BufferedWriter w = new BufferedWriter(new FileWriter(file))) {
                w.write(DatasetCsv.header());
                w.newLine();
            } catch (IOException exception) {
                plugin.getLogger().warning("Cannot create dataset file: " + exception.getMessage());
            }
        }
    }

    private synchronized void closeWriter() {
        BufferedWriter w = writer;
        if (w != null) {
            writer = null;
            try {
                w.close();
            } catch (IOException exception) {
                plugin.getLogger().log(Level.WARNING, "Cannot close dataset writer", exception);
            }
        }
    }
}
