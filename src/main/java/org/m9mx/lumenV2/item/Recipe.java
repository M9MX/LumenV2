package org.m9mx.lumenV2.item;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents a 3x3 forge recipe for a custom item
 */
public class Recipe {
    private String id;
    private RecipeType type;
    private RecipeSlot[][] grid; // 3x3 grid
    private String result; // item id
    
    /**
     * Represents a slot in the 3x3 recipe grid
     */
    public static class RecipeSlot {
        private String itemId;
        private int amount;
        
        public RecipeSlot(String itemId, int amount) {
            this.itemId = itemId;
            this.amount = amount;
        }
        
        public String getItemId() { return itemId; }
        public int getAmount() { return amount; }
    }
    
    public Recipe(String id, RecipeType type) {
        this.id = id;
        this.type = type;
        this.grid = new RecipeSlot[3][3];
    }
    
    /**
     * Set a recipe slot in the 3x3 grid
     * @param row 0-2
     * @param col 0-2
     * @param itemId the item required
     * @param amount the amount of items required
     */
    public void setSlot(int row, int col, String itemId, int amount) {
        if (row < 0 || row >= 3 || col < 0 || col >= 3) {
            throw new IllegalArgumentException("Grid position must be 0-2");
        }
        grid[row][col] = new RecipeSlot(itemId, amount);
    }
    
    /**
     * Set result item
     */
    public void setResult(String itemId) {
        this.result = itemId;
    }
    
    public String getId() { return id; }
    public RecipeType getType() { return type; }
    public RecipeSlot[][] getGrid() { return grid; }
    public String getResult() { return result; }
    
    /**
     * Get slot at position
     */
    public RecipeSlot getSlot(int row, int col) {
        return grid[row][col];
    }
    
    /**
     * Get all required ingredients as a map (itemId -> total amount)
     * Useful for forge systems to check inventory
     */
    public Map<String, Integer> getRequiredIngredients() {
        Map<String, Integer> ingredients = new HashMap<>();
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                RecipeSlot slot = grid[row][col];
                if (slot != null) {
                    ingredients.put(slot.getItemId(), 
                        ingredients.getOrDefault(slot.getItemId(), 0) + slot.getAmount());
                }
            }
        }
        return ingredients;
    }
    
    /**
     * Get all occupied slots as a list
     * Useful for iterating through recipe ingredients
     */
    public List<RecipeSlot> getOccupiedSlots() {
        List<RecipeSlot> slots = new ArrayList<>();
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                if (grid[row][col] != null) {
                    slots.add(grid[row][col]);
                }
            }
        }
        return slots;
    }
}
