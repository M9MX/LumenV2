package org.m9mx.lumenV2.systems.protection.tracking;

import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.m9mx.lumenV2.config.ConfigManager;
import org.m9mx.lumenV2.util.ItemDataHelper;
import org.m9mx.lumenV2.util.ItemUtils;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Tracks item locations for STRICT protection mode
 */
public class ItemProtectionTracker {
    
    private Plugin plugin;
    private File trackerFile;
    private FileConfiguration trackerData;
    
    public ItemProtectionTracker(Plugin plugin) {
        this.plugin = plugin;
        this.trackerFile = new File(plugin.getDataFolder(), "item_tracking.yml");
        loadTrackerData();
    }
    
    /**
     * Load tracker data from file
     */
    private void loadTrackerData() {
        if (!trackerFile.exists()) {
            try {
                trackerFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Could not create item_tracking.yml: " + e.getMessage());
            }
        }
        trackerData = YamlConfiguration.loadConfiguration(trackerFile);
    }
    
    /**
     * Track an item's location when held by a player
     */
    public void trackItemLocation(ItemStack item, Player player) {
        // Check if logging is enabled
        if (!ConfigManager.getInstance().getSystemsConfig().getBoolean("item_protection.logging", true)) {
            return;
        }
        
        String itemId = ItemUtils.getItemId(item);
        if (itemId == null) {
            return;
        }
        
        // Get current location
        Location loc = player.getLocation();
        String locationInfo = String.format("World: %s, X: %d, Y: %d, Z: %d",
                loc.getWorld().getName(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
        
        // Create entry: item_id -> player_name, location, timestamp
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        
        String path = "items." + itemId;
        trackerData.set(path + ".player", player.getName());
        trackerData.set(path + ".location", locationInfo);
        trackerData.set(path + ".status", "in_player_inventory");
        trackerData.set(path + ".timestamp", timestamp);
        
        saveTrackerData();
    }
    
    /**
     * Track an item's location when placed in a container
     */
    public void trackItemInContainer(ItemStack item, Location containerLocation, String containerType) {
        // Check if logging is enabled
        if (!ConfigManager.getInstance().getSystemsConfig().getBoolean("item_protection.logging", true)) {
            return;
        }
        
        String itemId = ItemUtils.getItemId(item);
        if (itemId == null) {
            return;
        }
        
        String locationInfo = String.format("World: %s, X: %d, Y: %d, Z: %d (%s)",
                containerLocation.getWorld().getName(), containerLocation.getBlockX(), 
                containerLocation.getBlockY(), containerLocation.getBlockZ(), containerType);
        
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        
        String path = "items." + itemId;
        trackerData.set(path + ".player", "N/A (stored in container)");
        trackerData.set(path + ".location", locationInfo);
        trackerData.set(path + ".status", "in_container");
        trackerData.set(path + ".container_type", containerType);
        trackerData.set(path + ".timestamp", timestamp);
        
        saveTrackerData();
    }
    
    /**
     * Track item despawn location
     */
    public void trackItemDespawn(ItemStack item, Location location) {
        // Check if logging is enabled
        if (!ConfigManager.getInstance().getSystemsConfig().getBoolean("item_protection.logging", true)) {
            return;
        }
        
        String itemId = ItemUtils.getItemId(item);
        if (itemId == null) {
            return;
        }
        
        String locationInfo = String.format("World: %s, X: %d, Y: %d, Z: %d",
                location.getWorld().getName(), location.getBlockX(), location.getBlockY(), location.getBlockZ());
        
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        
        String path = "items." + itemId;
        trackerData.set(path + ".location", locationInfo);
        trackerData.set(path + ".status", "despawned");
        trackerData.set(path + ".player", "N/A");
        trackerData.set(path + ".timestamp", timestamp);
        
        saveTrackerData();
    }
    
    /**
     * Track item burned in fire/lava
     */
    public void trackItemBurned(ItemStack item, Location location, String cause) {
        // Check if logging is enabled
        if (!ConfigManager.getInstance().getSystemsConfig().getBoolean("item_protection.logging", true)) {
            return;
        }
        
        String itemId = ItemUtils.getItemId(item);
        if (itemId == null) {
            return;
        }
        
        String locationInfo = String.format("World: %s, X: %d, Y: %d, Z: %d",
                location.getWorld().getName(), location.getBlockX(), location.getBlockY(), location.getBlockZ());
        
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        
        String path = "items." + itemId;
        trackerData.set(path + ".location", locationInfo);
        trackerData.set(path + ".status", "burned");
        trackerData.set(path + ".cause", cause);
        trackerData.set(path + ".player", "N/A");
        trackerData.set(path + ".timestamp", timestamp);
        
        saveTrackerData();
    }
    
    /**
     * Track item destroyed in explosion
     */
    public void trackItemExploded(ItemStack item, Location location) {
        // Check if logging is enabled
        if (!ConfigManager.getInstance().getSystemsConfig().getBoolean("item_protection.logging", true)) {
            return;
        }
        
        String itemId = ItemUtils.getItemId(item);
        if (itemId == null) {
            return;
        }
        
        String locationInfo = String.format("World: %s, X: %d, Y: %d, Z: %d",
                location.getWorld().getName(), location.getBlockX(), location.getBlockY(), location.getBlockZ());
        
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        
        String path = "items." + itemId;
        trackerData.set(path + ".location", locationInfo);
        trackerData.set(path + ".status", "exploded");
        trackerData.set(path + ".player", "N/A");
        trackerData.set(path + ".timestamp", timestamp);
        
        saveTrackerData();
    }
    
    /**
     * Save tracker data to file
     */
    private void saveTrackerData() {
        try {
            trackerData.save(trackerFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save item_tracking.yml: " + e.getMessage());
        }
    }
}
