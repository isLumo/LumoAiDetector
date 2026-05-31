package com.lumskyy.lumoaidetector.storage;

import com.lumskyy.lumoaidetector.LumoAiDetectorPlugin;
import com.lumskyy.lumoaidetector.config.PluginSettings;
import com.lumskyy.lumoaidetector.dataset.RecordLabel;
import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

public final class StatsService {
    private final LumoAiDetectorPlugin plugin;
    private final File file;
    private final AtomicLong recordedLegit = new AtomicLong();
    private final AtomicLong recordedCheater = new AtomicLong();
    private final AtomicLong analyzedWindows = new AtomicLong();
    private final AtomicLong alertCount = new AtomicLong();
    private final ConcurrentHashMap<String, String> definiteCheaters = new ConcurrentHashMap<String, String>();
    private ScheduledFuture<?> autoSaveTask;

    public StatsService(LumoAiDetectorPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "stats.yml");
    }

    public synchronized void load() {
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        recordedLegit.set(config.getLong("recorded.legit", 0L));
        recordedCheater.set(config.getLong("recorded.cheater", 0L));
        analyzedWindows.set(config.getLong("analyzed-windows", 0L));
        alertCount.set(config.getLong("alerts", 0L));
        definiteCheaters.clear();
        ConfigurationSection section = config.getConfigurationSection("definite-cheaters");
        if (section != null) {
            for (String key : section.getKeys(false)) {
                definiteCheaters.put(key, section.getString(key, key));
            }
        }
    }

    public synchronized void save() {
        YamlConfiguration config = new YamlConfiguration();
        config.set("recorded.legit", recordedLegit.get());
        config.set("recorded.cheater", recordedCheater.get());
        config.set("analyzed-windows", analyzedWindows.get());
        config.set("alerts", alertCount.get());
        for (Map.Entry<String, String> entry : definiteCheaters.entrySet()) {
            config.set("definite-cheaters." + entry.getKey(), entry.getValue());
        }
        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }
        try {
            config.save(file);
        } catch (IOException exception) {
            plugin.getLogger().warning("Cannot save stats.yml: " + exception.getMessage());
        }
    }

    public synchronized void startAutoSave(ScheduledExecutorService executor, PluginSettings settings) {
        if (autoSaveTask != null) {
            autoSaveTask.cancel(false);
        }
        autoSaveTask = executor.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                save();
            }
        }, settings.saveStatsIntervalSeconds, settings.saveStatsIntervalSeconds, TimeUnit.SECONDS);
    }

    public void incrementRecorded(RecordLabel label) {
        if (label == RecordLabel.LEGIT) {
            recordedLegit.incrementAndGet();
        } else if (label == RecordLabel.CHEATER) {
            recordedCheater.incrementAndGet();
        }
    }

    public void incrementAnalyzed() {
        analyzedWindows.incrementAndGet();
    }

    public void incrementAlerts() {
        alertCount.incrementAndGet();
    }

    public void markDefinite(UUID uuid, String name) {
        definiteCheaters.put(uuid.toString(), name);
    }

    public long recordedLegit() {
        return recordedLegit.get();
    }

    public long recordedCheater() {
        return recordedCheater.get();
    }

    public long analyzedWindows() {
        return analyzedWindows.get();
    }

    public long alerts() {
        return alertCount.get();
    }

    public int definiteCheaters() {
        return definiteCheaters.size();
    }
}
