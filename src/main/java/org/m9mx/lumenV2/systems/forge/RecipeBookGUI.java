package org.m9mx.lumenV2.systems.forge;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.m9mx.lumenV2.config.ConfigManager;
import org.m9mx.lumenV2.item.CustomItem;
import org.m9mx.lumenV2.item.ItemRegistry;

import java.util.ArrayList;
import java.util.List;

/**
 * Recipe Book GUI - Main menu showing all available recipes
 * 6 rows (54 slots)
 * 
 * Slot layout:
 * x = available recipe item slots (10,11,12,13,14,15,16, 19,20,21,22,23,24,25, 28,29,30,31,32,33,34) - 21 items max per page
 * z = back button: 47
 * c = next page button: 50 (only if more than 21 recipes)
 * v = back page button: 52 (only if more than 21 recipes)
 */
public class RecipeBookGUI {
    
    private static final int[] RECIPE_SLOTS = {
        10, 11, 12, 13, 14, 15, 16,
        19, 20, 21, 22, 23, 24, 25,
        28, 29, 30, 31, 32, 33, 34
    };
    private static final int BACK_BUTTON_SLOT = 47;
    private static final int NEXT_PAGE_BUTTON_SLOT = 50;
    private static final int PREV_PAGE_BUTTON_SLOT = 52;
    private static final int MAX_RECIPES_PER_PAGE = 21;
    
    /**
     * Create the recipe book main menu GUI
     * @param page the page number (0-indexed)
     * @return the inventory
     */
    public static Inventory createRecipeBookGUI(int page) {
        Inventory gui = Bukkit.createInventory(null, 54, Component.text("Recipe Book").decorate(TextDecoration.BOLD));
        
        // Fill with gray glass panes
        ItemStack grayPane = createGrayPane();
        for (int i = 0; i < 54; i++) {
            gui.setItem(i, grayPane);
        }
        
        // Get all enabled recipes
        List<CustomItem> enabledRecipes = getEnabledRecipes();
        
        // Calculate pages
        int totalPages = Math.max(1, (enabledRecipes.size() + MAX_RECIPES_PER_PAGE - 1) / MAX_RECIPES_PER_PAGE);
        int adjustedPage = Math.min(page, totalPages - 1);
        
        // Display recipes for current page
        int startIndex = adjustedPage * MAX_RECIPES_PER_PAGE;
        int endIndex = Math.min(startIndex + MAX_RECIPES_PER_PAGE, enabledRecipes.size());
        
        for (int i = startIndex; i < endIndex; i++) {
            int slotIndex = i - startIndex;
            CustomItem item = enabledRecipes.get(i);
            ItemStack displayItem = item.build();
            gui.setItem(RECIPE_SLOTS[slotIndex], displayItem);
        }
        
        // Set back button
        gui.setItem(BACK_BUTTON_SLOT, createBackButton());
        
        // Set pagination buttons only if there are more than 21 recipes
        if (enabledRecipes.size() > MAX_RECIPES_PER_PAGE) {
            if (adjustedPage > 0) {
                gui.setItem(PREV_PAGE_BUTTON_SLOT, createPrevPageButton());
            } else {
                gui.setItem(PREV_PAGE_BUTTON_SLOT, createGrayPane());
            }
            
            if (adjustedPage < totalPages - 1) {
                gui.setItem(NEXT_PAGE_BUTTON_SLOT, createNextPageButton());
            } else {
                gui.setItem(NEXT_PAGE_BUTTON_SLOT, createGrayPane());
            }
        }
        
        return gui;
    }
    
    /**
     * Get all enabled recipes from items that have recipes enabled in config
     */
    private static List<CustomItem> getEnabledRecipes() {
        List<CustomItem> recipes = new ArrayList<>();
        
        for (CustomItem item : ItemRegistry.getAllItems()) {
            // Check if item is enabled
            if (!item.isEnabled()) continue;
            
            // Check if item has a recipe
            if (!item.hasRecipe()) continue;
            
            // Check if recipe is enabled in config
            if (!isRecipeEnabled(item.getId())) continue;
            
            recipes.add(item);
        }
        
        return recipes;
    }
    
    /**
     * Check if a recipe is enabled in systems.yml
     */
    private static boolean isRecipeEnabled(String itemId) {
        try {
            return ConfigManager.getInstance().getSystemsConfig().getBoolean("forge.recipes." + itemId, true);
        } catch (Exception e) {
            return true; // Default to enabled if config not found
        }
    }
    
    /**
     * Create a gray glass pane
     */
    private static ItemStack createGrayPane() {
        ItemStack pane = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta meta = pane.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text(" "));
            pane.setItemMeta(meta);
        }
        return pane;
    }
    
    /**
     * Create the back button (Barrier block)
     */
    private static ItemStack createBackButton() {
        ItemStack button = new ItemStack(Material.BARRIER);
        ItemMeta meta = button.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text("Back", TextColor.color(0xFF0000), TextDecoration.BOLD));
            List<Component> lore = new ArrayList<>();
            lore.add(Component.text("Return to forge GUI", TextColor.color(0xAAAAAA)));
            meta.lore(lore);
            button.setItemMeta(meta);
        }
        return button;
    }
    
    /**
     * Create the next page button
     */
    private static ItemStack createNextPageButton() {
        ItemStack button = new ItemStack(Material.ARROW);
        ItemMeta meta = button.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text("Next Page", TextColor.color(0x4ECDC4), TextDecoration.BOLD));
            List<Component> lore = new ArrayList<>();
            lore.add(Component.text("Go to next page", TextColor.color(0xAAAAAA)));
            meta.lore(lore);
            button.setItemMeta(meta);
        }
        return button;
    }
    
    /**
     * Create the previous page button
     */
    private static ItemStack createPrevPageButton() {
        ItemStack button = new ItemStack(Material.ARROW);
        ItemMeta meta = button.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text("Previous Page", TextColor.color(0x4ECDC4), TextDecoration.BOLD));
            List<Component> lore = new ArrayList<>();
            lore.add(Component.text("Go to previous page", TextColor.color(0xAAAAAA)));
            meta.lore(lore);
            button.setItemMeta(meta);
        }
        return button;
    }
    
    /**
     * Get the recipe item slots
     */
    public static int[] getRecipeSlots() {
        return RECIPE_SLOTS;
    }
    
    /**
     * Get the back button slot
     */
    public static int getBackButtonSlot() {
        return BACK_BUTTON_SLOT;
    }
    
    /**
     * Get the next page button slot
     */
    public static int getNextPageButtonSlot() {
        return NEXT_PAGE_BUTTON_SLOT;
    }
    
    /**
     * Get the previous page button slot
     */
    public static int getPrevPageButtonSlot() {
        return PREV_PAGE_BUTTON_SLOT;
    }
    
    /**
     * Get max recipes per page
     */
    public static int getMaxRecipesPerPage() {
        return MAX_RECIPES_PER_PAGE;
    }
    
    /**
     * Get total number of enabled recipes
     */
    public static int getTotalEnabledRecipes() {
        return getEnabledRecipes().size();
    }
    
    /**
     * Get the enabled recipe at the given index
     */
    public static CustomItem getEnabledRecipeAt(int index) {
        List<CustomItem> recipes = getEnabledRecipes();
        if (index >= 0 && index < recipes.size()) {
            return recipes.get(index);
        }
        return null;
    }
}
