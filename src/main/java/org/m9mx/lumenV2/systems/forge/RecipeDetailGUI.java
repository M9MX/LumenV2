package org.m9mx.lumenV2.systems.forge;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.m9mx.lumenV2.config.ConfigManager;
import org.m9mx.lumenV2.item.CustomItem;
import org.m9mx.lumenV2.item.Recipe;
import org.m9mx.lumenV2.item.RecipeType;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;

/**
 * Recipe Detail GUI - Shows recipe grid and output for a specific item
 * 6 rows (54 slots)
 * 
 * Slot layout:
 * x = recipe grid (3x3): 10-12, 19-21, 28-30
 * z = green/red panes around output: 14-16, 23, 25, 32-34
 * c = output slot: 24
 * v = give items button (creative only): 26
 * b = back button: 47
 */
public class RecipeDetailGUI {

    private static final int[] RECIPE_GRID_SLOTS = { 10, 11, 12, 19, 20, 21, 28, 29, 30 };
    private static final int[] OUTPUT_BORDER_SLOTS = { 14, 15, 16, 23, 25, 32, 33, 34 };
    private static final int OUTPUT_SLOT = 24;
    private static final int GIVE_ITEMS_BUTTON_SLOT = 26;
    private static final int BACK_BUTTON_SLOT = 47;

    /**
     * Create the recipe detail GUI for a specific item
     */
    public static Inventory createRecipeDetailGUI(CustomItem customItem, Player player) {
        Inventory gui = Bukkit.createInventory(null, 54,
                Component.text("Recipe: ").decorate(TextDecoration.BOLD)
                        .append(customItem.getDisplayName()));

        // Fill with gray glass panes
        ItemStack grayPane = createGrayPane();
        for (int i = 0; i < 54; i++) {
            gui.setItem(i, grayPane);
        }

        Recipe recipe = customItem.getRecipe();
        if (recipe != null) {
            // Display recipe grid
            displayRecipeGrid(gui, recipe);

            // Display output
            gui.setItem(OUTPUT_SLOT, customItem.build());

            // Set border color based on recipe type
            boolean isNormal = recipe.getType() == RecipeType.NORMAL;
            ItemStack borderPane = createBorderPane(isNormal);
            for (int slot : OUTPUT_BORDER_SLOTS) {
                gui.setItem(slot, borderPane);
            }
        }

        // Set back button
        gui.setItem(BACK_BUTTON_SLOT, createBackButton());

        // Show give items button only in creative mode
        if (player.getGameMode() == org.bukkit.GameMode.CREATIVE) {
            ItemStack button = createGiveItemsButton();
            gui.setItem(GIVE_ITEMS_BUTTON_SLOT, button);
            if (ConfigManager.getInstance().getMainConfig().isDebugEnabled()) {
                System.out.println("[RecipeDetailGUI] Placed give items button at slot: " + GIVE_ITEMS_BUTTON_SLOT
                        + " for player: " + player.getName());
            }
        }

        return gui;
    }

