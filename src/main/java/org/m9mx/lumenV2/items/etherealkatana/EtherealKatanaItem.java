package org.m9mx.lumenV2.items.etherealkatana;

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
 * Ethereal Katana - A gray/purple katana with soul mechanics and spectral abilities.
 * - Souls: Gain +5 per player kill, +1 per 10 mob kills (max 20)
 * - Right-Click: Spectral Dash (1 soul, 30s cooldown)
 * - Swap Hands (F): Spectral Nova (5 souls, 120s cooldown)
 */
public class EtherealKatanaItem extends CustomItem {
    private static final NamespacedKey SOULS_KEY = new NamespacedKey("lumen", "ethereal_souls");

    public EtherealKatanaItem() {
        super("ethereal_katana");
    }

    @Override
    protected void initialize() {
        setMaterial(Material.DIAMOND_SWORD);
        setItemModel("lumen:etherealkatana_standard");
        setUnbreakable(true);
        setEnhancable(true);
        setProtectionMode(ItemProtectionMode.STRICT);
        
        MiniMessage miniMessage = MiniMessage.miniMessage();
        setDisplayName(miniMessage.deserialize("<dark_purple><bold>Ethereal Katana"));

        List<Component> lore = new ArrayList<>();
        lore.add(miniMessage.deserialize("<dark_purple><bold>Stats:"));
        lore.add(miniMessage.deserialize("<light_purple>  ◦ <reset><gray>Damage: <light_purple>6 <reset><gray>HP"));
        lore.add(miniMessage.deserialize("<light_purple>  ◦ <reset><gray>Attack Speed: <light_purple>2.0"));
        lore.add(miniMessage.deserialize(" "));
        lore.add(miniMessage.deserialize("<dark_purple><bold>Abilities:"));
        lore.add(miniMessage.deserialize("<light_purple>  ◦ <reset><gray>Right-Click: Spectral Dash"));
        lore.add(miniMessage.deserialize("<light_purple>  ◦ <reset><gray>Swap Hands (F): Spectral Nova"));
        lore.add(miniMessage.deserialize(" "));
        lore.add(miniMessage.deserialize("<dark_purple><bold>Spectral Dash:"));
        lore.add(miniMessage.deserialize("<light_purple>  ◦ <reset><gray>Cost: <light_purple>1 <reset><gray>soul"));
        lore.add(miniMessage.deserialize("<light_purple>  ◦ <reset><gray>Range: <light_purple>8 <reset><gray>blocks + <light_purple>1 <reset><gray>per shard"));
        lore.add(miniMessage.deserialize("<light_purple>  ◦ <reset><gray>Damage: <light_purple>2 <reset><gray>HP (ignores armor) + <light_purple>1 <reset><gray>per shard"));
        lore.add(miniMessage.deserialize("<light_purple>  ◦ <reset><gray>Cooldown: <light_purple>30s"));
        lore.add(miniMessage.deserialize(" "));
        lore.add(miniMessage.deserialize("<dark_purple><bold>Spectral Nova:"));
        lore.add(miniMessage.deserialize("<light_purple>  ◦ <reset><gray>Cost: <light_purple>5 <reset><gray>souls"));
        lore.add(miniMessage.deserialize("<light_purple>  ◦ <reset><gray>Radius: <light_purple>6 <reset><gray>blocks + <light_purple>1 <reset><gray>per shard"));
        lore.add(miniMessage.deserialize("<light_purple>  ◦ <reset><gray>Damage: <light_purple>4 <reset><gray>HP (ignores armor) + <light_purple>1 <reset><gray>per shard"));
        lore.add(miniMessage.deserialize("<light_purple>  ◦ <reset><gray>Cooldown: <light_purple>120s"));
        lore.add(miniMessage.deserialize(" "));
        lore.add(miniMessage.deserialize("<dark_purple><bold>Soul System:"));
        lore.add(miniMessage.deserialize("<light_purple>  ◦ <reset><gray>Player Kill: <light_purple>+5 <reset><gray>souls"));
        lore.add(miniMessage.deserialize("<light_purple>  ◦ <reset><gray>Mob Kill: <light_purple>+1 <reset><gray>soul per 10 kills"));
        lore.add(miniMessage.deserialize("<light_purple>  ◦ <reset><gray>Max Souls: <light_purple>20"));
        lore.add(miniMessage.deserialize(" "));
        lore.add(miniMessage.deserialize("<dark_gray>Shift + Right-Click: Enhancement Slot"));
        setLore(lore);

        // Set attributes
        ItemAttributes attrs = getAttributes();
        attrs.setAttackDamage(6.0); // Player base is 1.0, so 6.0 gives +5 modifier
        attrs.setAttackSpeed(2.0); // Player base is 4.0, so 2.0 gives -2 modifier
        
        // Create ritual recipe
        Recipe recipe = new Recipe("ethereal_katana_recipe", RecipeType.RITUAL);
        
        // Grid layout (0,1,2 = top row; 3,4,5 = middle row; 6,7,8 = bottom row)
        // Row 0: Eye Trim (1), Chorus Fruit (64), Eye Trim (1)
        recipe.setSlot(0, 0, "EYE_ARMOR_TRIM_SMITHING_TEMPLATE", 1);
        recipe.setSlot(0, 1, "CHORUS_FRUIT", 64);
        recipe.setSlot(0, 2, "EYE_ARMOR_TRIM_SMITHING_TEMPLATE", 1);
        
        // Row 1: Ender Pearl (16), Dragon Egg (64), Ender Pearl (16)
        recipe.setSlot(1, 0, "ENDER_PEARL", 16);
        recipe.setSlot(1, 1, "DRAGON_EGG", 1);
        recipe.setSlot(1, 2, "ENDER_PEARL", 16);
        
        // Row 2: Spire Trim (1), Breeze Rod (16), Spire Trim (1)
        recipe.setSlot(2, 0, "SPIRE_ARMOR_TRIM_SMITHING_TEMPLATE", 1);
        recipe.setSlot(2, 1, "BREEZE_ROD", 16);
        recipe.setSlot(2, 2, "SPIRE_ARMOR_TRIM_SMITHING_TEMPLATE", 1);
        
        recipe.setResult("ethereal_katana");
        setRecipe(recipe);
    }

