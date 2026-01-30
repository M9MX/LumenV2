package org.m9mx.lumenV2.items.lotusblossom;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.m9mx.lumenV2.item.CustomItem;
import org.m9mx.lumenV2.item.ItemAttributes;
import org.m9mx.lumenV2.item.Recipe;
import org.m9mx.lumenV2.item.RecipeType;
import org.m9mx.lumenV2.systems.protection.ItemProtectionMode;

import io.papermc.paper.datacomponent.DataComponentTypes;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;

/**
 * Lotus Blossom - An eastern floral/nature-themed diamond sword with defensive abilities.
 * Right-Click: Blossom Shield - creates 5 rotating spore blossom displays around player for 10 seconds,
 *             halves damage taken and reflects 1.5x damage back to attacker.
 */
public class LotusBlossomItem extends CustomItem {
    private static final NamespacedKey LOTUS_BLOSSOM_KEY = new NamespacedKey("lumen", "lotus_blossom");

    public LotusBlossomItem() {
        super("lotus_blossom");
    }

    @Override
    protected void initialize() {
        setMaterial(Material.DIAMOND_SWORD);
        setItemModel("lumen:lotus_blossom");
        setUnbreakable(true);
        setEnhancable(false);
        setProtectionMode(ItemProtectionMode.STRICT);
        
        MiniMessage miniMessage = MiniMessage.miniMessage();
        setDisplayName(miniMessage.deserialize("<light_purple><bold>Lotus Blossom"));

        List<Component> lore = new ArrayList<>();
        lore.add(miniMessage.deserialize("<light_purple><bold>Stats:"));
        lore.add(miniMessage.deserialize("<light_purple>  ◦ <reset><gray>Damage: <light_purple>7 <reset><gray>HP"));
        lore.add(miniMessage.deserialize("<light_purple>  ◦ <reset><gray>Attack Speed: <light_purple>1.6"));
        lore.add(miniMessage.deserialize(" "));
        lore.add(miniMessage.deserialize("<light_purple><bold>Abilities:"));
        lore.add(miniMessage.deserialize("<light_purple>  ◦ <reset><gray>Right-Click: Blossom Shield"));
        lore.add(miniMessage.deserialize(" "));
        lore.add(miniMessage.deserialize("<light_purple><bold>Blossom Shield:"));
        lore.add(miniMessage.deserialize("<light_purple>  ◦ <reset><gray>Duration: <light_purple>10 <reset><gray>seconds"));
        lore.add(miniMessage.deserialize("<light_purple>  ◦ <reset><gray>Effect: <light_purple>16 <reset><gray>rotating spore blossoms in 2 circles"));
        lore.add(miniMessage.deserialize("<light_purple>  ◦ <reset><gray>Damage Reduction: <light_purple>50%"));
        lore.add(miniMessage.deserialize("<light_purple>  ◦ <reset><gray>Thorns: <light_purple>1.5x <reset><gray>reflected damage"));
        lore.add(miniMessage.deserialize("<light_purple>  ◦ <reset><gray>Cooldown: <light_purple>60s"));
        setLore(lore);

        // Set attributes
        ItemAttributes attrs = getAttributes();
        attrs.setAttackDamage(7.0); // Player base is 1.0, so 7.0 gives +6 modifier
        attrs.setAttackSpeed(1.6); // Diamond sword base speed

        // Create ritual recipe
        Recipe recipe = new Recipe("lotus_blossom_recipe", RecipeType.RITUAL);

        // Grid layout (0,1,2 = top row; 3,4,5 = middle row; 6,7,8 = bottom row):
        // Row 0: Red Tulip (16) | Blocks of Iron (16) | Pink Tulip (16)
        recipe.setSlot(0, 0, "RED_TULIP", 16);
        recipe.setSlot(0, 1, "IRON_BLOCK", 16);
        recipe.setSlot(0, 2, "PINK_TULIP", 16);

        // Row 1: Spore Blossom (8) | Pink Petals (16) | Spore Blossom (8)
        recipe.setSlot(1, 0, "SPORE_BLOSSOM", 8);
        recipe.setSlot(1, 1, "PINK_PETALS", 16);
        recipe.setSlot(1, 2, "SPORE_BLOSSOM", 8);

        // Row 2: Orange Tulip (16) | Diamond Sword (1) | White Tulip (16)
        recipe.setSlot(2, 0, "ORANGE_TULIP", 16);
        recipe.setSlot(2, 1, "DIAMOND_SWORD", 1);
        recipe.setSlot(2, 2, "WHITE_TULIP", 16);

        recipe.setResult("lotus_blossom");
        setRecipe(recipe);
    }

    @Override
    public ItemStack build() {
        ItemStack item = super.build();
        
        // Initialize any persistent data if needed
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            PersistentDataContainer pdc = meta.getPersistentDataContainer();
            pdc.set(LOTUS_BLOSSOM_KEY, PersistentDataType.STRING, "lotus_blossom");
            item.setItemMeta(meta);
        }
        
        return item;
    }

    /**
     * Check if an ItemStack is a Lotus Blossom sword
     */
    public static boolean isLotusBlossom(ItemStack item) {
        if (item == null || item.getType() != Material.DIAMOND_SWORD) return false;
        if (!item.hasItemMeta()) return false;
        Object model = item.getData(DataComponentTypes.ITEM_MODEL);
        if (model == null) return false;
        return model.toString().contains("lotus_blossom");
    }
}
