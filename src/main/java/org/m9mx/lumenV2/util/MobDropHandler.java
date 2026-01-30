package org.m9mx.lumenV2.util;

import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.m9mx.lumenV2.item.ItemRegistry;
import org.m9mx.lumenV2.item.CustomItem;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Generic mob drop handler that allows configuring mob drops with items and chances.
 * Drops only occur if the mob is killed by a player.
 */
public class MobDropHandler implements Listener {
    
    private static final Random random = new Random();
    
    /**
     * Represents a drop configuration with entity type, item ID, and chance
     */
    public static class DropConfig {
        private final EntityType entityType;
        private final String itemId;
        private final double chance; // Between 0.0 and 1.0 (0% to 100%)
        
        public DropConfig(@NotNull EntityType entityType, @NotNull String itemId, double chance) {
            this.entityType = entityType;
            this.itemId = itemId;
            this.chance = Math.max(0.0, Math.min(1.0, chance)); // Clamp between 0.0 and 1.0
        }
        
        public EntityType getEntityType() {
            return entityType;
        }
        
        public String getItemId() {
            return itemId;
        }
        
        public double getChance() {
            return chance;
        }
    }
    
    private final List<DropConfig> dropConfigs = new ArrayList<>();
    
    /**
     * Add a drop configuration
     * @param dropConfig The drop configuration to add
     */
    public void addDropConfig(@NotNull DropConfig dropConfig) {
        dropConfigs.add(dropConfig);
    }
    
    /**
     * Handle entity death event and check for configured drops
     */
    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        // Check if the entity was killed by a player
        if (event.getEntity().getKiller() == null) {
            return;
        }
        
        Player killer = event.getEntity().getKiller();
        EntityType killedEntityType = event.getEntity().getType();
        
        // Check all configured drops for this entity type
        for (DropConfig config : dropConfigs) {
            if (config.getEntityType() == killedEntityType) {
                // Check if the drop should happen based on chance
                if (random.nextDouble() < config.getChance()) {
                    // Try to get the custom item from registry
                    ItemStack dropItem = null;
                    var customItem = ItemRegistry.getItemStatic(config.getItemId());
                    
                    if (customItem != null) {
                        dropItem = customItem.build();
                    }
                    
                    // If not found in registry, try to create as a basic material item
                    if (dropItem == null) {
                        try {
                            Material material = Material.valueOf(config.getItemId().toUpperCase());
                            dropItem = new ItemStack(material);
                        } catch (IllegalArgumentException e) {
                            // Log error if item doesn't exist in either registry or materials
                            System.out.println("Warning: Could not find item or material: " + config.getItemId());
                            continue;
                        }
                    }
                    
                    // Add the drop to the death event
                    event.getDrops().add(dropItem);
                }
            }
        }
    }
    
    /**
     * Remove all drop configurations for a specific entity type
     */
    public void removeDropConfigsForEntityType(@NotNull EntityType entityType) {
        dropConfigs.removeIf(config -> config.getEntityType() == entityType);
    }
}