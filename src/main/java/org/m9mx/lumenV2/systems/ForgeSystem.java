package org.m9mx.lumenV2.systems;

import org.bukkit.Bukkit;
import org.bukkit.event.Listener;
import org.bukkit.inventory.Inventory;
import org.bukkit.plugin.Plugin;

import org.m9mx.lumenV2.Lumen;
import org.m9mx.lumenV2.config.ConfigManager;
import org.m9mx.lumenV2.config.SystemsConfig;
import org.m9mx.lumenV2.data.ItemCraftsManager;
import org.m9mx.lumenV2.systems.forge.ForgeListener;
import org.m9mx.lumenV2.systems.forge.RitualSystem;
import java.util.HashMap;
import java.util.Map;

/**
 * Forge System
 * Handles custom forge crafting for custom items
 */
public class ForgeSystem {
    
    private static ForgeSystem instance;
    private Plugin plugin;
    private ForgeListener forgeListener;
    private RitualSystem ritualSystem;
    private ItemCraftsManager itemCraftsManager;
    private boolean enabled;
    private Map<String, Boolean> recipeEnableMap;
    
    private ForgeSystem(Plugin plugin) {
        this.plugin = plugin;
        this.itemCraftsManager = new ItemCraftsManager(plugin);
        this.ritualSystem = new RitualSystem(plugin, itemCraftsManager);
        this.forgeListener = new ForgeListener(this);
        this.recipeEnableMap = new HashMap<>();
        loadConfig();
        registerListeners();
    }
    
    public static void initialize(Plugin plugin) {
        if (instance == null) {
            instance = new ForgeSystem(plugin);
        }
    }
    
    public static ForgeSystem getInstance() {
        if (instance == null) {
            throw new RuntimeException("ForgeSystem not initialized. Call initialize(plugin) first.");
        }
        return instance;
    }
    
    /**
     * Load configuration from systems.yml
     */
    public void loadConfig() {
        this.enabled = ConfigManager.getInstance().getSystemsConfig().getBoolean("forge.enabled", true);
        
        // Load recipe enable/disable status
        org.bukkit.configuration.ConfigurationSection recipes = ConfigManager.getInstance()
                .getSystemsConfig().getConfig().getConfigurationSection("forge.recipes");
        if (recipes != null) {
            for (String key : recipes.getKeys(false)) {
                recipeEnableMap.put(key, recipes.getBoolean(key, true));
            }
        }
        
        // Load ritual system config
        ritualSystem.reloadConfiguration();
        
        plugin.getLogger().info("Forge System config loaded - Enabled: " + enabled + 
                ", Recipes configured: " + recipeEnableMap.size());
    }
    
    /**
     * Register event listeners for forge
     */
    private void registerListeners() {
        Bukkit.getPluginManager().registerEvents(forgeListener, plugin);
        Bukkit.getPluginManager().registerEvents(forgeListener.getRecipeBookListener(), plugin);
        Bukkit.getPluginManager().registerEvents(ritualSystem, plugin);  // Register RitualSystem as a listener
        plugin.getLogger().info("Forge System initialized");
    }
    
    /**
     * Check if a recipe is enabled
     * @param itemId the item ID of the recipe result
     * @return true if recipe is enabled
     */
    public boolean isRecipeEnabled(String itemId) {
        return recipeEnableMap.getOrDefault(itemId, true);
    }
    
    /**
     * Set recipe enabled/disabled
     */
    public void setRecipeEnabled(String itemId, boolean enabled) {
        recipeEnableMap.put(itemId, enabled);
    }
    
    /**
     * Update the recipe state in the systems.yml config file
     */
    public void updateRecipeConfig(String itemId, boolean enabled) {
        SystemsConfig systemsConfig = ConfigManager.getInstance().getSystemsConfig();
        systemsConfig.getConfig().set("forge.recipes." + itemId, enabled);
        systemsConfig.save();
    }
    
    public ForgeListener getForgeListener() {
        return forgeListener;
    }
    
    public RitualSystem getRitualSystem() {
        return ritualSystem;
    }
    
    public ItemCraftsManager getItemCraftsManager() {
        return itemCraftsManager;
    }
    
    public org.bukkit.plugin.Plugin getPlugin() {
        return plugin;
    }
    
    public boolean isEnabled() {
        return enabled;
    }
    
    /**
     * Check if ritual system is enabled
     */
    public boolean isRitualSystemEnabled() {
        return ConfigManager.getInstance().getSystemsConfig().getBoolean("ritual.enabled", true);
    }
    
    /**
     * Get the server-wide ritual cooldown end time
     */
    public long getServerRitualCooldownEnd() {
        return ritualSystem.getServerRitualCooldownEnd();
    }
}
