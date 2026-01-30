package org.m9mx.lumenV2.systems.enhancement;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import io.papermc.paper.datacomponent.DataComponentTypes;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import java.util.ArrayList;
import java.util.List;

/**
 * Enhancement GUI inventory
 */
public class EnhancementGUI {

    private static final int CATALYST_SLOT = 13; // Center slot
    private static final int DISPLAY_SLOT = 4; // Slot above center

    /**
     * Create the enhancement GUI for an item
     */
    public static Inventory createGUI(ItemStack enhancableItem) {
        Inventory gui = Bukkit.createInventory(null, 27, Component.text("Enhancement GUI"));

        // Fill all slots with gray glass panes (non-interactable decoration)
        ItemStack grayPane = createGrayPane();
        for (int i = 0; i < 27; i++) {
            gui.setItem(i, grayPane);
        }

        // Get the saved shard count from the item
        int shardCount = EnhancementManager.getShardCount(enhancableItem);

        // Restore shards to the catalyst slot (13 - center) if any were saved
        if (shardCount > 0) {
            ItemStack restoredShards = getCatalystShard();
            restoredShards.setAmount(shardCount);
            gui.setItem(CATALYST_SLOT, restoredShards);
        } else {
            gui.setItem(CATALYST_SLOT, null);
        }

        // Create and set the display item in slot 4 (above center)
        ItemStack displayItem = createDisplayItem(shardCount);
        gui.setItem(DISPLAY_SLOT, displayItem);

        return gui;
    }

    /**
     * Create a gray glass pane for decoration
     */
    private static ItemStack createGrayPane() {
        ItemStack pane = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = pane.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text(" ", TextColor.color(0x808080)));
            pane.setItemMeta(meta);
        }
        return pane;
    }

    /**
     * Create the display item showing current shard count
     */
    private static ItemStack createDisplayItem(int shardCount) {
        ItemStack display = new ItemStack(Material.AMETHYST_SHARD);
        ItemMeta meta = display.getItemMeta();

        if (meta != null) {
            meta.displayName(Component.text("Catalyst Shards", TextColor.color(0xB24BF3)));

            // Create lore
            List<Component> lore = new ArrayList<>();
            int maxShards = org.m9mx.lumenV2.systems.EnhancementSystem.getInstance().getMaxShards();
            lore.add(Component.text("Current Shards: " + shardCount + "/" + maxShards,
                    TextColor.color(0x9D4EDD)));
            lore.add(Component.empty());
            lore.add(Component.text("Place shards in the center slot", TextColor.color(0x7B68EE)));
            lore.add(Component.text("to enhance this item.", TextColor.color(0x7B68EE)));
            lore.add(Component.empty());
            lore.add(Component.text("Each shard provides a modifier", TextColor.color(0x5E60CE)));
            lore.add(Component.text("to the item's abilities.", TextColor.color(0x5E60CE)));

            meta.lore(lore);
            meta.setUnbreakable(true);
            display.setItemMeta(meta);
        }

        // Apply same custom model as catalyst shard
        display.setData(DataComponentTypes.ITEM_MODEL, NamespacedKey.fromString("lumen:catalyst_shard"));

        return display;
    }

    /**
     * Get the catalyst shard item for the input slot
     */
    public static ItemStack getCatalystShard() {
        // Get the catalyst shard from ItemRegistry to ensure it has all the proper components
        org.m9mx.lumenV2.item.CustomItem catalystShardItem = org.m9mx.lumenV2.item.ItemRegistry.getInstance().getItem("catalyst_shard");
        if (catalystShardItem != null) {
            return catalystShardItem.build();
        }
        
        // Fallback if not found in registry
        ItemStack shard = new ItemStack(Material.AMETHYST_SHARD);
        ItemMeta meta = shard.getItemMeta();

        if (meta != null) {
            meta.displayName(Component.text("Catalyst Shard", TextColor.color(0xB24BF3)));

            List<Component> lore = new ArrayList<>();
            lore.add(Component.text("Can be used to upgrade items", TextColor.color(0x9D4EDD)));

            meta.lore(lore);
            shard.setItemMeta(meta);
        }

        // Set custom model
        shard.setData(DataComponentTypes.ITEM_MODEL, NamespacedKey.fromString("lumen:catalyst_shard"));

        return shard;
    }

    /**
     * Get the catalyst slot index
     */
    public static int getCatalystSlot() {
        return CATALYST_SLOT;
    }

    /**
     * Get the display slot index
     */
    public static int getDisplaySlot() {
        return DISPLAY_SLOT;
    }
}
