package com.lumskyy.lumoaidetector.detector;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerAnimationEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public final class DetectionListener implements Listener {
    private final DetectionService detectionService;

    public DetectionListener(DetectionService detectionService) {
        this.detectionService = detectionService;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        detectionService.handleMove(event);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onAnimation(PlayerAnimationEvent event) {
        detectionService.markCombat(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player) {
            detectionService.markCombat((Player) event.getDamager());
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        detectionService.quit(event.getPlayer());
    }
}
