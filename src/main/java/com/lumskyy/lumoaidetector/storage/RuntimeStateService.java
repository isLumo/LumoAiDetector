package com.lumskyy.lumoaidetector.storage;

import com.lumskyy.lumoaidetector.LumoAiDetectorPlugin;
import java.io.File;
import java.io.IOException;
import org.bukkit.configuration.file.YamlConfiguration;

public final class RuntimeStateService {
    private final LumoAiDetectorPlugin plugin;
    private final File file;
    private volatile String cachedModel = "";

    public RuntimeStateService(LumoAiDetectorPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "runtime.yml");
        this.cachedModel = loadModel();
    }

    public String activeModel() {
        return cachedModel;
    }

    public void setActiveModel(String model) {
        cachedModel = model == null ? "" : model;
        save();
    }

    private String loadModel() {
        if (!file.exists()) {
            return "";
        }
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        return config.getString("active-model", "");
    }

    private void save() {
        YamlConfiguration config = new YamlConfiguration();
        config.set("active-model", cachedModel);
        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }
        try {
            config.save(file);
        } catch (IOException exception) {
            plugin.getLogger().warning("Cannot save runtime.yml: " + exception.getMessage());
        }
    }
}
