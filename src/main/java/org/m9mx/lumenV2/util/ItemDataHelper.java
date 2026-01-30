package org.m9mx.lumenV2.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.util.*;
import java.util.stream.Collectors;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

import org.bukkit.Material;
import org.m9mx.lumenV2.item.CustomItem;
import org.m9mx.lumenV2.item.ItemAttributes;
import org.m9mx.lumenV2.item.ItemRegistry;
import org.m9mx.lumenV2.item.Recipe;
import org.m9mx.lumenV2.systems.protection.ItemProtectionMode;

import net.kyori.adventure.text.Component;

/**
 * Helper class for forge and other systems to access item data by ID
 * All item properties can be accessed anywhere in the code using just the item ID
 */
public class ItemDataHelper {
    
    /**
     * Get a custom item by ID
     */
    public static CustomItem getItem(String itemId) {
        return ItemRegistry.getInstance().getItem(itemId);
    }
    
    // ===== Basic Properties =====
    
    /**
     * Check if item is enhancable
     */
    public static boolean isEnhancable(String itemId) {
        // Hardcoded enhancable items
        if ("ethereal_katana".equals(itemId)) {
            return true;
        }
        
        CustomItem item = getItem(itemId);
        return item != null && item.isEnhancable();
    }
    
    /**
     * Get display name of the item (with MiniMessage formatting)
     */
    public static Component getDisplayName(String itemId) {
        CustomItem item = getItem(itemId);
        return item != null ? item.getDisplayName() : null;
    }
    
    /**
     * Get lore of the item (multi-line with MiniMessage formatting)
     */
    public static List<Component> getLore(String itemId) {
        CustomItem item = getItem(itemId);
        return item != null ? item.getLore() : null;
    }
    
    /**
     * Get if item is unbreakable
     */
    public static boolean isUnbreakable(String itemId) {
        CustomItem item = getItem(itemId);
        return item != null && item.isUnbreakable();
    }
    
    /**
     * Get material of item
     */
    public static Material getMaterial(String itemId) {
        CustomItem item = getItem(itemId);
        return item != null ? item.getMaterial() : null;
    }
    
    /**
     * Get item model (e.g., "lumen:test")
     */
    public static String getItemModel(String itemId) {
        CustomItem item = getItem(itemId);
        return item != null ? item.getItemModel() : null;
    }
    
    // ===== Attributes =====
    
    /**
     * Get all attributes of the item
     */
    public static ItemAttributes getAttributes(String itemId) {
        CustomItem item = getItem(itemId);
        return item != null ? item.getAttributes() : null;
    }
    
    /**
     * Get attack damage attribute
     */
    public static double getAttackDamage(String itemId) {
        ItemAttributes attrs = getAttributes(itemId);
        return attrs != null ? attrs.getAttackDamage() : 0;
    }
    
    /**
     * Get attack speed attribute
     */
    public static double getAttackSpeed(String itemId) {
        ItemAttributes attrs = getAttributes(itemId);
        return attrs != null ? attrs.getAttackSpeed() : 0;
    }
    
    /**
     * Get armor attribute
     */
    public static double getArmor(String itemId) {
        ItemAttributes attrs = getAttributes(itemId);
        return attrs != null ? attrs.getArmor() : 0;
    }
    
    /**
     * Get health attribute
     */
    public static double getHealth(String itemId) {
        ItemAttributes attrs = getAttributes(itemId);
        return attrs != null ? attrs.getHealth() : 0;
    }
    
    /**
     * Get armor toughness attribute
     */
    public static double getArmorToughness(String itemId) {
        ItemAttributes attrs = getAttributes(itemId);
        return attrs != null ? attrs.getArmorToughness() : 0;
    }
    
    /**
     * Get attack knockback attribute
     */
    public static double getAttackKnockback(String itemId) {
        ItemAttributes attrs = getAttributes(itemId);
        return attrs != null ? attrs.getAttackKnockback() : 0;
    }
    
    /**
     * Get attack reach attribute
     */
    public static double getAttackReach(String itemId) {
        ItemAttributes attrs = getAttributes(itemId);
        return attrs != null ? attrs.getAttackReach() : 2.5; // Default is 2.5
    }
    