    /**
     * Display the 3x3 recipe grid
     */
    private static void displayRecipeGrid(Inventory gui, Recipe recipe) {
        Recipe.RecipeSlot[][] grid = recipe.getGrid();
        int gridIndex = 0;

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                Recipe.RecipeSlot slot = grid[row][col];
                int guiSlot = RECIPE_GRID_SLOTS[gridIndex];

                if (slot != null) {
                    ItemStack item = createRecipeSlotItem(slot);
                    gui.setItem(guiSlot, item);
                }
                // If slot is null, leave it empty (gray pane from background)

                gridIndex++;
            }
        }
    }

    /**
     * Create an item display for a recipe slot showing the item and amount required
     */
    private static ItemStack createRecipeSlotItem(Recipe.RecipeSlot slot) {
        String itemId = slot.getItemId();
        int amount = slot.getAmount();
        
        ItemStack item = null;
        
        // Handle LUMEN: prefixed custom items
        if (itemId.startsWith("LUMEN:")) {
            String customItemId = itemId.substring(6); // Remove "LUMEN:" prefix
            // Convert to lowercase to match registry keys
            customItemId = customItemId.toLowerCase();
            org.m9mx.lumenV2.item.ItemRegistry registry = org.m9mx.lumenV2.item.ItemRegistry.getInstance();
            CustomItem customItem = registry.getItem(customItemId);
            
            if (customItem != null) {
                item = customItem.build();
                item.setAmount(Math.min(amount, 64)); // Cap at 64 for display
            }
        } else {
            // Try direct custom item first
            org.m9mx.lumenV2.item.ItemRegistry registry = org.m9mx.lumenV2.item.ItemRegistry.getInstance();
            CustomItem customItem = registry.getItem(itemId.toLowerCase());
            
            if (customItem != null) {
                item = customItem.build();
                item.setAmount(Math.min(amount, 64)); // Cap at 64 for display
            } else {
                // Try vanilla material
                try {
                    Material material = Material.valueOf(itemId.toUpperCase());
                    item = new ItemStack(material);
                    item.setAmount(Math.min(amount, 64)); // Cap at 64 for display
                } catch (IllegalArgumentException e) {
                    return null;
                }
            }
        }

        // Add lore showing required amount
        if (item != null) {
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                List<Component> lore = new ArrayList<>();
                if (meta.hasLore()) {
                    lore.addAll(meta.lore());
                }
                lore.add(Component.text("Required: " + amount, TextColor.color(0xFFFF00)));
                meta.lore(lore);
                item.setItemMeta(meta);
            }
        }

        return item;
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
     * Create a colored border pane (green for NORMAL, red for RITUAL)
     */
    private static ItemStack createBorderPane(boolean isNormal) {
        Material material = isNormal ? Material.GREEN_STAINED_GLASS_PANE : Material.RED_STAINED_GLASS_PANE;
        ItemStack pane = new ItemStack(material);
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
            lore.add(Component.text("Return to recipe book", TextColor.color(0xAAAAAA)));
            meta.lore(lore);
            button.setItemMeta(meta);
        }
        return button;
    }

    /**
     * Create the give items button (creative mode only)
     */
    public static ItemStack createGiveItemsButton() {
        ItemStack button = new ItemStack(Material.GREEN_WOOL);
        ItemMeta meta = button.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text("Give Items", TextColor.color(0x00FF00), TextDecoration.BOLD));
            List<Component> lore = new ArrayList<>();
            lore.add(Component.text("Get all ingredients", TextColor.color(0xAAAAAA)));
            meta.lore(lore);
            button.setItemMeta(meta);
        }
        return button;
    }

    /**
     * Give all recipe ingredients to the player
     */
    public static void giveRecipeItems(Player player, Recipe recipe) {
        org.m9mx.lumenV2.item.ItemRegistry registry = org.m9mx.lumenV2.item.ItemRegistry.getInstance();
        boolean debug = ConfigManager.getInstance().getMainConfig().isDebugEnabled();

        if (debug) {
            System.out.println("[RecipeDetailGUI] Starting to give items for recipe. Occupied slots: "
                    + recipe.getOccupiedSlots().size());
        }

        for (Recipe.RecipeSlot slot : recipe.getOccupiedSlots()) {
            String itemId = slot.getItemId();
            int amount = slot.getAmount();

            if (debug) {
                System.out.println("[RecipeDetailGUI] Processing item: " + itemId + " x" + amount);
            }

            // Handle LUMEN: prefixed custom items
            if (itemId.startsWith("LUMEN:")) {
                String customItemId = itemId.substring(6); // Remove "LUMEN:" prefix
                // Convert to lowercase to match registry keys
                customItemId = customItemId.toLowerCase();
                CustomItem customItem = registry.getItem(customItemId);
                if (customItem != null) {
                    ItemStack item = customItem.build();
                    item.setAmount(amount);
                    player.getInventory().addItem(item);
                    if (debug) {
                        System.out.println("[RecipeDetailGUI] Added LUMEN: custom item: " + customItemId);
                    }
                } else {
                    if (debug) {
                        System.out.println("[RecipeDetailGUI] LUMEN: custom item not found: " + customItemId);
                    }
                }
            } else {
                // Try direct custom item first
                CustomItem customItem = registry.getItem(itemId.toLowerCase());
                if (customItem != null) {
                    ItemStack item = customItem.build();
                    item.setAmount(amount);
                    player.getInventory().addItem(item);
                    if (debug) {
                        System.out.println("[RecipeDetailGUI] Added custom item: " + itemId);
                    }
                } else {
                    // Try vanilla material
                    try {
                        org.bukkit.Material material = org.bukkit.Material.valueOf(itemId.toUpperCase());
                        ItemStack item = new ItemStack(material);
                        item.setAmount(amount);
                        player.getInventory().addItem(item);
                        if (debug) {
                            System.out.println("[RecipeDetailGUI] Added vanilla material: " + itemId);
                        }
                    } catch (IllegalArgumentException e) {
                        if (debug) {
                            System.out.println("[RecipeDetailGUI] Item not found: " + itemId);
                        }
                    }
                }
            }
        }
        if (debug) {
            System.out.println("[RecipeDetailGUI] Finished giving items");
        }
    }

    /**
     * Get recipe grid slots
     */
    public static int[] getRecipeGridSlots() {
        return RECIPE_GRID_SLOTS;
    }

    /**
     * Get output border slots
     */
    public static int[] getOutputBorderSlots() {
        return OUTPUT_BORDER_SLOTS;
    }

    /**
     * Get output slot
     */
    public static int getOutputSlot() {
        return OUTPUT_SLOT;
    }

    /**
     * Get give items button slot
     */
    public static int getGiveItemsButtonSlot() {
        return GIVE_ITEMS_BUTTON_SLOT;
    }

    /**
     * Get back button slot
     */
    public static int getBackButtonSlot() {
        return BACK_BUTTON_SLOT;
    }
}
