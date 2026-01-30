package org.m9mx.lumenV2.systems.forge;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.m9mx.lumenV2.item.ItemRegistry;
import org.m9mx.lumenV2.item.Recipe;
import org.m9mx.lumenV2.item.RecipeType;
import org.m9mx.lumenV2.systems.ForgeSystem;
import org.m9mx.lumenV2.config.ConfigManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Listens to player interactions with smithing tables and forge GUI
 */
public class ForgeListener implements Listener {

    private final ForgeSystem forgeSystem;
    private final Map<UUID, Inventory> playerGUIs = new HashMap<>();
    private final RecipeBookListener recipeBookListener;
    private final Map<UUID, Integer> gridUpdateTasks = new HashMap<>();
    private final Map<UUID, String> previousRecipeState = new HashMap<>();
    private final Map<UUID, Integer> previousQuickCraftCount = new HashMap<>();
    private final Map<UUID, org.bukkit.Location> smithingTableLocations = new HashMap<>();
    private static final int GRID_UPDATE_INTERVAL = 10; // ticks

    public ForgeListener(ForgeSystem forgeSystem) {
        this.forgeSystem = forgeSystem;
        this.recipeBookListener = new RecipeBookListener();
        this.recipeBookListener.setForgeListener(this);
    }
    
    public RecipeBookListener getRecipeBookListener() {
        return recipeBookListener;
    }
    
    /**
     * Public method to open/reopen the forge GUI (called from recipe book)
     */
    public void reopenForgeGUI(Player player) {
        openForgeGUI(player);
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (!forgeSystem.isEnabled()) {
            return;
        }

        Block block = event.getClickedBlock();
        
        // Check for clicking ritual reward barrier
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK && block != null && block.getType() == Material.BARRIER) {
            if (forgeSystem.getRitualSystem().isRewardBlock(block.getLocation())) {
                Player player = event.getPlayer();
                ItemStack rewardItem = forgeSystem.getRitualSystem().getRewardItem(block.getLocation());
                String itemType = forgeSystem.getRitualSystem().getRewardItemType(block.getLocation());
                
                if (rewardItem != null && itemType != null) {
                    // Register the item craft
                    forgeSystem.getItemCraftsManager().registerItemCraft(player.getUniqueId(), player.getName(), itemType);
                    
                    // Give item to player
                    player.getInventory().addItem(rewardItem.clone());
                    player.playSound(player.getLocation(), Sound.BLOCK_AMETHYST_CLUSTER_BREAK, 1.0f, 1.5f);

                    
                    // Prepare and send broadcast message to all players
                     net.kyori.adventure.text.minimessage.MiniMessage miniMsg = net.kyori.adventure.text.minimessage.MiniMessage.miniMessage();
                     net.kyori.adventure.text.Component claimMsg = RitualSystem.createMiniMessageComponent(
                         "<gold>═══════════════════════════════\n" +
                         "<green>✦ Ritual Reward Claimed ✦\n" +
                         "<aqua>Player: <white>" + player.getName() + "\n" +
                         "<aqua>Location: <white>" + block.getX() + ", " + block.getY() + ", " + block.getZ() + "\n" +
                         "<aqua>Item: ",
                         rewardItem).append(miniMsg.deserialize("\n<gold>═══════════════════════════════"));
                    
                    // Primary broadcast
                    org.bukkit.Bukkit.broadcast(claimMsg);
                    
                    // Secondary broadcast to each player individually as backup
                    for (org.bukkit.entity.Player onlinePlayer : org.bukkit.Bukkit.getOnlinePlayers()) {
                        if (!onlinePlayer.equals(player)) { // Don't send twice to the same player
                            onlinePlayer.sendMessage(claimMsg);
                        }
                    }
                    
                    // Log successful broadcast
                    String itemName = rewardItem.hasItemMeta() && rewardItem.getItemMeta().hasDisplayName() 
                        ? net.kyori.adventure.text.serializer.plain.PlainComponentSerializer.plain().serialize(rewardItem.getItemMeta().displayName())
                        : rewardItem.getType().name();
                    
                    forgeSystem.getPlugin().getLogger().info("[LumenV2] Ritual reward claimed broadcast sent for " + player.getName() + 
                                          " item " + itemName);
                    
                    // Remove barrier blocks
                    forgeSystem.getRitualSystem().removeRewardBlock(block.getLocation());
                }
                
                event.setCancelled(true);
                return;
            }
        }

        // Check for right-click on smithing table
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        if (block == null || block.getType() != Material.SMITHING_TABLE) {
            return;
        }

        event.setCancelled(true);
        
        // Save smithing table location for ritual use
        Player player = event.getPlayer();
        smithingTableLocations.put(player.getUniqueId(), block.getLocation());

