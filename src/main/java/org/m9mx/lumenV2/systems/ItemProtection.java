package org.m9mx.lumenV2.systems;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.m9mx.lumenV2.config.ConfigManager;
import org.m9mx.lumenV2.systems.protection.ItemProtectionListener;

/**
 * Item Protection System
 * Handles protection modes: NONE, SIMPLE, STRICT
 */
public class ItemProtection {
    
    private static ItemProtection instance;
    private Plugin plugin;
    private ItemProtectionListener protectionListener;
    private boolean enabled;
    private boolean loggingEnabled;
    
    private ItemProtection(Plugin plugin) {
        this.plugin = plugin;
        this.protectionListener = new ItemProtectionListener(plugin, this);
        loadConfig();
        registerListeners();
    }
    
    public static void initialize(Plugin plugin) {
        if (instance == null) {
            instance = new ItemProtection(plugin);
        }
    }
    
    public static ItemProtection getInstance() {
        if (instance == null) {
            throw new RuntimeException("ItemProtection not initialized. Call initialize(plugin) first.");
        }
        return instance;
    }
    
    /**
     * Load configuration from systems.yml
     */
    public void loadConfig() {
        this.enabled = ConfigManager.getInstance().getSystemsConfig().getBoolean("item_protection.enabled", true);
        this.loggingEnabled = ConfigManager.getInstance().getSystemsConfig().getBoolean("item_protection.logging", true);
        plugin.getLogger().info("Item Protection System config loaded - Enabled: " + enabled + ", Logging: " + loggingEnabled);
    }
    
    /**
     * Register event listeners for item protection
     */
    private void registerListeners() {
        Bukkit.getPluginManager().registerEvents(protectionListener, plugin);
        plugin.getLogger().info("Item Protection System initialized");
    }
    
    public ItemProtectionListener getProtectionListener() {
        return protectionListener;
    }
    
    public boolean isEnabled() {
        return enabled;
    }
    
    public boolean isLoggingEnabled() {
        return loggingEnabled;
    }
}
