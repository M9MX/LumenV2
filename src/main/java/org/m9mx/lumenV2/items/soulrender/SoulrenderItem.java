package org.m9mx.lumenV2.items.soulrender;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.m9mx.lumenV2.item.CustomItem;
import org.m9mx.lumenV2.item.ItemAttributes;
import org.m9mx.lumenV2.item.Recipe;
import org.m9mx.lumenV2.item.RecipeType;
import org.m9mx.lumenV2.systems.protection.ItemProtectionMode;

import io.papermc.paper.datacomponent.DataComponentTypes;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;

/**
 * Soulrender custom item definition for LumenV2.
 * A soul/death-themed diamond sword with abilities to summon skeletal horses and drop player skulls.
 */
public class SoulrenderItem extends CustomItem {

    public SoulrenderItem() {
        super("soulrender");
    }

    @Override
    protected void initialize() {
        setMaterial(Material.DIAMOND_SWORD);
        setItemModel("lumen:soulrender");
        setUnbreakable(true);
        setEnhancable(false);
        setProtectionMode(ItemProtectionMode.STRICT);

        MiniMessage miniMessage = MiniMessage.miniMessage();
        setDisplayName(miniMessage.deserialize("<dark_aqua><bold>Soulrender"));

        List<Component> lore = new ArrayList<>();
        lore.add(miniMessage.deserialize("<dark_aqua><bold>Stats:"));
        lore.add(miniMessage.deserialize("<aqua>  ◦ <reset><gray>Damage: <dark_aqua>9 <reset><gray>HP"));
        lore.add(miniMessage.deserialize("<aqua>  ◦ <reset><gray>Attack Speed: <dark_aqua>1.3"));
        lore.add(miniMessage.deserialize("<aqua>  ◦ <reset><gray>Range: <dark_aqua>3.5 <reset><gray>blocks"));
        lore.add(miniMessage.deserialize(" "));
        lore.add(miniMessage.deserialize("<dark_aqua><bold>Abilities:"));
        lore.add(miniMessage.deserialize("<aqua>  ◦ <reset><gray>Right-Click: Summon Skeletal Horse"));
        lore.add(miniMessage.deserialize("<aqua>  ◦ <reset><gray>Kill Player: Drop Player Skull"));
        lore.add(miniMessage.deserialize(" "));
        lore.add(miniMessage.deserialize("<dark_aqua><bold>Skeletal Horse:"));
        lore.add(miniMessage.deserialize("<aqua>  ◦ <reset><gray>Duration: <dark_aqua>90s"));
        lore.add(miniMessage.deserialize("<aqua>  ◦ <reset><gray>Cooldown: <dark_aqua>180s"));
        lore.add(miniMessage.deserialize("<aqua>  ◦ <reset><gray>Speed: <dark_aqua>16.0 <reset><gray>blocks/sec"));
        lore.add(miniMessage.deserialize("<aqua>  ◦ <reset><gray>Health: <dark_aqua>25.0 <reset><gray>hearts"));
        lore.add(miniMessage.deserialize(" "));
        lore.add(miniMessage.deserialize("<dark_aqua><bold>Soul Harvest:"));
        lore.add(miniMessage.deserialize("<aqua>  ◦ <reset><gray>Drop Chance: <dark_aqua>100%"));
        lore.add(miniMessage.deserialize("<aqua>  ◦ <reset><gray>Skull Format: <dark_aqua>[Victim Name]"));

        setLore(lore);

        // Set attributes
        ItemAttributes attrs = getAttributes();
        attrs.setAttackDamage(9.0); // 9 attack damage total (diamond sword base is 7, so +2 modifier)
        attrs.setAttackSpeed(1.3); // 1.3 attack speed (diamond sword base is 4, so -2.7 modifier)
        
        // Set entity interaction range to 3.5 (default is 3.0)
        attrs.setEntityInteractionRange(3.5);

        // Create ritual recipe for Soulrender
        Recipe recipe = new Recipe("soulrender_recipe", RecipeType.RITUAL);
        
        // Creating the specified recipe pattern:
        // x z x  (Lost Soul, 4 Netherite Ingots, Lost Soul)
        // c v b  (64 Soul Sand, Diamond Sword, 64 Soul Soil)
        // x v x  (Lost Soul, Diamond Sword, Lost Soul)
        recipe.setSlot(0, 0, "LUMEN:LOST_SOUL", 1);      // x - Lost Soul
        recipe.setSlot(0, 1, "NETHERITE_INGOT", 4);      // z - 4 Netherite Ingots
        recipe.setSlot(0, 2, "LUMEN:LOST_SOUL", 1);      // x - Lost Soul
        
        recipe.setSlot(1, 0, "SOUL_SAND", 64);           // c - 64 Soul Sand
        recipe.setSlot(1, 1, "DIAMOND_SWORD", 1);        // v - Diamond Sword
        recipe.setSlot(1, 2, "SOUL_SOIL", 64);           // b - 64 Soul Soil
        
        recipe.setSlot(2, 0, "LUMEN:LOST_SOUL", 1);      // x - Lost Soul
        recipe.setSlot(2, 1, "DIAMOND_SWORD", 1);        // v - Diamond Sword
        recipe.setSlot(2, 2, "LUMEN:LOST_SOUL", 1);      // x - Lost Soul
        
        recipe.setResult("soulrender");
        setRecipe(recipe);
    }

    /**
     * Check if an ItemStack is a Soulrender item
     */
    public static boolean isSoulrender(ItemStack item) {
        if (item == null || item.getType() != Material.DIAMOND_SWORD) return false;
        if (!item.hasItemMeta()) return false;

        // Try to get the item model
        Object model = item.getData(DataComponentTypes.ITEM_MODEL);
        if (model != null) {
            String modelStr = model.toString();
            if (modelStr.contains("soulrender")) {
                return true;
            }
        }

        // Fallback: check display name as backup
        ItemMeta meta = item.getItemMeta();
        if (meta != null && meta.hasDisplayName()) {
            String displayName = meta.getDisplayName().toLowerCase();
            return displayName.contains("soulrender");
        }

        return false;
    }
}