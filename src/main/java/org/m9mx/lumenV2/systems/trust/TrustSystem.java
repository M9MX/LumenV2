package org.m9mx.lumenV2.systems.trust;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.m9mx.lumenV2.config.ConfigManager;

/**
 * Trust System
 * Manages teams, alliances, and ability immunity
 */
public class TrustSystem {
    
    private static TrustSystem instance;
    private Plugin plugin;
    private TrustDataManager dataManager;
    private TrustListener trustListener;
    private boolean enabled;
    private int maxTeamsPerPlayer;
    private int maxTeamSize;
    
    private TrustSystem(Plugin plugin) {
        this.plugin = plugin;
        this.dataManager = new TrustDataManager(plugin);
        this.trustListener = new TrustListener(plugin, this);
        loadConfig();
        registerListeners();
    }
    
    public static void initialize(Plugin plugin) {
        if (instance == null) {
            instance = new TrustSystem(plugin);
        }
    }
    
    public static TrustSystem getInstance() {
        if (instance == null) {
            throw new RuntimeException("TrustSystem not initialized. Call initialize(plugin) first.");
        }
        return instance;
    }
    
    /**
     * Load configuration from systems.yml
     */
    public void loadConfig() {
        this.enabled = ConfigManager.getInstance().getSystemsConfig().getBoolean("trust.enabled", true);
        this.maxTeamsPerPlayer = ConfigManager.getInstance().getSystemsConfig().getInt("trust.team.max_per_player", 2);
        this.maxTeamSize = ConfigManager.getInstance().getSystemsConfig().getInt("trust.team.max_members", 25);
        plugin.getLogger().info("Trust System config loaded - Enabled: " + enabled + 
                               ", Max Teams: " + maxTeamsPerPlayer + 
                               ", Max Team Size: " + maxTeamSize);
    }
    
    /**
     * Register event listeners for trust
     */
    private void registerListeners() {
        Bukkit.getPluginManager().registerEvents(trustListener, plugin);
        plugin.getLogger().info("Trust System initialized");
    }
    
    public TrustDataManager getDataManager() {
        return dataManager;
    }
    
    public TrustListener getTrustListener() {
        return trustListener;
    }
    
    public boolean isEnabled() {
        return enabled;
    }
    
    public int getMaxTeamsPerPlayer() {
        return maxTeamsPerPlayer;
    }
    
    public int getMaxTeamSize() {
        return maxTeamSize;
    }
}
