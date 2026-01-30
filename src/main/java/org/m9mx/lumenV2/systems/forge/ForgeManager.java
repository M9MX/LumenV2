package org.m9mx.lumenV2.systems.forge;

import org.bukkit.Material; 
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.m9mx.lumenV2.config.ConfigManager;
import org.m9mx.lumenV2.item.CustomItem;
import org.m9mx.lumenV2.item.ItemRegistry;
import org.m9mx.lumenV2.item.Recipe;
import org.m9mx.lumenV2.util.ItemUtils;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Manages forge operations - recipe validation, crafting, quick craft
 */
public class ForgeManager {
    
    // Cache for grid states to prevent log spam
    private static final Map<Integer, String> gridStateCache = new HashMap<>();
    
    private static String getGridStateHash(Inventory gui) {
        StringBuilder sb = new StringBuilder();
        int[] gridSlots = ForgeGUI.getCraftingGridSlots();
        for (int slot : gridSlots) {
            ItemStack item = gui.getItem(slot);
            if (item != null && !item.isEmpty()) {
                String id = ItemUtils.isCustomItem(item) ? "LUMEN:" + ItemUtils.getItemId(item) : item.getType().name();
                sb.append(id).append(":").append(item.getAmount()).append("|");
            } else {
                sb.append("empty|");
            }
        }
        return sb.toString();
    }

     /**
      * Validate if a recipe can be crafted from the current grid state
      * @param gui the forge GUI inventory
      * @return true if recipe is valid
      */
     public static boolean isRecipeValid(Inventory gui) {
         // Get the recipe from current grid state
         Recipe recipe = getRecipeFromGrid(gui);
         return recipe != null;
     }

    /**
     * Try to match a recipe from the current crafting grid
     * Validates exact grid position matching only
     * @return the matching recipe, or null if no recipe matches
     */
    public static Recipe getRecipeFromGrid(Inventory gui) {
        int[] gridSlots = ForgeGUI.getCraftingGridSlots();
        boolean debug = ConfigManager.getInstance().getMainConfig().isDebugEnabled();
        
        // Check grid state hash to avoid log spam
        int guiHash = System.identityHashCode(gui);
        String currentHash = getGridStateHash(gui);
        String previousHash = gridStateCache.get(guiHash);
        boolean gridChanged = !currentHash.equals(previousHash);
        
        if (gridChanged) {
            gridStateCache.put(guiHash, currentHash);
        }
        
        // Build current grid state (3x3)
        Recipe.RecipeSlot[][] currentGrid = new Recipe.RecipeSlot[3][3];
        for (int i = 0; i < gridSlots.length; i++) {
            ItemStack item = gui.getItem(gridSlots[i]);
            int row = i / 3;
            int col = i % 3;
            
            if (item != null && !item.isEmpty()) {
                // Check if custom item first
                if (ItemUtils.isCustomItem(item)) {
                    String itemId = ItemUtils.getItemId(item);
                    currentGrid[row][col] = new Recipe.RecipeSlot(itemId, item.getAmount());
                    if (debug && gridChanged) {
                        System.out.println("[ForgeManager] Grid [" + row + "][" + col + "]: Custom item " + itemId + " x" + item.getAmount());
                    }
                } else {
                    // Try vanilla material
                    String materialName = item.getType().name();
                    currentGrid[row][col] = new Recipe.RecipeSlot(materialName, item.getAmount());
                    if (debug && gridChanged) {
                        System.out.println("[ForgeManager] Grid [" + row + "][" + col + "]: Vanilla " + materialName + " x" + item.getAmount());
                    }
                }
            } else if (debug && gridChanged && (item == null || item.isEmpty())) {
                System.out.println("[ForgeManager] Grid [" + row + "][" + col + "]: Empty");
            }
        }
        
        // Search all items for matching recipe
        for (CustomItem customItem : ItemRegistry.getAllItems()) {
            if (!customItem.hasRecipe() || !customItem.isEnabled()) continue;
            
            // Check if recipe is enabled in config
            if (!isRecipeEnabled(customItem.getId())) continue;
            
            Recipe recipe = customItem.getRecipe();
            if (matchesRecipe(currentGrid, recipe.getGrid())) {
                if (debug && gridChanged) {
                    System.out.println("[ForgeManager] MATCH FOUND: " + customItem.getId());
                }
                return recipe;
            }
        }
        
        return null;
    }

