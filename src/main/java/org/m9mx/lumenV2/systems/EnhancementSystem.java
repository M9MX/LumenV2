package org.m9mx.lumenV2.systems;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.m9mx.lumenV2.config.ConfigManager;
import org.m9mx.lumenV2.systems.enhancement.EnhancementListener;

/**
 * Enhancement System
 * Handles item enhancement with Catalyst Shards
 */
public class EnhancementSystem {
    
    private static EnhancementSystem instance;
    private Plugin plugin;
    private EnhancementListener enhancementListener;
    private boolean enabled;
    private int maxShards;
    
    private EnhancementSystem(Plugin plugin) {
        this.plugin = plugin;
        this.enhancementListener = new EnhancementListener(plugin, this);
        loadConfig();
        registerListeners();
    }
    
    public static void initialize(Plugin plugin) {
        if (instance == null) {
            instance = new EnhancementSystem(plugin);
        }
    }
    
    public static EnhancementSystem getInstance() {
        if (instance == null) {
            throw new RuntimeException("EnhancementSystem not initialized. Call initialize(plugin) first.");
        }
        return instance;
    }
    
    /**
     * Load configuration from systems.yml
     */
    public void loadConfig() {
        this.enabled = ConfigManager.getInstance().getSystemsConfig().getBoolean("enhancement.enabled", true);
        this.maxShards = ConfigManager.getInstance().getSystemsConfig().getInt("enhancement.max_amount", 5);
        plugin.getLogger().info("Enhancement System config loaded - Enabled: " + enabled + ", Max Shards: " + maxShards);
    }
    
    /**
     * Register event listeners for enhancement
     */
    private void registerListeners() {
        Bukkit.getPluginManager().registerEvents(enhancementListener, plugin);
        plugin.getLogger().info("Enhancement System initialized");
    }
    
    public EnhancementListener getEnhancementListener() {
        return enhancementListener;
    }
    
    public boolean isEnabled() {
        return enabled;
    }
    
    public int getMaxShards() {
        return maxShards;
    }
}
