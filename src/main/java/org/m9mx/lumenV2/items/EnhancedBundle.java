package org.m9mx.lumenV2.items;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.m9mx.lumenV2.item.CustomItem;
import org.m9mx.lumenV2.item.Recipe;
import org.m9mx.lumenV2.item.RecipeType;

public class EnhancedBundle extends CustomItem {

    public static final NamespacedKey BUNDLE_CONTENTS_KEY = new NamespacedKey("lumen", "bundle_contents");

    public EnhancedBundle() {
        super("enhanced_bundle");
    }

    @Override
    protected void initialize() {
        setMaterial(Material.LEATHER_HORSE_ARMOR);
        setDisplayName(MiniMessage.miniMessage().deserialize("<white><bold>Enhanced Bundle"));
        
        setLore(java.util.Arrays.asList(
            MiniMessage.miniMessage().deserialize("<gray>A sturdy bundle with enhanced storage capabilities"),
            MiniMessage.miniMessage().deserialize("<gray>Right-click to access its contents"),
            MiniMessage.miniMessage().deserialize("<gray>Protects contents from loss")
        ));
        
        // Use the vanilla bundle model
        setItemModel("minecraft:bundle");
        setUnbreakable(false);

        // Vanilla recipe: 1 leather and 1 string (shapeless)
        // Recipe is registered in RecipeManager or similar registry
        setRecipe(null);
    }
    
    @Override
    public ItemStack build() {
        ItemStack item = super.build(); // Call parent build method first
        
        // Then add our custom bundle content key
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            PersistentDataContainer pdc = meta.getPersistentDataContainer();
            pdc.set(BUNDLE_CONTENTS_KEY, PersistentDataType.STRING, "");
            item.setItemMeta(meta);
        }
        
        return item;
    }
}