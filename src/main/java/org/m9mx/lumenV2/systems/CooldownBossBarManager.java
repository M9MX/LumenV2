package org.m9mx.lumenV2.systems;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Universal cooldown boss bar manager for item abilities
 * Provides a simple way to show cooldown progress for any ability
 */
public class CooldownBossBarManager {
    
    private static CooldownBossBarManager instance;
    private final JavaPlugin plugin;
    private final Map<UUID, Map<String, BossBar>> activeBossBars = new HashMap<>();
    
    private CooldownBossBarManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Initialize the singleton instance
     */
    public static void initialize(JavaPlugin plugin) {
        if (instance == null) {
            instance = new CooldownBossBarManager(plugin);
        }
    }
    
    /**
     * Get the singleton instance
     */
    public static CooldownBossBarManager getInstance() {
        return instance;
    }
    
    /**
     * Start a cooldown boss bar for a player
     * @param player The player to show the boss bar to
     * @param abilityId Unique identifier for the ability
     * @param title Title to display on the boss bar
     * @param cooldownMs Cooldown duration in milliseconds
     * @param color Color of the boss bar
     */
    public void startCooldown(Player player, String abilityId, String title, long cooldownMs, BarColor color) {
        UUID playerId = player.getUniqueId();
        
        // Remove existing boss bar for this ability if any
        removeCooldown(playerId, abilityId);
        
        // Get or create the player's boss bar map
        Map<String, BossBar> playerBossBars = activeBossBars.computeIfAbsent(playerId, k -> new HashMap<>());
        
        // Create new boss bar
        BossBar bossBar = Bukkit.createBossBar(title, color, BarStyle.SOLID);
        bossBar.addPlayer(player);
        playerBossBars.put(abilityId, bossBar);
        
        // Update boss bar progress and remove when cooldown ends
        new BukkitRunnable() {
            long startTime = System.currentTimeMillis();
            
            @Override
            public void run() {
                long elapsed = System.currentTimeMillis() - startTime;
                double progress = Math.max(0, 1.0 - (double) elapsed / cooldownMs);
                
                if (progress <= 0) {
                    bossBar.removeAll();
                    Map<String, BossBar> playerBars = activeBossBars.get(playerId);
                    if (playerBars != null) {
                        playerBars.remove(abilityId);
                        if (playerBars.isEmpty()) {
                            activeBossBars.remove(playerId);
                        }
                    }
                    cancel();
                    return;
                }
                
                bossBar.setProgress(progress);
            }
        }.runTaskTimer(plugin, 0, 1);
    }
    
    /**
     * Remove a cooldown boss bar for a player
     * @param playerId The player's UUID
     */
    public void removeCooldown(UUID playerId) {
        removeAllCooldowns(playerId);
    }
    
    /**
     * Remove a cooldown boss bar for a player by ability ID
     * @param player The player
     * @param abilityId The ability identifier
     */
    public void removeCooldown(Player player, String abilityId) {
        removeCooldown(player.getUniqueId(), abilityId);
    }
    
    /**
     * Remove a cooldown boss bar for a player by ability ID
     * @param playerId The player's UUID
     * @param abilityId The ability identifier
     */
    public void removeCooldown(UUID playerId, String abilityId) {
        Map<String, BossBar> playerBossBars = activeBossBars.get(playerId);
        if (playerBossBars != null) {
            BossBar bossBar = playerBossBars.remove(abilityId);
            if (bossBar != null) {
                bossBar.removeAll();
            }
            if (playerBossBars.isEmpty()) {
                activeBossBars.remove(playerId);
            }
        }
    }
    
    /**
     * Remove all cooldown boss bars for a player
     * @param player The player
     */
    public void removeAllCooldowns(Player player) {
        removeAllCooldowns(player.getUniqueId());
    }
    
    /**
     * Remove all cooldown boss bars for a player
     * @param playerId The player's UUID
     */
    public void removeAllCooldowns(UUID playerId) {
        Map<String, BossBar> playerBossBars = activeBossBars.remove(playerId);
        if (playerBossBars != null) {
            for (BossBar bossBar : playerBossBars.values()) {
                bossBar.removeAll();
            }
        }
    }
    
    /**
     * Clear all active cooldown boss bars
     */
    public void clearAllCooldowns() {
        for (Map<String, BossBar> playerBossBars : activeBossBars.values()) {
            for (BossBar bossBar : playerBossBars.values()) {
                bossBar.removeAll();
            }
        }
        activeBossBars.clear();
    }
    
    /**
     * Check if a player has an active cooldown
     * @param player The player
     * @return true if the player has an active cooldown boss bar
     */
    public boolean hasActiveCooldown(Player player) {
        Map<String, BossBar> playerBossBars = activeBossBars.get(player.getUniqueId());
        return playerBossBars != null && !playerBossBars.isEmpty();
    }
    
    /**
     * Check if a player has an active cooldown for a specific ability
     * @param player The player
     * @param abilityId The ability identifier
     * @return true if the player has an active cooldown for the specified ability
     */
    public boolean hasActiveCooldown(Player player, String abilityId) {
        Map<String, BossBar> playerBossBars = activeBossBars.get(player.getUniqueId());
        return playerBossBars != null && playerBossBars.containsKey(abilityId);
    }
    
    /**
     * Get the active boss bar for a player and ability
     * @param player The player
     * @param abilityId The ability identifier
     * @return The active boss bar, or null if none
     */
    public BossBar getActiveBossBar(Player player, String abilityId) {
        Map<String, BossBar> playerBossBars = activeBossBars.get(player.getUniqueId());
        return playerBossBars != null ? playerBossBars.get(abilityId) : null;
    }
    
    /**
     * Get all active boss bars for a player
     * @param player The player
     * @return Map of ability IDs to boss bars
     */
    public Map<String, BossBar> getAllActiveBossBars(Player player) {
        return activeBossBars.getOrDefault(player.getUniqueId(), new HashMap<>());
    }
}
