package org.m9mx.lumenV2.systems.trust;

import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;

/**
 * Event listener for trust system mechanics
 * - Ability damage immunity
 * - Team/ally related events
 */
public class TrustListener implements Listener {
    
    private Plugin plugin;
    private TrustSystem trustSystem;
    
    public TrustListener(Plugin plugin, TrustSystem trustSystem) {
        this.plugin = plugin;
        this.trustSystem = trustSystem;
    }
    
    // Ability immunity will be checked in ability damage calls
    // This is handled by individual abilities checking team/ally status
}
