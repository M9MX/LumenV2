package org.m9mx.lumenV2.systems.forge;

import org.bukkit.GameMode;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.m9mx.lumenV2.item.CustomItem;
import org.m9mx.lumenV2.config.ConfigManager;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Handles clicks in the recipe book GUIs
 */
public class RecipeBookListener implements Listener {
    
    private final Map<UUID, Integer> playerRecipePages = new HashMap<>();
    private final Map<UUID, Inventory> playerRecipeGUIs = new HashMap<>();
    private final Map<UUID, CustomItem> playerCurrentRecipe = new HashMap<>();
    private ForgeListener forgeListener;
    
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        Inventory gui = event.getInventory();
        Player player = (Player) event.getWhoClicked();
        int slot = event.getRawSlot();
        
        // Check if it's the recipe book GUI
        if (isRecipeBookGUI(gui)) {
            handleRecipeBookClick(event, gui, player, slot);
            return;
        }
        
        // Check if it's the recipe detail GUI
        if (isRecipeDetailGUI(gui)) {
            handleRecipeDetailClick(event, gui, player, slot);
            return;
        }
    }
    
    /**
     * Handle clicks in the recipe book (main menu)
     */
    private void handleRecipeBookClick(InventoryClickEvent event, Inventory gui, Player player, int slot) {
        // Allow player inventory interactions
        if (slot >= 54) {
            return;
        }
        
        event.setCancelled(true);
        
        // Back button - return to forge GUI
        if (slot == RecipeBookGUI.getBackButtonSlot()) {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 0.8f);
            openForgeGUI(player);
            return;
        }
        
        // Next page button
        if (slot == RecipeBookGUI.getNextPageButtonSlot()) {
            int currentPage = playerRecipePages.getOrDefault(player.getUniqueId(), 0);
            int totalRecipes = RecipeBookGUI.getTotalEnabledRecipes();
            int maxPages = Math.max(1, (totalRecipes + RecipeBookGUI.getMaxRecipesPerPage() - 1) / RecipeBookGUI.getMaxRecipesPerPage());
            
            if (currentPage < maxPages - 1) {
                currentPage++;
                playerRecipePages.put(player.getUniqueId(), currentPage);
                Inventory newGui = RecipeBookGUI.createRecipeBookGUI(currentPage);
                playerRecipeGUIs.put(player.getUniqueId(), newGui);
                player.openInventory(newGui);
                player.playSound(player.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 1.0f, 1.2f);
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
            } else {
                player.playSound(player.getLocation(), Sound.BLOCK_LAVA_EXTINGUISH, 0.3f, 1.5f);
            }
            return;
        }
        
        // Previous page button
        if (slot == RecipeBookGUI.getPrevPageButtonSlot()) {
            int currentPage = playerRecipePages.getOrDefault(player.getUniqueId(), 0);
            if (currentPage > 0) {
                currentPage--;
                playerRecipePages.put(player.getUniqueId(), currentPage);
                Inventory newGui = RecipeBookGUI.createRecipeBookGUI(currentPage);
                playerRecipeGUIs.put(player.getUniqueId(), newGui);
                player.openInventory(newGui);
                player.playSound(player.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 1.0f, 0.8f);
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 0.8f);
            } else {
                player.playSound(player.getLocation(), Sound.BLOCK_LAVA_EXTINGUISH, 0.3f, 1.5f);
            }
            return;
        }
        
        // Recipe item slots
        int[] recipeSlots = RecipeBookGUI.getRecipeSlots();
        for (int i = 0; i < recipeSlots.length; i++) {
            if (recipeSlots[i] == slot) {
                // Calculate actual recipe index based on current page
                int currentPage = playerRecipePages.getOrDefault(player.getUniqueId(), 0);
                int recipeIndex = currentPage * RecipeBookGUI.getMaxRecipesPerPage() + i;
                
                CustomItem selectedRecipe = RecipeBookGUI.getEnabledRecipeAt(recipeIndex);
                if (selectedRecipe != null && selectedRecipe.hasRecipe()) {
                    player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.1f);
                    openRecipeDetailGUI(player, selectedRecipe);
                }
                return;
            }
        }
    }
    
    /**
     * Handle clicks in the recipe detail GUI
     */
    private void handleRecipeDetailClick(InventoryClickEvent event, Inventory gui, Player player, int slot) {
        // Allow player inventory interactions
        if (slot >= 54) {
            return;
        }
        
        event.setCancelled(true);
        boolean debug = ConfigManager.getInstance().getMainConfig().isDebugEnabled();
        
        if (debug) {
            System.out.println("[RecipeBook] Detail GUI click - slot: " + slot);
        }
        
        // Back button - return to recipe book
        if (slot == RecipeDetailGUI.getBackButtonSlot()) {
            if (debug) {
                System.out.println("[RecipeBook] Back button clicked");
            }
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 0.8f);
            int currentPage = playerRecipePages.getOrDefault(player.getUniqueId(), 0);
            openRecipeBookGUI(player, currentPage);
            return;
        }
        
        // Give items button (creative mode only)
        if (slot == RecipeDetailGUI.getGiveItemsButtonSlot()) {
            if (debug) {
                System.out.println("[RecipeBook] Give items button clicked at slot " + slot);
            }
            if (player.getGameMode() != GameMode.CREATIVE) {
                player.sendMessage(Component.text("This button only works in creative mode!", TextColor.color(0xFF0000)));
                player.playSound(player.getLocation(), Sound.BLOCK_LAVA_EXTINGUISH, 1.0f, 1.0f);
                return;
            }
            
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.2f);
            
            // Get the recipe from stored player data
            CustomItem recipe = playerCurrentRecipe.get(player.getUniqueId());
            if (recipe != null && recipe.hasRecipe()) {
                RecipeDetailGUI.giveRecipeItems(player, recipe.getRecipe());
                player.sendMessage(Component.text("Items given! (" + recipe.getId() + ")", TextColor.color(0x00FF00)));
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 1.0f, 2.0f);
                if (debug) {
                    System.out.println("[RecipeBook] Gave items for recipe: " + recipe.getId());
                }
            } else {
                player.sendMessage(Component.text("Error: Recipe not found!", TextColor.color(0xFF0000)));
                player.playSound(player.getLocation(), Sound.BLOCK_LAVA_EXTINGUISH, 1.0f, 1.0f);
                if (debug) {
                    System.out.println("[RecipeBook] ERROR: Recipe not found for player " + player.getName());
                }
            }
            return;
        }
    }
    
    /**
     * Open the recipe book GUI
     */
    public void openRecipeBookGUI(Player player, int page) {
        playerRecipePages.put(player.getUniqueId(), Math.max(0, page));
        Inventory gui = RecipeBookGUI.createRecipeBookGUI(page);
        playerRecipeGUIs.put(player.getUniqueId(), gui);
        player.openInventory(gui);
        player.playSound(player.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 1.0f, 1.0f);
    }
    
    /**
     * Open the recipe detail GUI
     */
    public void openRecipeDetailGUI(Player player, CustomItem customItem) {
        Inventory gui = RecipeDetailGUI.createRecipeDetailGUI(customItem, player);
        playerRecipeGUIs.put(player.getUniqueId(), gui);
        playerCurrentRecipe.put(player.getUniqueId(), customItem);
        player.openInventory(gui);
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
    }
    
    /**
     * Set the forge listener reference
     */
    public void setForgeListener(ForgeListener forgeListener) {
        this.forgeListener = forgeListener;
    }
    
    /**
     * Open the forge GUI (from recipe book)
     */
    private void openForgeGUI(Player player) {
        // Clear recipe book state
        playerRecipePages.remove(player.getUniqueId());
        playerRecipeGUIs.remove(player.getUniqueId());
        playerCurrentRecipe.remove(player.getUniqueId());
        
        // Use ForgeListener to properly reopen the forge GUI with full state refresh
        if (forgeListener != null) {
            forgeListener.reopenForgeGUI(player);
        } else {
            // Fallback if listener not set
            Inventory forge = ForgeGUI.createForgeGUI();
            player.openInventory(forge);
        }
    }
    
    /**
     * Check if inventory is the recipe book GUI
     */
    private boolean isRecipeBookGUI(Inventory inventory) {
        if (inventory.getSize() != 54 || inventory.getViewers().isEmpty()) {
            return false;
        }
        try {
            String title = inventory.getViewers().get(0).getOpenInventory().getTitle();
            return title != null && title.contains("Recipe Book");
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Check if inventory is the recipe detail GUI
     */
    private boolean isRecipeDetailGUI(Inventory inventory) {
        if (inventory.getSize() != 54 || inventory.getViewers().isEmpty()) {
            return false;
        }
        try {
            String title = inventory.getViewers().get(0).getOpenInventory().getTitle();
            return title != null && title.contains("Recipe:");
        } catch (Exception e) {
            return false;
        }
    }
}
