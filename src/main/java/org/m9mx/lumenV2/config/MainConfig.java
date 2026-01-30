package org.m9mx.lumenV2.config;

import org.bukkit.plugin.Plugin;

/**
 * Main config.yml handler
 */
public class MainConfig extends Config {
    
    public MainConfig(Plugin plugin) {
        super(plugin, "config.yml");
    }
    
    public boolean isDebugEnabled() {
        return getBoolean("settings.debug", false);
    }
}
