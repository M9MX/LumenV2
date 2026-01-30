package org.m9mx.lumenV2.listeners;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.m9mx.lumenV2.items.lumeguide.LumenGuideItem;

import java.io.File;

/**
 * Listener for first-time player joins
 * Gives the Lumen Guide book to players on their first join
 */
public class FirstJoinListener implements Listener {
    private final JavaPlugin plugin;

    public FirstJoinListener(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        
        // Check if this is the player's first time joining the server
        if (!player.hasPlayedBefore()) {
            // Schedule giving the book after 1 second
            Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () -> {
                player.getInventory().addItem(new LumenGuideItem().build());
            }, 20L); // 20 ticks = 1 second
        }
    }
}