    /**
     * Get block break speed attribute
     */
    public static double getBlockBreakSpeed(String itemId) {
        ItemAttributes attrs = getAttributes(itemId);
        return attrs != null ? attrs.getBlockBreakSpeed() : 1; // Default is 1
    }
    
    /**
     * Get block interaction range attribute
     */
    public static double getBlockInteractionRange(String itemId) {
        ItemAttributes attrs = getAttributes(itemId);
        return attrs != null ? attrs.getBlockInteractionRange() : 4.5; // Default is 4.5
    }
    
    /**
     * Get burning time attribute
     */
    public static double getBurningTime(String itemId) {
        ItemAttributes attrs = getAttributes(itemId);
        return attrs != null ? attrs.getBurningTime() : 1; // Default is 1
    }
    
    /**
     * Get camera distance attribute
     */
    public static double getCameraDistance(String itemId) {
        ItemAttributes attrs = getAttributes(itemId);
        return attrs != null ? attrs.getCameraDistance() : 4; // Default is 4
    }
    
    /**
     * Get entity interaction range attribute
     */
    public static double getEntityInteractionRange(String itemId) {
        ItemAttributes attrs = getAttributes(itemId);
        return attrs != null ? attrs.getEntityInteractionRange() : 3; // Default is 3
    }
    
    /**
     * Get explosion knockback resistance attribute
     */
    public static double getExplosionKnockbackResistance(String itemId) {
        ItemAttributes attrs = getAttributes(itemId);
        return attrs != null ? attrs.getExplosionKnockbackResistance() : 0; // Default is 0
    }
    
    /**
     * Get fall damage multiplier attribute
     */
    public static double getFallDamageMultiplier(String itemId) {
        ItemAttributes attrs = getAttributes(itemId);
        return attrs != null ? attrs.getFallDamageMultiplier() : 1; // Default is 1
    }
    
    /**
     * Get flying speed attribute
     */
    public static double getFlyingSpeed(String itemId) {
        ItemAttributes attrs = getAttributes(itemId);
        return attrs != null ? attrs.getFlyingSpeed() : 0.4; // Default is 0.4
    }
    
    /**
     * Get follow range attribute
     */
    public static double getFollowRange(String itemId) {
        ItemAttributes attrs = getAttributes(itemId);
        return attrs != null ? attrs.getFollowRange() : 32; // Default is 32
    }
    
    /**
     * Get gravity attribute
     */
    public static double getGravity(String itemId) {
        ItemAttributes attrs = getAttributes(itemId);
        return attrs != null ? attrs.getGravity() : 0.08; // Default is 0.08
    }
    
    /**
     * Get jump strength attribute
     */
    public static double getJumpStrength(String itemId) {
        ItemAttributes attrs = getAttributes(itemId);
        return attrs != null ? attrs.getJumpStrength() : 0.42; // Default is 0.42
    }
    
    /**
     * Get knockback resistance attribute
     */
    public static double getKnockbackResistance(String itemId) {
        ItemAttributes attrs = getAttributes(itemId);
        return attrs != null ? attrs.getKnockbackResistance() : 0; // Default is 0
    }
    
    /**
     * Get luck attribute
     */
    public static double getLuck(String itemId) {
        ItemAttributes attrs = getAttributes(itemId);
        return attrs != null ? attrs.getLuck() : 0; // Default is 0
    }
    
    /**
     * Get max absorption attribute
     */
    public static double getMaxAbsorption(String itemId) {
        ItemAttributes attrs = getAttributes(itemId);
        return attrs != null ? attrs.getMaxAbsorption() : 0; // Default is 0
    }
    
    /**
     * Get max health attribute
     */
    public static double getMaxHealth(String itemId) {
        ItemAttributes attrs = getAttributes(itemId);
        return attrs != null ? attrs.getMaxHealth() : 20; // Default is 20
    }
    
    /**
     * Get mining efficiency attribute
     */
    public static double getMiningEfficiency(String itemId) {
        ItemAttributes attrs = getAttributes(itemId);
        return attrs != null ? attrs.getMiningEfficiency() : 0; // Default is 0
    }
    
    /**
     * Get movement efficiency attribute
     */
    public static double getMovementEfficiency(String itemId) {
        ItemAttributes attrs = getAttributes(itemId);
        return attrs != null ? attrs.getMovementEfficiency() : 0; // Default is 0
    }
    