    @Override
    public ItemStack build() {
        ItemStack item = super.build();
        
        // Initialize souls to 0
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            PersistentDataContainer pdc = meta.getPersistentDataContainer();
            pdc.set(SOULS_KEY, PersistentDataType.INTEGER, 0);
            item.setItemMeta(meta);
        }
        
        return item;
    }

    /**
     * Check if an ItemStack is an Ethereal Katana
     */
    public static boolean isEtherealKatana(ItemStack item) {
        if (item == null || item.getType() != Material.DIAMOND_SWORD) return false;
        if (!item.hasItemMeta()) return false;
        Object model = item.getData(DataComponentTypes.ITEM_MODEL);
        if (model == null) return false;
        return model.toString().contains("etherealkatana");
    }

    /**
     * Get the number of souls on an Ethereal Katana
     */
    public static int getSouls(ItemStack item) {
        if (!isEtherealKatana(item) || !item.hasItemMeta()) return 0;
        PersistentDataContainer pdc = item.getItemMeta().getPersistentDataContainer();
        return pdc.getOrDefault(SOULS_KEY, PersistentDataType.INTEGER, 0);
    }

    /**
     * Set the number of souls on an Ethereal Katana
     */
    public static void setSouls(ItemStack item, int souls) {
        if (!isEtherealKatana(item) || !item.hasItemMeta()) return;
        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        int clamped = Math.max(0, Math.min(20, souls));
        pdc.set(SOULS_KEY, PersistentDataType.INTEGER, clamped);
        updateSoulLore(meta, clamped);
        item.setItemMeta(meta);
        updateModelForSouls(item);
    }

    /**
     * Add souls to an Ethereal Katana
     */
    public static void addSouls(ItemStack item, int amount) {
        if (!isEtherealKatana(item) || !item.hasItemMeta()) return;
        int current = getSouls(item);
        setSouls(item, current + amount);
    }

    /**
     * Consume souls from an Ethereal Katana
     */
    public static boolean consumeSouls(ItemStack item, int amount) {
        if (!isEtherealKatana(item) || !item.hasItemMeta()) return false;
        int current = getSouls(item);
        if (current < amount) return false;
        setSouls(item, current - amount);
        return true;
    }

    /**
     * Calculate enhanced damage based on shard count
     */
    public double calculateEnhancedDamage(double baseDamage, int shardCount) {
        return baseDamage + shardCount;
    }

    /**
     * Update the item model based on soul count
     */
    private static void updateModelForSouls(ItemStack item) {
        if (!item.hasItemMeta()) return;
        int souls = getSouls(item);
        String modelName = getModelNameForSouls(souls);
        item.setData(DataComponentTypes.ITEM_MODEL, NamespacedKey.fromString(modelName));
    }

    /**
     * Get the model name based on soul count
     */
    private static String getModelNameForSouls(int souls) {
        return switch (souls) {
            case 0, 1, 2, 3, 4 -> "lumen:etherealkatana_standard";
            case 5, 6, 7, 8, 9 -> "lumen:etherealkatana_1";
            case 10, 11, 12, 13, 14 -> "lumen:etherealkatana_2";
            case 15, 16, 17, 18, 19 -> "lumen:etherealkatana_3";
            case 20 -> "lumen:etherealkatana_final";
            default -> "lumen:etherealkatana_standard";
        };
    }

    /**
     * Update the soul count in the lore
     */
    private static void updateSoulLore(ItemMeta meta, int souls) {
        if (meta == null || meta.lore() == null || meta.lore().isEmpty()) return;
        
        List<Component> lore = new ArrayList<>(meta.lore());
        MiniMessage miniMessage = MiniMessage.miniMessage();
        
        int soulsLineIndex = -1;
        for (int i = 0; i < lore.size(); i++) {
            String text = lore.get(i).toString();
            if (text.contains("Souls:") || text.contains("souls")) {
                soulsLineIndex = i;
                break;
            }
        }
        
        if (soulsLineIndex == -1) {
            lore.add(2, miniMessage.deserialize("<dark_purple><bold>Souls: <light_purple>" + souls + "<reset><dark_purple>/20"));
        } else {
            lore.set(soulsLineIndex, miniMessage.deserialize("<dark_purple><bold>Souls: <light_purple>" + souls + "<reset><dark_purple>/20"));
        }
        
        meta.lore(lore);
    }
}
