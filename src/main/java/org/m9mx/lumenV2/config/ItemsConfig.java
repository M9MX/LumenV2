package org.m9mx.lumenV2.config;

import org.bukkit.plugin.Plugin;

/**
 * Items config handler (for item-specific settings)
 */
public class ItemsConfig extends Config {
    
    public ItemsConfig(Plugin plugin) {
        super(plugin, "items.yml");
    }
    
    // Add item-specific getters here as needed
}
