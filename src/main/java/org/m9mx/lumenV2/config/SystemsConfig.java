package org.m9mx.lumenV2.config;

import org.bukkit.plugin.Plugin;

/**
 * Systems config handler (for forge, rituals, etc.)
 */
public class SystemsConfig extends Config {
    
    public SystemsConfig(Plugin plugin) {
        super(plugin, "systems.yml");
    }
    
    // Add system-specific getters here as needed
}
