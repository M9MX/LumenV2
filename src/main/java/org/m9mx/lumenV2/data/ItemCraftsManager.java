package org.m9mx.lumenV2.data;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

/**
 * Manages item craft ownership in items.yml
 */
public class ItemCraftsManager {
    
    private final Plugin plugin;
    private final File dataFolder;
    private final File itemsFile;
    private FileConfiguration itemsConfig;
    
    public ItemCraftsManager(Plugin plugin) {
        this.plugin = plugin;
        this.dataFolder = new File(plugin.getDataFolder(), "data");
        this.itemsFile = new File(dataFolder, "items.yml");
        
        loadConfig();
    }
    
    /**
     * Load the items.yml config
     */
    private void loadConfig() {
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
        
        if (!itemsFile.exists()) {
            try {
                itemsFile.createNewFile();
                itemsConfig = new YamlConfiguration();
                itemsConfig.createSection("crafts");
                itemsConfig.save(itemsFile);
            } catch (IOException e) {
                plugin.getLogger().severe("Could not create items.yml: " + e.getMessage());
            }
        } else {
            itemsConfig = YamlConfiguration.loadConfiguration(itemsFile);
        }
    }
    
    /**
     * Register that a player crafted an item
     * @param playerId the UUID of the player
     * @param playerName the name of the player
     * @param itemId the item ID that was crafted
     */
    public void registerItemCraft(UUID playerId, String playerName, String itemId) {
        if (itemsConfig.getConfigurationSection("crafts") == null) {
            itemsConfig.createSection("crafts");
        }
        
        String craftKey = "crafts." + itemId;
        
        if (itemsConfig.contains(craftKey)) {
            // Item already crafted, don't override
            return;
        }
        
        itemsConfig.set(craftKey + ".crafter_uuid", playerId.toString());
        itemsConfig.set(craftKey + ".crafter_name", playerName);
        itemsConfig.set(craftKey + ".crafted_at", System.currentTimeMillis());
        
        saveConfig();
        plugin.getLogger().info("Registered craft of " + itemId + " by " + playerName);
    }
    
    /**
     * Check if an item has already been crafted
     * @param itemId the item ID to check
     * @return true if the item has been crafted before
     */
    public boolean hasBeenCrafted(String itemId) {
        return itemsConfig.contains("crafts." + itemId);
    }
    
    /**
     * Get the UUID of the player who crafted an item
     * @param itemId the item ID
     * @return the UUID of the crafter, or null if not found
     */
    public UUID getCrafterUUID(String itemId) {
        String uuidStr = itemsConfig.getString("crafts." + itemId + ".crafter_uuid");
        if (uuidStr != null) {
            try {
                return UUID.fromString(uuidStr);
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Invalid UUID for item " + itemId + ": " + uuidStr);
            }
        }
        return null;
    }
    
    /**
     * Get the name of the player who crafted an item
     * @param itemId the item ID
     * @return the name of the crafter, or null if not found
     */
    public String getCrafterName(String itemId) {
        return itemsConfig.getString("crafts." + itemId + ".crafter_name");
    }
    
    /**
     * Get when an item was crafted
     * @param itemId the item ID
     * @return the timestamp in milliseconds, or -1 if not found
     */
    public long getCraftTime(String itemId) {
        return itemsConfig.getLong("crafts." + itemId + ".crafted_at", -1);
    }
    
    /**
     * Save the config to file
     */
    private void saveConfig() {
        try {
            itemsConfig.save(itemsFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save items.yml: " + e.getMessage());
        }
    }
    
    /**
     * Reload the config from file
     */
    public void reload() {
        loadConfig();
    }
}
