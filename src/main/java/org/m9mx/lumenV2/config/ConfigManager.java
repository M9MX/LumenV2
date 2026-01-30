package org.m9mx.lumenV2.config;

import org.bukkit.plugin.Plugin;

/**
 * Central config manager for all Lumen configs
 */
public class ConfigManager {
    
    private static ConfigManager instance;
    private MainConfig mainConfig;
    private SystemsConfig systemsConfig;
    private ItemsConfig itemsConfig;
    
    private ConfigManager(Plugin plugin) {
        this.mainConfig = new MainConfig(plugin);
        this.systemsConfig = new SystemsConfig(plugin);
        this.itemsConfig = new ItemsConfig(plugin);
    }
    
    public static void initialize(Plugin plugin) {
        if (instance == null) {
            instance = new ConfigManager(plugin);
        }
    }
    
    public static ConfigManager getInstance() {
        if (instance == null) {
            throw new RuntimeException("ConfigManager not initialized. Call initialize(plugin) first.");
        }
        return instance;
    }
    
    /**
     * Reload all configs
     */
    public void reloadAll() {
        mainConfig.reload();
        systemsConfig.reload();
        itemsConfig.reload();
    }
    
    /**
     * Reload specific config
     */
    public void reload(String type) {
        switch (type.toLowerCase()) {
            case "config":
                mainConfig.reload();
                break;
            case "systems":
                systemsConfig.reload();
                break;
            case "items":
                itemsConfig.reload();
                break;
        }
    }
    
    /**
     * Save all configs
     */
    public void saveAll() {
        mainConfig.save();
        systemsConfig.save();
        itemsConfig.save();
    }
    
    /**
     * Get main config
     */
    public MainConfig getMainConfig() {
        return mainConfig;
    }
    
    /**
     * Get systems config
     */
    public SystemsConfig getSystemsConfig() {
        return systemsConfig;
    }
    
    /**
     * Get items config
     */
    public ItemsConfig getItemsConfig() {
        return itemsConfig;
    }
}