    /**
     * Get movement speed attribute
     */
    public static double getMovementSpeed(String itemId) {
        ItemAttributes attrs = getAttributes(itemId);
        return attrs != null ? attrs.getMovementSpeed() : 0.7; // Default is 0.7
    }
    
    /**
     * Get oxygen bonus attribute
     */
    public static double getOxygenBonus(String itemId) {
        ItemAttributes attrs = getAttributes(itemId);
        return attrs != null ? attrs.getOxygenBonus() : 0; // Default is 0
    }
    
    /**
     * Get safe fall distance attribute
     */
    public static double getSafeFallDistance(String itemId) {
        ItemAttributes attrs = getAttributes(itemId);
        return attrs != null ? attrs.getSafeFallDistance() : 3; // Default is 3
    }
    
    /**
     * Get scale attribute
     */
    public static double getScale(String itemId) {
        ItemAttributes attrs = getAttributes(itemId);
        return attrs != null ? attrs.getScale() : 1; // Default is 1
    }
    
    /**
     * Get spawn reinforcements attribute
     */
    public static double getSpawnReinforcements(String itemId) {
        ItemAttributes attrs = getAttributes(itemId);
        return attrs != null ? attrs.getSpawnReinforcements() : 0; // Default is 0
    }
    
    /**
     * Get sneaking speed attribute
     */
    public static double getSneakingSpeed(String itemId) {
        ItemAttributes attrs = getAttributes(itemId);
        return attrs != null ? attrs.getSneakingSpeed() : 0.3; // Default is 0.3
    }
    
    /**
     * Get step height attribute
     */
    public static double getStepHeight(String itemId) {
        ItemAttributes attrs = getAttributes(itemId);
        return attrs != null ? attrs.getStepHeight() : 0.6; // Default is 0.6
    }
    
    /**
     * Get submerged mining speed attribute
     */
    public static double getSubmergedMiningSpeed(String itemId) {
        ItemAttributes attrs = getAttributes(itemId);
        return attrs != null ? attrs.getSubmergedMiningSpeed() : 0.2; // Default is 0.2
    }
    
    /**
     * Get sweeping damage ratio attribute
     */
    public static double getSweepingDamageRatio(String itemId) {
        ItemAttributes attrs = getAttributes(itemId);
        return attrs != null ? attrs.getSweepingDamageRatio() : 0; // Default is 0
    }
    
    /**
     * Get tempt range attribute
     */
    public static double getTemptRange(String itemId) {
        ItemAttributes attrs = getAttributes(itemId);
        return attrs != null ? attrs.getTemptRange() : 10; // Default is 10
    }
    
    /**
     * Get water movement efficiency attribute
     */
    public static double getWaterMovementEfficiency(String itemId) {
        ItemAttributes attrs = getAttributes(itemId);
        return attrs != null ? attrs.getWaterMovementEfficiency() : 0; // Default is 0
    }
    
    /**
     * Get waypoint receive range attribute
     */
    public static double getWaypointReceiveRange(String itemId) {
        ItemAttributes attrs = getAttributes(itemId);
        return attrs != null ? attrs.getWaypointReceiveRange() : 0; // Default is 0
    }
    
    /**
     * Get waypoint transmit range attribute
     */
    public static double getWaypointTransmitRange(String itemId) {
        ItemAttributes attrs = getAttributes(itemId);
        return attrs != null ? attrs.getWaypointTransmitRange() : 0; // Default is 0
    }
    
    // ===== Recipe =====
    
    /**
     * Get recipe for an item by item ID
     */
    public static Recipe getRecipe(String itemId) {
        CustomItem item = getItem(itemId);
        if (item != null && item.hasRecipe()) {
            return item.getRecipe();
        }
        return null;
    }
    
    /**
     * Get required ingredients for an item (itemId -> amount)
     * Returns null if item has no recipe
     */
    public static Map<String, Integer> getRequiredIngredients(String itemId) {
        Recipe recipe = getRecipe(itemId);
        if (recipe != null) {
            return recipe.getRequiredIngredients();
        }
        return null;
    }
    
    /**
     * Check if item has a recipe
     */
    public static boolean hasRecipe(String itemId) {
        CustomItem item = getItem(itemId);
        return item != null && item.hasRecipe();
    }
    
    // ===== Enhancement & Protection =====
    
