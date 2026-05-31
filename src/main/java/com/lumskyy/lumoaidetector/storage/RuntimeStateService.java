package com.lumskyy.lumoaidetector.storage;

import com.lumskyy.lumoaidetector.LumoAiDetectorPlugin;
import java.io.File;
import java.io.IOException;
import org.bukkit.configuration.file.YamlConfiguration;

public final class RuntimeStateService {
    private final LumoAiDetectorPlugin plugin;
    private final File file;

    public RuntimeStateService(LumoAiDetectorPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "runtime.yml");
    }

    public String activeModel() {
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        return config.getString("active-model", "");
    }

    public void setActiveModel(String model) {
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        config.set("active-model", model == null ? "" : model);
        save(config);
    }

    private void save(YamlConfiguration config) {
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
