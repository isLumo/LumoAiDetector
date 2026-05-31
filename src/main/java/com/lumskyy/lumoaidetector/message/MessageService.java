package com.lumskyy.lumoaidetector.message;

import com.lumskyy.lumoaidetector.LumoAiDetectorPlugin;
import java.io.File;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

public final class MessageService {
    private final LumoAiDetectorPlugin plugin;
    private FileConfiguration messages;

    public MessageService(LumoAiDetectorPlugin plugin) {
        this.plugin = plugin;
    }

    public void reload() {
        File file = new File(plugin.getDataFolder(), "messages.yml");
        this.messages = YamlConfiguration.loadConfiguration(file);
    }

    public String get(String key) {
        String value = messages.getString(key, key);
        return color(applyPrefix(value));
    }

    public String get(String key, Map<String, String> placeholders) {
        String value = messages.getString(key, key);
        value = applyPrefix(value);
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            value = value.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        return color(value);
    }

    public void send(CommandSender sender, String key) {
        sender.sendMessage(get(key));
    }

    public void send(CommandSender sender, String key, Map<String, String> placeholders) {
        sender.sendMessage(get(key, placeholders));
    }

    public List<String> list(String key) {
        List<String> list = messages.getStringList(key);
        if (list == null) {
            return Collections.emptyList();
        }
        return list;
    }

    public Map<String, String> placeholders(Object... values) {
        Map<String, String> map = new LinkedHashMap<String, String>();
        for (int i = 0; i + 1 < values.length; i += 2) {
            map.put(String.valueOf(values[i]), String.valueOf(values[i + 1]));
        }
        return map;
    }

    public String color(String value) {
        return ChatColor.translateAlternateColorCodes('&', value == null ? "" : value);
    }

    public String plain(String key, Map<String, String> placeholders) {
        String value = messages.getString(key, key);
        value = applyPrefix(value);
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            value = value.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        return value;
    }

    private String applyPrefix(String value) {
        String prefix = messages.getString("prefix", "&9LumoAiDetector &7>");
        return value.replace("{prefix}", prefix);
    }
}