        // Open the menu GUI
        openMenuGUI(player);
    }
    
    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        if (!forgeSystem.isEnabled()) {
            return;
        }
        
        if (event.getBlock().getType() == Material.BARRIER) {
            if (forgeSystem.getRitualSystem().isRewardBlock(event.getBlock().getLocation())) {
                event.setCancelled(true);
            }
        }
    }

    /**
     * Open the menu GUI with FORGE and SMITHING buttons
     */
    private void openMenuGUI(Player player) {
        Inventory menu = SmithingTableGUI.createMenuGUI();
        playerGUIs.put(player.getUniqueId(), menu);
        player.openInventory(menu);
    }

    /**
     * Open the forge GUI
     */
    private void openForgeGUI(Player player) {
        Inventory forge = ForgeGUI.createForgeGUI();
        playerGUIs.put(player.getUniqueId(), forge);

        UUID playerId = player.getUniqueId();
        
        // Initial grid state update
        updateGridState(forge, player);

        // Start repeating grid update task
        int taskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(
            Bukkit.getPluginManager().getPlugin("LumenV2"),
            () -> {
                // Check if player still has forge open
                if (playerGUIs.containsKey(playerId)) {
                    Inventory currentGui = playerGUIs.get(playerId);
                    Player currentPlayer = Bukkit.getPlayer(playerId);
                    if (currentPlayer != null) {
                        updateGridState(currentGui, currentPlayer);
                    }
                }
            },
            0L,
            GRID_UPDATE_INTERVAL
        );
        gridUpdateTasks.put(playerId, taskId);

        player.openInventory(forge);
    }

    /**
     * Open the normal smithing table GUI
     */
    private void openSmithingGUI(Player player) {
        player.openSmithingTable(null, true);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!forgeSystem.isEnabled()) {
            return;
        }

        Inventory gui = event.getInventory();
        Player player = (Player) event.getWhoClicked();

        // Check if it's the menu GUI
        if (isMenuGUI(gui)) {
            event.setCancelled(true);

            int slot = event.getRawSlot();
            if (slot == SmithingTableGUI.getForgeButtonSlot()) {
                openForgeGUI(player);
            } else if (slot == SmithingTableGUI.getSmithingButtonSlot()) {
                openSmithingGUI(player);
            }
            return;
        }

        // Check if it's the forge GUI
        if (isForgeGUI(gui)) {
            handleForgeClick(event, gui, player);
            return;
        }
    }

    /**
     * Handle clicks in the forge GUI
     */
    private void handleForgeClick(InventoryClickEvent event, Inventory gui, @SuppressWarnings("unused") Player player) {
        int slot = event.getRawSlot();

        // Allow interactions with player inventory (slots >= 54)
        if (slot >= 54) {
            return;
        }

        // Crafting grid slots - allow movement from/to inventory
        if (ForgeGUI.isCraftingGridSlot(slot)) {
            event.setCancelled(false);
            // Grid state is checked every 10 ticks by the repeating task
            return;
        }

        // Block all other GUI slots by default
        event.setCancelled(true);

        // Quick craft slots - handle crafting
        if (ForgeGUI.isQuickCraftSlot(slot)) {
            List<org.m9mx.lumenV2.item.CustomItem> options = ForgeManager.getQuickCraftOptions(player.getInventory());
            int quickSlotIndex = -1;
            int[] quickSlots = ForgeGUI.getQuickCraftSlots();
            for (int i = 0; i < quickSlots.length; i++) {
                if (quickSlots[i] == slot) {
                    quickSlotIndex = i;
                    break;
                }
            }

            if (quickSlotIndex >= 0 && quickSlotIndex < options.size()) {
                org.m9mx.lumenV2.item.CustomItem selectedItem = options.get(quickSlotIndex);
                Recipe recipe = selectedItem.getRecipe();

                // Place recipe in grid from player inventory
                ForgeManager.placeRecipeInGrid(gui, player.getInventory(), recipe);
                // Grid state will be updated in the next 10-tick cycle
            }
            return;
        }

        // Output slot
        if (slot == ForgeGUI.getOutputSlot()) {
            boolean debug = ConfigManager.getInstance().getMainConfig().isDebugEnabled();
            Recipe recipe = ForgeManager.getRecipeFromGrid(gui);
            if (debug) {
                System.out.println("[ForgeListener] Output slot clicked - Recipe: " + (recipe != null ? recipe.getId() : "NONE"));
            }

            if (recipe != null) {
                org.m9mx.lumenV2.item.CustomItem resultItem = ItemRegistry.getInstance().getItem(recipe.getResult());
                if (resultItem != null) {
                    // Check recipe type
                    if (recipe.getType() == RecipeType.NORMAL) {
                        // Give item to player inventory
                        ItemStack result = resultItem.build();
                        player.getInventory().addItem(result);
                        player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_USE, 1.0f, 1.2f);

                        // Consume exact amounts from grid
                        consumeRecipeFromGrid(gui, recipe);
                        // Grid state will be updated in the next 10-tick cycle
                    } else if (recipe.getType() == RecipeType.RITUAL) {
                        // Check if anyone else crafted this item
                        if (forgeSystem.getItemCraftsManager().hasBeenCrafted(recipe.getResult())) {
                           player.sendMessage(Component.text("This item has already been crafted by someone else.", TextColor.color(0xFF0000)));
                           player.playSound(player.getLocation(), Sound.BLOCK_LAVA_EXTINGUISH, 1.0f, 1.0f);
                           
                           // Return items in grid to player
                           List<ItemStack> gridContents = ForgeManager.getGridContents(gui);
                           for (ItemStack item : gridContents) {
                               player.getInventory().addItem(item);
                           }
                           // Clear the grid after returning items
                           ForgeManager.clearGrid(gui);
                           return;
                        }
                        
                        // Check if a ritual is already in progress
                        if (forgeSystem.getRitualSystem().isRitualInProgress()) {
                            player.sendMessage(Component.text("A ritual is already in progress. Please wait.", TextColor.color(0xFF6B6B)));
                            player.playSound(player.getLocation(), Sound.BLOCK_LAVA_EXTINGUISH, 1.0f, 1.0f);
                            
                            // Return items in grid to player
                            List<ItemStack> gridContents = ForgeManager.getGridContents(gui);
                            for (ItemStack item : gridContents) {
                                player.getInventory().addItem(item);
                            }
                            // Clear the grid after returning items
                            ForgeManager.clearGrid(gui);
                            return;
                        }
                        
                        // Check server-wide cooldown
                        long currentTime = System.currentTimeMillis();
                        long serverCooldownEnd = forgeSystem.getRitualSystem().getServerRitualCooldownEnd();
                        if (currentTime < serverCooldownEnd) {
                            long millisecondsRemaining = serverCooldownEnd - currentTime;
                            long minutesRemaining = millisecondsRemaining / (60 * 1000);
                            player.sendMessage(Component.text("The server must wait " + minutesRemaining + " minutes before the next ritual can begin.", TextColor.color(0xFF0000)));
                            player.playSound(player.getLocation(), Sound.BLOCK_LAVA_EXTINGUISH, 1.0f, 1.0f);
                            
                            // Return items in grid to player
                            List<ItemStack> gridContents = ForgeManager.getGridContents(gui);
                            for (ItemStack item : gridContents) {
                                player.getInventory().addItem(item);
                            }
                            // Clear the grid after returning items
                            ForgeManager.clearGrid(gui);
                            return;
                        }
                        
                        // If ritual system is disabled, craft as NORMAL but still register
                        if (!forgeSystem.isRitualSystemEnabled()) {
                            ItemStack result = resultItem.build();
                            player.getInventory().addItem(result);
                            player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_USE, 1.0f, 1.2f);
                            
                            // Register the item craft (only 1 per server)
                            forgeSystem.getItemCraftsManager().registerItemCraft(player.getUniqueId(), player.getName(), recipe.getResult());
                            
                            // Consume recipe items
                            consumeRecipeFromGrid(gui, recipe);
                            // Grid state will be updated in the next 10-tick cycle
                            return;
                        }
                        
                        // Consume items from grid
                         consumeRecipeFromGrid(gui, recipe);
                         player.playSound(player.getLocation(), Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, 1.0f, 1.0f);
                         // Grid state will be updated in the next 10-tick cycle
                         
                         // Start ritual
                          player.closeInventory();
                          
                          org.bukkit.Location smithingTableLoc = smithingTableLocations.get(player.getUniqueId());
                          if (smithingTableLoc != null) {
                              forgeSystem.getRitualSystem().startRitual(player, smithingTableLoc, resultItem.build(), recipe.getResult());
                          } else {
                              forgeSystem.getPlugin().getLogger().severe("[ForgeListener] ERROR: Smithing table location not found!");
                          }
                    }
                }
            }
            return;
        }

        // Recipe GUI slot - open recipe book
        if (slot == ForgeGUI.getRecipeGUISlot()) {
            recipeBookListener.openRecipeBookGUI(player, 0);
            return;
        }
    }

    /**
     * Update all grid-related displays: quick craft slots, output border, and recipe detection
     */
    private void updateGridState(Inventory gui, Player player) {
        boolean debug = ConfigManager.getInstance().getMainConfig().isDebugEnabled();
        UUID playerId = player.getUniqueId();
        
        // Get recipe from current grid state
        Recipe recipe = ForgeManager.getRecipeFromGrid(gui);
        String currentRecipeState = recipe != null ? recipe.getId() : "NONE";
        String previousState = previousRecipeState.getOrDefault(playerId, "");
        
        // Update recipe state
        previousRecipeState.put(playerId, currentRecipeState);
        
        // Set output slot
        if (recipe != null) {
            org.m9mx.lumenV2.item.CustomItem resultItem = ItemRegistry.getInstance().getItem(recipe.getResult());
            if (resultItem != null) {
                gui.setItem(ForgeGUI.getOutputSlot(), resultItem.build());
            }
        } else {
            gui.setItem(ForgeGUI.getOutputSlot(), null);
        }
        
        // Update quick craft options
        List<org.m9mx.lumenV2.item.CustomItem> options = ForgeManager.getQuickCraftOptions(player.getInventory());
        int[] quickSlots = ForgeGUI.getQuickCraftSlots();
        int currentQuickCraftCount = options.size();
        int previousCount = previousQuickCraftCount.getOrDefault(playerId, -1);
        
        // Update quick craft count
         previousQuickCraftCount.put(playerId, currentQuickCraftCount);
         
         for (int i = 0; i < quickSlots.length; i++) {
            if (i < options.size()) {
                org.m9mx.lumenV2.item.CustomItem item = options.get(i);
                ItemStack display = item.build();
                gui.setItem(quickSlots[i], display);
            } else {
                // Show red glass pane for unavailable quick craft slots
                ItemStack redPane = new org.bukkit.inventory.ItemStack(org.bukkit.Material.RED_STAINED_GLASS_PANE);
                org.bukkit.inventory.meta.ItemMeta meta = redPane.getItemMeta();
                if (meta != null) {
                    meta.displayName(net.kyori.adventure.text.Component.text("No Quick Craft Available", net.kyori.adventure.text.format.NamedTextColor.RED));
                    redPane.setItemMeta(meta);
                }
                gui.setItem(quickSlots[i], redPane);
            }
        }
        
        // Update output border based on recipe validity
        ForgeGUI.updateOutputBorder(gui, recipe != null);
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!forgeSystem.isEnabled()) {
            return;
        }

        Player player = (Player) event.getPlayer();
        UUID playerId = player.getUniqueId();
        Inventory gui = event.getInventory();

        // If closing forge GUI, return items to player and cancel update task
        if (isForgeGUI(gui)) {
            List<ItemStack> gridContents = ForgeManager.getGridContents(gui);
            for (ItemStack item : gridContents) {
                player.getInventory().addItem(item);
            }
            playerGUIs.remove(playerId);
            
            // Cancel the repeating grid update task
            if (gridUpdateTasks.containsKey(playerId)) {
                try {
                    Bukkit.getScheduler().cancelTask(gridUpdateTasks.get(playerId));
                } catch (Exception e) {
                    // Task already completed
                }
                gridUpdateTasks.remove(playerId);
            }
            
            // Clean up state tracking
            previousRecipeState.remove(playerId);
            previousQuickCraftCount.remove(playerId);
            // Don't remove smithingTableLocations here - keep it for ritual crafting
            // It will be overwritten the next time they open a smithing table
        }
    }

    /**
     * Consume exact amounts of items from the grid for one recipe craft
     */
    private void consumeRecipeFromGrid(Inventory gui, Recipe recipe) {
        Recipe.RecipeSlot[][] grid = recipe.getGrid();
        int[] gridSlots = ForgeGUI.getCraftingGridSlots();
        
        int gridIndex = 0;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                Recipe.RecipeSlot recipeSlot = grid[row][col];
                if (recipeSlot != null) {
                    int slot = gridSlots[gridIndex];
                    ItemStack item = gui.getItem(slot);
                    
                    if (item != null && !item.isEmpty()) {
                        // Decrease amount by exact recipe requirement
                        int newAmount = item.getAmount() - recipeSlot.getAmount();
                        if (newAmount > 0) {
                            item.setAmount(newAmount);
                        } else {
                            // Clear slot if amount becomes 0
                            gui.setItem(slot, null);
                        }
                    }
                }
                gridIndex++;
            }
        }
    }

    /**
     * Check if inventory is the menu GUI (3 rows = 27 slots)
     */
    private boolean isMenuGUI(Inventory inventory) {
        return inventory.getSize() == 27 && 
               !inventory.getViewers().isEmpty() &&
               inventory.getViewers().get(0) instanceof Player &&
               inventory.getViewers().get(0).getOpenInventory().getTitle().contains("Smithing Table");
    }

    /**
     * Check if inventory is the forge GUI (6 rows = 54 slots)
     */
    private boolean isForgeGUI(Inventory inventory) {
        return inventory.getSize() == 54 && 
               !inventory.getViewers().isEmpty() &&
               inventory.getViewers().get(0) instanceof Player &&
               inventory.getViewers().get(0).getOpenInventory().getTitle().contains("Forge");
    }
}
