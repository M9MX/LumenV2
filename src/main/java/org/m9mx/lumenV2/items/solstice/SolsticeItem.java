package org.m9mx.lumenV2.items.solstice;

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
 * Solstice - A mythical sword with sun and light themed abilities.
 * - Passive: Day/night cycle modulation of damage.
 * - Active: Sunbeam ability - spawns a damaging pillar of sunlight.
 * - Enhancement: Supports up to 5 Catalyst Shards.
 */
public class SolsticeItem extends CustomItem {
    private static final NamespacedKey SOLSTICE_KEY = new NamespacedKey("lumen", "solstice");

    public SolsticeItem() {
        super("solstice");
    }

    @Override
    protected void initialize() {
        setMaterial(Material.DIAMOND_SWORD);
        setItemModel("lumen:solstice");
        setUnbreakable(true);
        setEnhancable(true);
        setProtectionMode(ItemProtectionMode.STRICT);
        
        MiniMessage miniMessage = MiniMessage.miniMessage();
        setDisplayName(miniMessage.deserialize("<gold><bold>Solstice"));

        List<Component> lore = new ArrayList<>();
        lore.add(miniMessage.deserialize("<gold><bold>Stats:"));
        lore.add(miniMessage.deserialize("<yellow>  ◦ <reset><gray>Damage: <gold>7 <reset><gray>HP"));
        lore.add(miniMessage.deserialize("<yellow>  ◦ <reset><gray>Attack Speed: <gold>1.6"));
        lore.add(miniMessage.deserialize(" "));
        lore.add(miniMessage.deserialize("<gold><bold>Abilities:"));
        lore.add(miniMessage.deserialize("<yellow>  ◦ <reset><gray>Right-Click: Sunbeam"));
        lore.add(miniMessage.deserialize("<yellow>  ◦ <reset><gray>Passive: Day/Night Modulation"));
        lore.add(miniMessage.deserialize(" "));
        lore.add(miniMessage.deserialize("<gold><bold>Sunbeam:"));
        lore.add(miniMessage.deserialize("<yellow>  ◦ <reset><gray>Damage: <gold>6 <reset><gray>HP + <gold>1 <reset><gray>per shard"));
        lore.add(miniMessage.deserialize("<yellow>  ◦ <reset><gray>Radius: <gold>3 <reset><gray>blocks"));
        lore.add(miniMessage.deserialize("<yellow>  ◦ <reset><gray>Cooldown: <gold>120s"));
        lore.add(miniMessage.deserialize(" "));
        lore.add(miniMessage.deserialize("<gold><bold>Passive:"));
        lore.add(miniMessage.deserialize("<yellow>  ◦ <reset><gray>Night: <gold>+10% <reset><gray>damage"));
        lore.add(miniMessage.deserialize("<yellow>  ◦ <reset><gray>Day: <gold>-10% <reset><gray>damage"));
        lore.add(miniMessage.deserialize(" "));
        lore.add(miniMessage.deserialize("<dark_gray>Shift + Right-Click: Enhancement Slot"));
        setLore(lore);

        // Set attributes
        ItemAttributes attrs = getAttributes();
        attrs.setAttackDamage(7.0); // Player base is 1.0, so 7.0 gives +6 modifier
        attrs.setAttackSpeed(1.6); // Diamond sword base speed
        
        // Create ritual recipe
        Recipe recipe = new Recipe("solstice_recipe", RecipeType.RITUAL);
        
        // Grid layout (0,1,2 = top row; 3,4,5 = middle row; 6,7,8 = bottom row)
        // Row 0: Diamond Block (4), Music Disc (any), Diamond Block (4)
        recipe.setSlot(0, 0, "DIAMOND_BLOCK", 4);
        recipe.setSlot(0, 1, "MUSIC_DISC_TEARS", 1); // Using "Otherside" disc (Zefiras)
        recipe.setSlot(0, 2, "DIAMOND_BLOCK", 4);
        
        // Row 1: Gold Block (16), Diamond Sword (1), Gold Block (16)
        recipe.setSlot(1, 0, "GOLD_BLOCK", 16);
        recipe.setSlot(1, 1, "DIAMOND_SWORD", 1);
        recipe.setSlot(1, 2, "GOLD_BLOCK", 16);
        
        // Row 2: Dune Trim (1), Blaze Rod (16), Dune Trim (1)
        recipe.setSlot(2, 0, "DUNE_ARMOR_TRIM_SMITHING_TEMPLATE", 1);
        recipe.setSlot(2, 1, "BLAZE_ROD", 16);
        recipe.setSlot(2, 2, "DUNE_ARMOR_TRIM_SMITHING_TEMPLATE", 1);
        
        recipe.setResult("solstice");
        setRecipe(recipe);
    }

    @Override
    public ItemStack build() {
        ItemStack item = super.build();
        
        // Initialize any persistent data if needed
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            PersistentDataContainer pdc = meta.getPersistentDataContainer();
            pdc.set(SOLSTICE_KEY, PersistentDataType.STRING, "solstice");
            item.setItemMeta(meta);
        }
        
        return item;
    }

    /**
     * Check if an ItemStack is a Solstice sword
     */
    public static boolean isSolstice(ItemStack item) {
        if (item == null || item.getType() != Material.DIAMOND_SWORD) return false;
        if (!item.hasItemMeta()) return false;
        Object model = item.getData(DataComponentTypes.ITEM_MODEL);
        if (model == null) return false;
        return model.toString().contains("solstice");
    }

    /**
     * Calculate enhanced damage based on shard count
     */
    public double calculateEnhancedDamage(double baseDamage, int shardCount) {
        return baseDamage + shardCount;
    }
}
