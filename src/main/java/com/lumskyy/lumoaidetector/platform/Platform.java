package com.lumskyy.lumoaidetector.platform;

import com.lumskyy.lumoaidetector.LumoAiDetectorPlugin;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.logging.Level;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class Platform {
    private final LumoAiDetectorPlugin plugin;
    private final boolean folia;

    public Platform(LumoAiDetectorPlugin plugin) {
        this.plugin = plugin;
        this.folia = detectFolia();
    }

    public boolean isFolia() {
        return folia;
    }

    public String serverName() {
        return Bukkit.getName() + " " + Bukkit.getVersion();
    }

    public void runGlobal(Runnable runnable) {
        if (folia && runFoliaGlobal(runnable)) {
            return;
        }
        Bukkit.getScheduler().runTask(plugin, runnable);
    }

    public void send(CommandSender sender, String message) {
        if (sender instanceof Player) {
            sendPlayer((Player) sender, message);
            return;
        }
        sender.sendMessage(message);
    }

    public void sendPlayer(final Player player, final String message) {
        if (folia && runEntity(player, new Runnable() {
            @Override
            public void run() {
                player.sendMessage(message);
            }
        })) {
            return;
        }
        if (!folia && !isPrimaryThread()) {
            Bukkit.getScheduler().runTask(plugin, new Runnable() {
                @Override
                public void run() {
                    player.sendMessage(message);
                }
            });
            return;
        }
        player.sendMessage(message);
    }

    public void broadcastPermission(final String permission, final String message) {
        Runnable task = new Runnable() {
            @Override
            public void run() {
                Collection<? extends Player> players = Bukkit.getOnlinePlayers();
                for (Player player : players) {
                    if (player.hasPermission(permission)) {
                        sendPlayer(player, message);
                    }
                }
            }
        };
        runGlobal(task);
    }

    public void dispatchConsoleCommand(final String command) {
        runGlobal(new Runnable() {
            @Override
            public void run() {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
            }
        });
    }

    private boolean detectFolia() {
        try {
            Bukkit.class.getMethod("getGlobalRegionScheduler");
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private boolean runFoliaGlobal(Runnable runnable) {
        try {
            Method method = Bukkit.class.getMethod("getGlobalRegionScheduler");
            Object scheduler = method.invoke(null);
            Method execute = scheduler.getClass().getMethod("execute", org.bukkit.plugin.Plugin.class, Runnable.class);
            execute.invoke(scheduler, plugin, runnable);
            return true;
        } catch (Throwable throwable) {
            plugin.getLogger().log(Level.WARNING, "Folia global scheduler fallback failed", throwable);
            return false;
        }
    }

    private boolean runEntity(Player player, Runnable runnable) {
        try {
            Method getScheduler = player.getClass().getMethod("getScheduler");
            Object scheduler = getScheduler.invoke(player);
            Method execute = scheduler.getClass().getMethod("execute", org.bukkit.plugin.Plugin.class, Runnable.class, Runnable.class, long.class);
            Object result = execute.invoke(scheduler, plugin, runnable, null, 1L);
            return !(result instanceof Boolean) || ((Boolean) result).booleanValue();
        } catch (Throwable ignored) {
            return false;
        }
    }

    private boolean isPrimaryThread() {
        try {
            Method method = Bukkit.class.getMethod("isPrimaryThread");
            Object result = method.invoke(null);
            return result instanceof Boolean && ((Boolean) result).booleanValue();
        } catch (Throwable ignored) {
            return true;
        }
    }
}
