package org.m9mx.lumenV2.util;

import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.m9mx.lumenV2.systems.protection.ItemProtectionMode;

/**
 * Utility methods for working with custom items
 */
public class ItemUtils {
    
    private static final NamespacedKey ITEM_ID_KEY = new NamespacedKey("lumen", "item_id");
    
    /**
     * Get the item ID from an ItemStack's NBT tags
     * Returns null if the item is not a custom item
     */
    public static String getItemId(ItemStack item) {
        if (item == null || item.isEmpty()) {
            return null;
        }
        
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return null;
        }
        
        String itemId = meta.getPersistentDataContainer().get(ITEM_ID_KEY, PersistentDataType.STRING);
        return itemId;
    }
    
    /**
     * Check if an item is a custom item
     */
    public static boolean isCustomItem(ItemStack item) {
        return getItemId(item) != null;
    }
    
    /**
     * Get protection mode for an item
     */
    public static ItemProtectionMode getProtectionMode(ItemStack item) {
        String itemId = getItemId(item);
        if (itemId == null) {
            return ItemProtectionMode.NONE;
        }
        return ItemDataHelper.getProtectionMode(itemId);
    }
    
    /**
     * Check if item is protected (SIMPLE or STRICT mode)
     */
    public static boolean isProtected(ItemStack item) {
        ItemProtectionMode mode = getProtectionMode(item);
        return mode != ItemProtectionMode.NONE;
    }
    
    /**
     * Check if item requires strict protection (STRICT mode)
     */
    public static boolean isStrictlyProtected(ItemStack item) {
        ItemProtectionMode mode = getProtectionMode(item);
        return mode == ItemProtectionMode.STRICT;
    }
}
