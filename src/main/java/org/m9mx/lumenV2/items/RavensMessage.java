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

public class RavensMessage extends CustomItem {

    public static final NamespacedKey MAIL_CONTENTS_KEY = new NamespacedKey("lumen", "mail_contents");

    public RavensMessage() {
        super("ravens_message");
    }

    @Override
    protected void initialize() {
        setMaterial(Material.PAPER);
        setDisplayName(MiniMessage.miniMessage().deserialize("<white><bold>Raven's Message"));
        
        setLore(java.util.Arrays.asList(
            MiniMessage.miniMessage().deserialize("<gray>A mystical letter carried by ravens"),
            MiniMessage.miniMessage().deserialize("<gray>Right-click to compose a message"),
            MiniMessage.miniMessage().deserialize("<gray>The recipient receives it when they join")
        ));
        
        // Use raven model
        setItemModel("lumen:ravens_message_open");
        setUnbreakable(false);
        
        // Add model for open envelope variant
        // This is referenced by the ravens_message_open and ravens_message_closed models

        // Create NORMAL mode recipe - red envelope shape
        Recipe recipe = new Recipe("ravens_message_recipe", RecipeType.NORMAL);
        
        // Grid layout - Red Envelope shape:
        // X R X  (envelope flap with wax seal)
        // P X P  (middle with paper sides)
        // P P P  (envelope body all paper)
        // X = Red Wool (flap) x8
        // R = Redstone Block (wax seal) x8
        // P = Paper x8
        
        // Row 0: Red Wool (x8), Redstone Block (x8), Red Wool (x8) - flap with seal
        recipe.setSlot(0, 0, "RED_WOOL", 8);
        recipe.setSlot(0, 1, "REDSTONE_BLOCK", 8);
        recipe.setSlot(0, 2, "RED_WOOL", 8);
        
        // Row 1: Paper (x8), Red Wool (x8), Paper (x8) - middle
        recipe.setSlot(1, 0, "PAPER", 8);
        recipe.setSlot(1, 1, "RED_WOOL", 8);
        recipe.setSlot(1, 2, "PAPER", 8);
        
        // Row 2: Paper (x8), Paper (x8), Paper (x8) - body
        recipe.setSlot(2, 0, "PAPER", 8);
        recipe.setSlot(2, 1, "PAPER", 8);
        recipe.setSlot(2, 2, "PAPER", 8);
        
        recipe.setResult("ravens_message");
        setRecipe(recipe);
    }
    
    @Override
    public ItemStack build() {
        ItemStack item = super.build();
        
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            PersistentDataContainer pdc = meta.getPersistentDataContainer();
            pdc.set(MAIL_CONTENTS_KEY, PersistentDataType.STRING, "");
            item.setItemMeta(meta);
        }
        
        return item;
    }
}