    /**
     * Check if current grid matches a recipe
     * Allows slots to have MORE items than required (e.g., 20 gold blocks when 8 needed)
     */
    private static boolean matchesRecipe(Recipe.RecipeSlot[][] current, Recipe.RecipeSlot[][] recipe) {
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                Recipe.RecipeSlot currentSlot = current[row][col];
                Recipe.RecipeSlot recipeSlot = recipe[row][col];
                
                // Both empty
                if (currentSlot == null && recipeSlot == null) continue;
                
                // One empty, one not
                if (currentSlot == null || recipeSlot == null) return false;
                
                // Get both item IDs
                String currentItemId = currentSlot.getItemId();
                String recipeItemId = recipeSlot.getItemId();
                
                // Handle LUMEN: prefixed items - normalize both sides
                boolean itemsMatch = false;
                
                // If recipe specifies LUMEN: format, check if current item matches the custom item ID inside
                if (recipeItemId.startsWith("LUMEN:")) {
                    String actualRecipeId = recipeItemId.substring(6);
                    itemsMatch = actualRecipeId.equalsIgnoreCase(currentItemId);
                } 
                // If current item is from a LUMEN: format perspective (though normally it won't be in the grid)
                else if (currentItemId.startsWith("LUMEN:")) {
                    String actualCurrentId = currentItemId.substring(6);
                    itemsMatch = actualCurrentId.equalsIgnoreCase(recipeItemId);
                }
                // Otherwise just compare directly with case-insensitive matching
                else {
                    itemsMatch = currentItemId.equalsIgnoreCase(recipeItemId);
                }
                
                // Different items, or not enough amount (but can have more)
                if (!itemsMatch || currentSlot.getAmount() < recipeSlot.getAmount()) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Get all craftable items from player inventory
     * Shows recipes that can be crafted with available materials in the player's inventory
     * @param playerInventory the player's inventory to check
     * @return list of custom items that can be crafted
     */
    public static List<CustomItem> getQuickCraftOptions(Inventory playerInventory) {
        List<CustomItem> options = new ArrayList<>();
        
        for (CustomItem item : ItemRegistry.getAllItems()) {
            if (!item.hasRecipe() || !item.isEnabled()) continue;
            
            // Check if recipe is enabled in config
            if (!isRecipeEnabled(item.getId())) continue;
            
            Recipe recipe = item.getRecipe();
            if (canCraftRecipe(playerInventory, recipe)) {
                options.add(item);
            }
        }
        
        return options;
    }

    /**
     * Check if a recipe is enabled in the forge config
     */
    private static boolean isRecipeEnabled(String itemId) {
        try {
            return ConfigManager.getInstance().getSystemsConfig().getBoolean("forge.recipes." + itemId, true);
        } catch (Exception e) {
            return true; // Default to enabled if config not found
        }
    }

    /**
     * Check if a recipe can be crafted from player inventory
     * Checks if player has all ingredients needed for the recipe
     */
    private static boolean canCraftRecipe(Inventory playerInventory, Recipe recipe) {
        boolean debug = ConfigManager.getInstance().getMainConfig().isDebugEnabled();
        Map<String, Integer> required = recipe.getRequiredIngredients();
        Map<String, Integer> available = new HashMap<>();
        
        // Count available items in player inventory (both custom and vanilla)
        for (ItemStack item : playerInventory.getContents()) {
            if (item != null && !item.isEmpty()) {
                String itemId = null;
                
                // Check if custom item
                if (ItemUtils.isCustomItem(item)) {
                    itemId = ItemUtils.getItemId(item);
                } else {
                    // Try vanilla material
                    itemId = item.getType().name();
                }
                
                if (itemId != null) {
                    available.put(itemId.toLowerCase(), available.getOrDefault(itemId.toLowerCase(), 0) + item.getAmount());
                }
            }
        }
        
        // Check if all required items are available
        for (String requiredItemId : required.keySet()) {
            int needed = required.get(requiredItemId);
            int have = 0;
            
            // If required item is LUMEN: prefixed, check for the actual custom item ID
            if (requiredItemId.startsWith("LUMEN:")) {
                String actualItemId = requiredItemId.substring(6); // Remove "LUMEN:" prefix
                have = available.getOrDefault(actualItemId.toLowerCase(), 0);
            }
            // Otherwise, check directly (could be vanilla material or direct custom item ID)
            else {
                have = available.getOrDefault(requiredItemId.toLowerCase(), 0);
            }
            
            if (have < needed) {
                if (debug) {
                    System.out.println("[ForgeManager] Cannot craft recipe - needed " + needed + " of " + requiredItemId + ", but have " + have);
                }
                return false;
            }
        }
        
        return true;
    }

    /**
     * Place recipe ingredients into the grid from inventory (stackable quick craft)
     * Works like old code: places exact recipe amounts, removes from inventory, allows stacking
     * @param gui the forge GUI
     * @param playerInventory the player's inventory
     * @param recipe the recipe to place
     * @return true if successful
     */
    public static boolean placeRecipeInGrid(Inventory gui, Inventory playerInventory, Recipe recipe) {
        boolean debug = ConfigManager.getInstance().getMainConfig().isDebugEnabled();
        
        // Get required ingredients
        Recipe.RecipeSlot[][] grid = recipe.getGrid();
        int[] gridSlots = ForgeGUI.getCraftingGridSlots();
        
        if (debug) {
            System.out.println("[ForgeManager] Quick craft: Attempting to place recipe " + recipe.getId());
        }
        
        // First, verify player has all ingredients
        Map<String, Integer> required = recipe.getRequiredIngredients();
        if (!hasAllIngredients(playerInventory, required)) {
            if (debug) {
                System.out.println("[ForgeManager] Quick craft: Missing ingredients!");
            }
            return false;
        }
        
        // Check if grid already has items and if they match this recipe
        boolean isStacking = false;
        boolean gridHasItems = false;
        boolean gridMatchesRecipe = true;
        
        int gridIndex = 0;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                Recipe.RecipeSlot recipeSlot = grid[row][col];
                int slot = gridSlots[gridIndex];
                ItemStack gridItem = gui.getItem(slot);
                
                if (gridItem != null && !gridItem.isEmpty()) {
                    gridHasItems = true;
                    // Check if the grid item matches what this recipe needs
                    if (recipeSlot == null) {
                        gridMatchesRecipe = false;
                    } else {
                        // Get the actual item ID from the grid item
                        String gridItemId = null;
                        if (ItemUtils.isCustomItem(gridItem)) {
                            gridItemId = ItemUtils.getItemId(gridItem);
                        } else {
                            gridItemId = gridItem.getType().name();
                        }
                        
                        // Compare the grid item ID with the recipe slot ID
                        String recipeItemId = recipeSlot.getItemId();
                        
                        // Handle LUMEN: prefixed items - normalize both sides
                        if (recipeItemId.startsWith("LUMEN:")) {
                            String actualRecipeId = recipeItemId.substring(6);
                            gridMatchesRecipe = actualRecipeId.equalsIgnoreCase(gridItemId);
                        } else {
                            gridMatchesRecipe = recipeItemId.equalsIgnoreCase(gridItemId);
                        }
                    }
                } else {
                    // Grid slot is empty but recipe needs something
                    if (recipeSlot != null) {
                        gridMatchesRecipe = false;
                    }
                }
                gridIndex++;
            }
        }
        
