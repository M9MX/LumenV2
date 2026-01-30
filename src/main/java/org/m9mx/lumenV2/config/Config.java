package org.m9mx.lumenV2.config;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;

/**
 * Abstract base class for config files
 */
public abstract class Config {
    
    protected Plugin plugin;
    protected FileConfiguration config;
    protected File configFile;
    
    public Config(Plugin plugin, String filename) {
        this.plugin = plugin;
        this.configFile = new File(plugin.getDataFolder(), filename);
        loadConfig();
    }
    
    /**
     * Load the config from file
     */
    protected void loadConfig() {
        // Create data folder if it doesn't exist
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }
        
        // Create default config if it doesn't exist
        if (!configFile.exists()) {
            createDefaultConfig();
        }
        
        // Load the config
        config = YamlConfiguration.loadConfiguration(configFile);
        plugin.getLogger().info("Loaded " + configFile.getName());
    }
    
    /**
     * Create the default config file from resources
     */
    protected void createDefaultConfig() {
        try {
            // Try to get the default config from resources
            InputStream inputStream = plugin.getResource(configFile.getName());
            if (inputStream != null) {
                Files.copy(inputStream, configFile.toPath());
                inputStream.close();
            } else {
                // Create an empty config
                configFile.createNewFile();
            }
            plugin.getLogger().info("Created default " + configFile.getName());
        } catch (IOException e) {
            plugin.getLogger().severe("Could not create " + configFile.getName() + ": " + e.getMessage());
        }
    }
    
    /**
     * Reload the config from file
     */
    public void reload() {
        loadConfig();
    }
    
    /**
     * Save the config to file
     */
    public void save() {
        try {
            config.save(configFile);
            plugin.getLogger().info("Saved " + configFile.getName());
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save " + configFile.getName() + ": " + e.getMessage());
        }
    }
    
    /**
     * Get a config value by path
     */
    public Object get(String path) {
        return config.get(path);
    }
    
    /**
     * Get a string value with default
     */
    public String getString(String path, String defaultValue) {
        return config.getString(path, defaultValue);
    }
    
    /**
     * Get a boolean value with default
     */
    public boolean getBoolean(String path, boolean defaultValue) {
        return config.getBoolean(path, defaultValue);
    }
    
    /**
     * Get an integer value with default
     */
    public int getInt(String path, int defaultValue) {
        return config.getInt(path, defaultValue);
    }
    
    /**
     * Get the FileConfiguration object
     */
    public FileConfiguration getConfig() {
        return config;
    }
}
