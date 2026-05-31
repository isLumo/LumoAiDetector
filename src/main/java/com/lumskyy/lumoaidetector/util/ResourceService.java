package com.lumskyy.lumoaidetector.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import org.bukkit.plugin.java.JavaPlugin;

public final class ResourceService {
    private ResourceService() {
    }

    public static void saveIfMissing(JavaPlugin plugin, String path) {
        File file = new File(plugin.getDataFolder(), path);
        if (file.exists()) {
            return;
        }
        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }
        InputStream input = plugin.getResource(path);
        if (input == null) {
            return;
        }
        try {
            Files.copy(input, file.toPath());
        } catch (IOException exception) {
            plugin.getLogger().warning("Cannot save resource " + path + ": " + exception.getMessage());
        } finally {
            try {
                input.close();
            } catch (IOException ignored) {
            }
        }
    }
}
