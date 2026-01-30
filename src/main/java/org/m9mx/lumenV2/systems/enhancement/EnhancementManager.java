package org.m9mx.lumenV2.systems.enhancement;

import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.m9mx.lumenV2.systems.EnhancementSystem;

/**
 * Manages enhancement data for items using NBT tags
 */
public class EnhancementManager {
    
    private static final NamespacedKey SHARD_COUNT_KEY = new NamespacedKey("lumen", "enhancement_shards");

    /**
     * Get the number of enhancement shards on an item
     */
    public static int getShardCount(ItemStack item) {
        if (item == null || item.isEmpty()) {
            return 0;
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return 0;
        }

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        Integer count = pdc.get(SHARD_COUNT_KEY, PersistentDataType.INTEGER);

        return count != null ? count : 0;
    }

    /**
     * Set the number of enhancement shards on an item
     */
    public static void setShardCount(ItemStack item, int count) {
        if (item == null || item.isEmpty()) {
            return;
        }
        
        // Clamp count between 0 and configured max
        int maxShards = EnhancementSystem.getInstance().getMaxShards();
        count = Math.max(0, Math.min(count, maxShards));

        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return;
        }

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(SHARD_COUNT_KEY, PersistentDataType.INTEGER, count);
        item.setItemMeta(meta);
    }

    /**
     * Add shards to an item
     */
    public static void addShards(ItemStack item, int amount) {
        int currentCount = getShardCount(item);
        setShardCount(item, currentCount + amount);
    }

    /**
     * Remove shards from an item
     */
    public static void removeShards(ItemStack item, int amount) {
        int currentCount = getShardCount(item);
        setShardCount(item, currentCount - amount);
    }

    /**
     * Clear all shards from an item
     */
    public static void clearShards(ItemStack item) {
        setShardCount(item, 0);
    }

    /**
     * Get the maximum number of shards an item can have
     */
    public static int getMaxShards() {
        return EnhancementSystem.getInstance().getMaxShards();
    }
    
    /**
     * Check if item can hold more shards
     */
    public static boolean canAddShard(ItemStack item) {
        return getShardCount(item) < getMaxShards();
    }
}