        // If grid has items but they don't match this recipe, return them to inventory
        if (gridHasItems && !gridMatchesRecipe) {
            for (int slot : gridSlots) {
                ItemStack item = gui.getItem(slot);
                if (item != null && !item.isEmpty()) {
                    playerInventory.addItem(item.clone());
                    gui.setItem(slot, null);
                    if (debug) {
                        System.out.println("[ForgeManager] Returned grid item to inventory: " + item.getType() + " x" + item.getAmount());
                    }
                }
            }
            isStacking = false;
        } else if (gridHasItems && gridMatchesRecipe) {
            // Grid matches recipe, so we're stacking
            isStacking = true;
            if (debug) {
                System.out.println("[ForgeManager] Quick craft: Stacking mode = true");
            }
        }
        
        // First pass: add items to grid
        gridIndex = 0;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                Recipe.RecipeSlot recipeSlot = grid[row][col];
                if (recipeSlot != null) {
                    int slot = gridSlots[gridIndex];
                    int amountToAdd = recipeSlot.getAmount();
                    
                    if (debug) {
                        System.out.println("[ForgeManager] Grid slot " + slot + ": Placing " + recipeSlot.getItemId() + " x" + amountToAdd);
                    }
                    
                    if (isStacking) {
                        // Add to existing items
                        ItemStack existing = gui.getItem(slot);
                        if (existing != null && !existing.isEmpty()) {
                            existing.setAmount(existing.getAmount() + amountToAdd);
                        } else {
                            ItemStack item = createItemStack(recipeSlot.getItemId(), amountToAdd);
                            gui.setItem(slot, item);
                        }
                    } else {
                        // First time or different recipe - just place
                        ItemStack item = createItemStack(recipeSlot.getItemId(), amountToAdd);
                        gui.setItem(slot, item);
                    }
                }
                gridIndex++;
            }
        }
        
        // Second pass: remove from player inventory (only after grid is updated)
        gridIndex = 0;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                Recipe.RecipeSlot recipeSlot = grid[row][col];
                if (recipeSlot != null) {
                    removeFromInventory(playerInventory, recipeSlot.getItemId(), recipeSlot.getAmount());
                }
                gridIndex++;
            }
        }
        
        if (debug) {
            System.out.println("[ForgeManager] Quick craft: Recipe placed successfully");
        }
        return true;
    }

    /**
     * Check if player inventory has all required ingredients (supports both custom items and vanilla materials)
     */
    private static boolean hasAllIngredients(Inventory inventory, Map<String, Integer> required) {
        for (String itemId : required.keySet()) {
            int need = required.get(itemId);
            int have = 0;
            
            for (ItemStack item : inventory.getContents()) {
                if (item != null && !item.isEmpty()) {
                    String id = null;
                    
                    // Check if custom item
                    if (ItemUtils.isCustomItem(item)) {
                        id = ItemUtils.getItemId(item);
                        
                        // Check if this matches the required itemId directly
                        if (id != null && itemId.equalsIgnoreCase(id)) {
                            have += item.getAmount();
                        } 
                        // Check if this matches the LUMEN: prefixed format (e.g., required is "LUMEN:TEST" and actual is "test")
                        else if (itemId.startsWith("LUMEN:") && itemId.substring(6).equalsIgnoreCase(id)) {
                            have += item.getAmount();
                        }
                    } else {
                        // Try vanilla material
                        id = item.getType().name();
                        
                        if (id != null && itemId.equalsIgnoreCase(id)) {
                            have += item.getAmount();
                        }
                    }
                }
            }
            
            if (have < need) return false;
        }
        return true;
    }

    /**
     * Remove items from player inventory (supports both custom items and vanilla materials)
     */
    private static void removeFromInventory(Inventory inventory, String itemId, int amount) {
        int toRemove = amount;
        for (ItemStack item : inventory.getContents()) {
            if (toRemove <= 0) break;
            if (item != null && !item.isEmpty()) {
                String id = null;
                
                // Check if custom item
                if (ItemUtils.isCustomItem(item)) {
                    id = ItemUtils.getItemId(item);
                    
                    // Check if this matches the required itemId directly
                    if (id != null && itemId.equalsIgnoreCase(id)) {
                        int remove = Math.min(toRemove, item.getAmount());
                        item.setAmount(item.getAmount() - remove);
                        toRemove -= remove;
                    } 
                    // Check if this matches the LUMEN: prefixed format (e.g., required is "LUMEN:TEST" and actual is "test")
                    else if (itemId.startsWith("LUMEN:") && itemId.substring(6).equalsIgnoreCase(id)) {
                        int remove = Math.min(toRemove, item.getAmount());
                        item.setAmount(item.getAmount() - remove);
                        toRemove -= remove;
                    }
                } else {
                    // Try vanilla material
                    id = item.getType().name();
                    
                    if (id != null && itemId.equalsIgnoreCase(id)) {
                        int remove = Math.min(toRemove, item.getAmount());
                        item.setAmount(item.getAmount() - remove);
                        toRemove -= remove;
                    }
                }
            }
        }
    }

    /**
     * Create an ItemStack from item ID (supports both custom items and vanilla materials)
     */
    private static ItemStack createItemStack(String itemId, int amount) {
        // First try to get from custom item registry
        // Check if it's a LUMEN: prefixed custom item (e.g. LUMEN:TEST)
        if (itemId.startsWith("LUMEN:")) {
            String customItemId = itemId.substring(6); // Remove "LUMEN:" prefix
            // Convert to lowercase to match registry keys
            customItemId = customItemId.toLowerCase();
            CustomItem customItem = ItemRegistry.getInstance().getItem(customItemId);
            if (customItem != null) {
                ItemStack item = customItem.build();
                item.setAmount(amount);
                return item;
            }
        } else {
            // Check for direct custom item registration
            CustomItem customItem = ItemRegistry.getInstance().getItem(itemId.toLowerCase());
            if (customItem != null) {
                ItemStack item = customItem.build();
                item.setAmount(amount);
                return item;
            }
        }
        
        // Fall back to vanilla Minecraft material
        try {
            Material material = Material.valueOf(itemId.toUpperCase());
            ItemStack item = new ItemStack(material);
            item.setAmount(amount);
            return item;
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * Get items in crafting grid
     */
    public static List<ItemStack> getGridContents(Inventory gui) {
        List<ItemStack> contents = new ArrayList<>();
        for (int slot : ForgeGUI.getCraftingGridSlots()) {
            ItemStack item = gui.getItem(slot);
            if (item != null && !item.isEmpty()) {
                contents.add(item.clone());
            }
        }
        return contents;
    }

    /**
     * Clear the crafting grid
     */
    public static void clearGrid(Inventory gui) {
        for (int slot : ForgeGUI.getCraftingGridSlots()) {
            gui.setItem(slot, null);
        }
        ForgeGUI.updateOutputBorder(gui, false);
    }
}