    /**
     * Get shard count for an ItemStack
     */
    public static int getShardCount(org.bukkit.inventory.ItemStack itemStack) {
        return org.m9mx.lumenV2.systems.enhancement.EnhancementManager.getShardCount(itemStack);
    }
    
    /**
     * Set shard count for an ItemStack
     */
    public static void setShardCount(org.bukkit.inventory.ItemStack itemStack, int count) {
        org.m9mx.lumenV2.systems.enhancement.EnhancementManager.setShardCount(itemStack, count);
    }
    
    /**
     * Add shards to an ItemStack
     */
    public static void addShards(org.bukkit.inventory.ItemStack itemStack, int amount) {
        org.m9mx.lumenV2.systems.enhancement.EnhancementManager.addShards(itemStack, amount);
    }
    
    /**
     * Remove shards from an ItemStack
     */
    public static void removeShards(org.bukkit.inventory.ItemStack itemStack, int amount) {
        org.m9mx.lumenV2.systems.enhancement.EnhancementManager.removeShards(itemStack, amount);
    }
    
    /**
     * Get max shards allowed on an item
     */
    public static int getMaxShards() {
        return org.m9mx.lumenV2.systems.enhancement.EnhancementManager.getMaxShards();
    }
    
    /**
     * Check if item can hold more shards
     */
    public static boolean canAddShard(org.bukkit.inventory.ItemStack itemStack) {
        return org.m9mx.lumenV2.systems.enhancement.EnhancementManager.canAddShard(itemStack);
    }
    
    /**
     * Get protection mode of the item
     */
    public static ItemProtectionMode getProtectionMode(String itemId) {
        CustomItem item = getItem(itemId);
        return item != null ? item.getProtectionMode() : ItemProtectionMode.NONE;
    }
    
    /**
     * Serialize an ItemStack to a Base64 string
     */
    public static String serializeItem(ItemStack item) {
        if (item == null) {
            return "";
        }
        
        try {
            YamlConfiguration config = new YamlConfiguration();
            config.set("i", item);
            String yamlStr = config.saveToString();
            return Base64.getEncoder().encodeToString(yamlStr.getBytes());
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }
    
    /**
     * Deserialize an ItemStack from a Base64 string
     */
    public static ItemStack deserializeItem(String serializedItem) {
        if (serializedItem == null || serializedItem.isEmpty()) {
            return null;
        }
        
        try {
            byte[] data = Base64.getDecoder().decode(serializedItem);
            String yamlStr = new String(data);
            YamlConfiguration yaml = YamlConfiguration.loadConfiguration(new StringReader(yamlStr));
            return yaml.getItemStack("i");
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * Serialize a list of ItemStacks to a string
     */
    public static String serializeItemList(List<ItemStack> items) {
        if (items == null || items.isEmpty()) {
            return "";
        }
        
        List<String> serializedItems = new ArrayList<>();
        for (ItemStack item : items) {
            serializedItems.add(serializeItem(item));
        }
        
        return String.join(";;", serializedItems);
    }
    
    /**
     * Deserialize a string back to a list of ItemStacks
     */
    public static List<ItemStack> deserializeItemList(String serializedList) {
        if (serializedList == null || serializedList.isEmpty()) {
            return new ArrayList<>();
        }
        
        String[] parts = serializedList.split(";;");
        List<ItemStack> items = new ArrayList<>();
        
        for (String part : parts) {
            items.add(deserializeItem(part));
        }
        
        return items;
    }

    // ===== Utilities =====
    
    /**
     * Check if item exists
     */
    public static boolean itemExists(String itemId) {
        return ItemRegistry.getInstance().exists(itemId);
    }
    
    /**
     * Create an ItemStack for the given item ID
     */
    public static org.bukkit.inventory.ItemStack createItem(String itemId) {
        return ItemRegistry.getInstance().createItem(itemId);
    }
    
    /**
     * Get all registered item IDs
     */
    public static Set<String> getAllItemIds() {
        return ItemRegistry.getInstance().getAllItemIds();
    }
    
    /**
     * Get all registered item objects
     */
    public static Collection<CustomItem> getAllItems() {
        return ItemRegistry.getInstance().getAllItems();
    }
    
    /**
     * Get total number of registered items
     */
    public static int getItemCount() {
        return ItemRegistry.getInstance().getItemCount();
    }
}