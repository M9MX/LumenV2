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
 * Forge GUI - Custom crafting interface (6 rows)
 * Slot layout:
 * x = empty (gray glass panes)
 * z = crafting grid slots: 10,11,12,19,20,21,28,29,30
 * c = border around output: 14,15,16,23,25,32,33,34 (green/red)
 * v = quick craft slots: 17,26,35
 * b = output slot: 24
 * n = recipe GUI button: 51
 */
public class ForgeGUI {

    // Slot assignments for 6-row forge crafting
    private static final int[] CRAFTING_GRID = {10, 11, 12, 19, 20, 21, 28, 29, 30};
    private static final int[] OUTPUT_BORDER = {14, 15, 16, 23, 25, 32, 33, 34};
    private static final int[] QUICK_CRAFT_SLOTS = {17, 26, 35};
    private static final int OUTPUT_SLOT = 24;
    private static final int RECIPE_GUI_SLOT = 51;

    /**
     * Create the forge crafting GUI (6 rows)
     */
    public static Inventory createForgeGUI() {
        Inventory gui = Bukkit.createInventory(null, 54, Component.text("Forge").decorate(TextDecoration.BOLD));

        // Fill with gray glass panes
        ItemStack grayPane = createGrayPane();
        for (int i = 0; i < 54; i++) {
            gui.setItem(i, grayPane);
        }

        // Set quick craft slots as red glass panes with label
        ItemStack quickCraftPane = createQuickCraftPane();
        for (int slot : QUICK_CRAFT_SLOTS) {
            gui.setItem(slot, quickCraftPane);
        }

        // Clear crafting grid and interactive slots
        for (int slot : CRAFTING_GRID) {
            gui.setItem(slot, null);
        }
        gui.setItem(OUTPUT_SLOT, null);
        
        // Set recipe GUI button with knowledge book
        gui.setItem(RECIPE_GUI_SLOT, createRecipeButton());
        
        // Set output border to green initially
        updateOutputBorder(gui, true);

        return gui;
    }
    
    /**
     * Get the crafting grid slot indices
     */
    public static int[] getCraftingGridSlots() {
        return CRAFTING_GRID;
    }

    /**
     * Get the quick craft slot indices
     */
    public static int[] getQuickCraftSlots() {
        return QUICK_CRAFT_SLOTS;
    }

    /**
     * Get the output slot index
     */
    public static int getOutputSlot() {
        return OUTPUT_SLOT;
    }

    /**
     * Get the recipe GUI slot index
     */
    public static int getRecipeGUISlot() {
        return RECIPE_GUI_SLOT;
    }

    /**
     * Check if a slot is part of the crafting grid
     */
    public static boolean isCraftingGridSlot(int slot) {
        for (int gridSlot : CRAFTING_GRID) {
            if (gridSlot == slot) return true;
        }
        return false;
    }

    /**
     * Check if a slot is a quick craft slot
     */
    public static boolean isQuickCraftSlot(int slot) {
        for (int quickSlot : QUICK_CRAFT_SLOTS) {
            if (quickSlot == slot) return true;
        }
        return false;
    }

    /**
     * Update the output border color (green = valid recipe, red = invalid)
     */
    public static void updateOutputBorder(Inventory gui, boolean isValid) {
        ItemStack borderPane = createBorderPane(isValid);
        for (int slot : OUTPUT_BORDER) {
            gui.setItem(slot, borderPane);
        }
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
     * Create a colored border pane (green or red)
     */
    private static ItemStack createBorderPane(boolean isValid) {
        Material material = isValid ? Material.GREEN_STAINED_GLASS_PANE : Material.RED_STAINED_GLASS_PANE;
        ItemStack pane = new ItemStack(material);
        ItemMeta meta = pane.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text(" "));
            pane.setItemMeta(meta);
        }
        return pane;
    }

    /**
     * Create the quick craft pane (red glass with label)
     */
    private static ItemStack createQuickCraftPane() {
        ItemStack pane = new ItemStack(Material.RED_STAINED_GLASS_PANE);
        ItemMeta meta = pane.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text("Quick Craft", TextColor.color(0xFF6B6B), TextDecoration.BOLD));
            List<Component> lore = new ArrayList<>();
            lore.add(Component.text("No available items found", TextColor.color(0xAAAAAA)));
            meta.lore(lore);
            pane.setItemMeta(meta);
        }
        return pane;
    }

    /**
     * Create the recipe GUI button with knowledge book
     */
    private static ItemStack createRecipeButton() {
        ItemStack button = new ItemStack(Material.KNOWLEDGE_BOOK);
        ItemMeta meta = button.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text("Recipes", TextColor.color(0x4ECDC4), TextDecoration.BOLD));
            List<Component> lore = new ArrayList<>();
            lore.add(Component.text("Click to view recipes", TextColor.color(0xAAAAAA)));
            meta.lore(lore);
            button.setItemMeta(meta);
        }
        return button;
    }
}
