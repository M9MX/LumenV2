package org.m9mx.lumenV2.systems.forge;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import java.util.ArrayList;
import java.util.List;

/**
 * Smithing Table GUI - Main menu with Forge and Smithing Table buttons (3 rows)
 * Slot 11: Forge button (anvil icon)
 * Slot 15: Smithing Table button (smithing table icon)
 */
public class SmithingTableGUI {

    // Slot assignments for 3-row menu
    private static final int FORGE_BUTTON = 11;
    private static final int SMITHING_BUTTON = 15;

    /**
     * Create the main menu GUI with FORGE and SMITHING buttons (3 rows)
     */
    public static Inventory createMenuGUI() {
        Inventory gui = Bukkit.createInventory(null, 27, Component.text("Smithing Table").decorate(TextDecoration.BOLD));

        // Fill with gray glass panes
        ItemStack grayPane = createGrayPane();
        for (int i = 0; i < 27; i++) {
            gui.setItem(i, grayPane);
        }

        // Set button positions
        gui.setItem(FORGE_BUTTON, createForgeButton());
        gui.setItem(SMITHING_BUTTON, createSmithingButton());

        return gui;
    }

    /**
     * Get the forge button slot index
     */
    public static int getForgeButtonSlot() {
        return FORGE_BUTTON;
    }

    /**
     * Get the smithing button slot index
     */
    public static int getSmithingButtonSlot() {
        return SMITHING_BUTTON;
    }

    /**
     * Create a gray glass pane for decoration
     */
    private static ItemStack createGrayPane() {
        ItemStack pane = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = pane.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text(" "));
            pane.setItemMeta(meta);
        }
        return pane;
    }

    /**
     * Create the forge button with anvil icon
     */
    private static ItemStack createForgeButton() {
        ItemStack button = new ItemStack(Material.ANVIL);
        ItemMeta meta = button.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text("Forge", TextColor.color(0xFF6B6B), TextDecoration.BOLD));
            List<Component> lore = new ArrayList<>();
            lore.add(Component.text("Click to open forge", TextColor.color(0xAAAAAA)));
            meta.lore(lore);
            button.setItemMeta(meta);
        }
        return button;
    }

    /**
     * Create the smithing table button
     */
    private static ItemStack createSmithingButton() {
        ItemStack button = new ItemStack(Material.SMITHING_TABLE);
        ItemMeta meta = button.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text("Smithing Table", TextColor.color(0x4ECDC4), TextDecoration.BOLD));
            List<Component> lore = new ArrayList<>();
            lore.add(Component.text("Click to use smithing table", TextColor.color(0xAAAAAA)));
            meta.lore(lore);
            button.setItemMeta(meta);
        }
        return button;
    }
}
