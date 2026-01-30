package org.m9mx.lumenV2.items.awakenedlichblade;

import org.bukkit.Material;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.m9mx.lumenV2.item.CustomItem;
import org.m9mx.lumenV2.item.Recipe;
import org.m9mx.lumenV2.item.RecipeType;
import org.m9mx.lumenV2.systems.ItemProtection;
import org.m9mx.lumenV2.systems.protection.ItemProtectionMode;

import java.util.Arrays;
import java.util.List;

public class AwakenedLichbladeItem extends CustomItem {

    public AwakenedLichbladeItem() {
        super("awakened_lichblade");
    }

    @Override
    protected void initialize() {
        setMaterial(Material.DIAMOND_SWORD);
        setItemModel("lumen:awakened_lichblade");
        setUnbreakable(true);
        setEnhancable(true); // Enable enhancement support
        setProtectionMode(ItemProtectionMode.STRICT);

        MiniMessage miniMessage = MiniMessage.miniMessage();
        setDisplayName(miniMessage.deserialize("<dark_aqua><bold>Awakened Lichblade"));

        List<net.kyori.adventure.text.Component> lore = Arrays.asList(
            miniMessage.deserialize("<dark_aqua><bold>Stats:"),
            miniMessage.deserialize("<aqua>  ◦ <reset><gray>Damage: <dark_aqua>10 <reset><gray>HP"),
            miniMessage.deserialize("<aqua>  ◦ <reset><gray>Attack Speed: <dark_aqua>1.0"),
            miniMessage.deserialize(" "),
            miniMessage.deserialize("<dark_aqua><bold>Abilities:"),
            miniMessage.deserialize("<aqua>  ◦ <reset><gray>Right-Click: Sonic Boom"),
            miniMessage.deserialize("<aqua>  ◦ <reset><gray>Swap Hands (F): Blinding Pulse"),
            miniMessage.deserialize(" "),
            miniMessage.deserialize("<dark_aqua><bold>Sonic Boom:"),
            miniMessage.deserialize("<aqua>  ◦ <reset><gray>Damage: <dark_aqua>10 <reset><gray>HP (ignores armor) + <dark_aqua>1 <reset><gray>per shard"),
            miniMessage.deserialize("<aqua>  ◦ <reset><gray>Range: <dark_aqua>10 <reset><gray>blocks + <dark_aqua>2 <reset><gray>per shard"),
            miniMessage.deserialize("<aqua>  ◦ <reset><gray>Effect: Pierces through blocks"),
            miniMessage.deserialize("<aqua>  ◦ <reset><gray>Cooldown: <dark_aqua>40s"),
            miniMessage.deserialize(" "),
            miniMessage.deserialize("<dark_aqua><bold>Blinding Pulse:"),
            miniMessage.deserialize("<aqua>  ◦ <reset><gray>Radius: <dark_aqua>50 <reset><gray>blocks"),
            miniMessage.deserialize("<aqua>  ◦ <reset><gray>Effect: Blindness with distance-based duration"),
            miniMessage.deserialize("<aqua>  ◦ <reset><gray>Duration: 0-2 blocks: 60s, 50 blocks: 1s"),
            miniMessage.deserialize("<aqua>  ◦ <reset><gray>Cooldown: <dark_aqua>80s"),
            miniMessage.deserialize(" "),
            miniMessage.deserialize("<dark_gray>Shift + Right-Click: Enhancement Slot"),
            miniMessage.deserialize(" "),
            miniMessage.deserialize("<dark_gray><italic>Origin:"),
            miniMessage.deserialize("<aqua>  ◦ <reset><gray><italic>Ritual crafted with Warden Heart")
        );

        setLore(lore);

        // Set attributes
        var attrs = getAttributes();
        attrs.setAttackDamage(10.0); // Updated to match the displayed damage
        attrs.setAttackSpeed(1.0); // Default attack speed

        // Create ritual recipe
        Recipe recipe = new Recipe("awakened_lichblade_recipe", RecipeType.RITUAL);

        // Grid layout (0,1,2 = top row; 3,4,5 = middle row; 6,7,8 = bottom row)
        // Row 0: Bone Block (8), Sculk Shrieker (1), Bone Block (8)
        recipe.setSlot(0, 0, "BONE_BLOCK", 8);
        recipe.setSlot(0, 1, "SCULK_SHRIEKER", 1);
        recipe.setSlot(0, 2, "BONE_BLOCK", 8);

        // Row 1: Sculk Block (16), Warden Heart (1), Sculk Block (16)
        recipe.setSlot(1, 0, "SCULK", 16);
        recipe.setSlot(1, 1, "warden_heart", 1); // Custom item reference
        recipe.setSlot(1, 2, "SCULK", 16);

        // Row 2: Bone Block (8), Diamond Sword (1), Bone Block (8)
        recipe.setSlot(2, 0, "BONE_BLOCK", 8);
        recipe.setSlot(2, 1, "DIAMOND_SWORD", 1);
        recipe.setSlot(2, 2, "BONE_BLOCK", 8);

        recipe.setResult("awakened_lichblade");
        setRecipe(recipe);
    }

    /**
     * Check if an ItemStack is an Awakened Lichblade
     */
    public static boolean isAwakenedLichblade(org.bukkit.inventory.ItemStack item) {
        if (item == null || item.getType() != Material.DIAMOND_SWORD) return false;
        if (!item.hasItemMeta()) return false;
        var meta = item.getItemMeta();
        if (meta == null) return false;
        
        // Check for the item ID in the persistent data container
        org.bukkit.persistence.PersistentDataContainer pdc = meta.getPersistentDataContainer();
        org.bukkit.NamespacedKey key = new org.bukkit.NamespacedKey("lumen", "item_id");
        String itemId = pdc.get(key, org.bukkit.persistence.PersistentDataType.STRING);
        
        return "awakened_lichblade".equals(itemId);
    }

    /**
     * Calculate enhanced range based on shard count
     */
    public double calculateEnhancedRange(double baseRange, int shardCount) {
        return baseRange + (shardCount * 2); // Each shard adds 2 to the range
    }
}