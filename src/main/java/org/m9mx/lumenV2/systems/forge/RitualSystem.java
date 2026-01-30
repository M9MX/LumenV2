package org.m9mx.lumenV2.systems.forge;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.*;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.m9mx.lumenV2.data.ItemCraftsManager;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Ritual crafting system with visual effects
 */
public class RitualSystem implements Listener {
    private final Plugin plugin;
    private final ItemCraftsManager itemCraftsManager;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();
    private final Map<UUID, RitualSession> activeRituals = new HashMap<>();
    private final Map<String, Boolean> rewardBlocks = new HashMap<>();
    private final Map<String, ItemDisplay> rewardDisplays = new HashMap<>();
    private final Map<String, ItemStack> rewardItems = new HashMap<>();
    private final Map<String, String> rewardItemTypes = new HashMap<>();
    private final Map<String, String> rewardBlockPairs = new HashMap<>();
    private long serverRitualCooldownEnd = 0;
    
    private int ritualDuration;
    private static final int RITUAL_STAGES = 10;
    private static final double MAX_RADIUS = 6.0;
    private static final float PARTICLE_SIZE_MODIFIER = 2.5f;
    private static final float RING_PARTICLE_SIZE = 0.8f;
    private static final float STAGE1_PARTICLE_SIZE = 1.5f;
    private static final float CIRCLE_PARTICLE_SIZE = 1.8f;
    private static final double ITEM_SPAWN_HEIGHT = 1.0;
    private static final long RITUAL_COOLDOWN_MS = 30 * 60 * 1000;
    
    public RitualSystem(Plugin plugin, ItemCraftsManager itemCraftsManager) {
        this.plugin = plugin;
        this.itemCraftsManager = itemCraftsManager;
        loadConfiguration();
    }
    
    private void loadConfiguration() {
        // Load ritual duration from systems.yml via ConfigManager
        org.m9mx.lumenV2.config.ConfigManager configManager = org.m9mx.lumenV2.config.ConfigManager.getInstance();
        org.bukkit.configuration.ConfigurationSection ritualSection = configManager.getSystemsConfig().getConfig().getConfigurationSection("ritual");
        if (ritualSection != null) {
            this.ritualDuration = ritualSection.getInt("duration_seconds", 600);
        } else {
            this.ritualDuration = 600; // Default 10 minutes
        }
    }
    
    /**
     * Reload configuration (called by reload command)
     */
    public void reloadConfiguration() {
        loadConfiguration();
    }
    
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        // Handle clicking reward barrier - this is now handled in ForgeListener, keeping only essential checks
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK && event.getClickedBlock() != null && event.getClickedBlock().getType() == Material.BARRIER) {
            org.bukkit.block.Block block = event.getClickedBlock();
            String blockKey = block.getX() + "," + block.getY() + "," + block.getZ();
            String blockKeyBelow = block.getX() + "," + (block.getY() - 1) + "," + block.getZ();
            String blockKeyAbove = block.getX() + "," + (block.getY() + 1) + "," + block.getZ();
            
            // Check if this is a reward block (try current location, one below, and one above)
            String rewardKey = rewardBlocks.containsKey(blockKey) ? blockKey : 
                               rewardBlocks.containsKey(blockKeyBelow) ? blockKeyBelow :
                               rewardBlocks.containsKey(blockKeyAbove) ? blockKeyAbove : null;
            
            if (rewardKey != null) {
                Player rewardPlayer = event.getPlayer();
                
                ItemStack rewardItem = rewardItems.get(rewardKey);
                String itemType = rewardItemTypes.get(rewardKey);
                
                
                if (rewardItem != null) {
                    // Register the item in persistent storage when player receives it
                    itemCraftsManager.registerItemCraft(rewardPlayer.getUniqueId(), rewardPlayer.getName(), itemType);
                    rewardPlayer.getInventory().addItem(rewardItem.clone());
                    
                    // Send personal message to the player who claimed the reward
                    Component personalClaimMsg = miniMessage.deserialize(
                        "<gold>═══════════════════════════════\n" +
                        "<green>✦ Ritual Reward Claimed ✦\n" +
                        "<aqua>You have successfully claimed your ritual reward!\n" +
                        "<aqua>Item: "
                    ).append(createItemComponent(rewardItem)).append(miniMessage.deserialize("\n<gold>═══════════════════════════════"));
                    rewardPlayer.sendMessage(personalClaimMsg);
                    
                    // Broadcast who claimed the reward
                    Component claimMsg = miniMessage.deserialize(
                        "<gold>═══════════════════════════════\n" +
                        "<green>✦ Ritual Reward Claimed ✦\n" +
                        "<aqua>Player: <white>" + rewardPlayer.getName() + "\n" +
                        "<aqua>Location: <white>" + block.getX() + ", " + block.getY() + ", " + block.getZ() + "\n" +
                        "<aqua>Item: "
                    ).append(createItemComponent(rewardItem)).append(miniMessage.deserialize("\n<gold>═══════════════════════════════"));
                    
                    // Use broadcast with a string message as fallback if component broadcast doesn't work
                    Bukkit.broadcast(claimMsg);
                    
                    
                }
                
                rewardPlayer.playSound(rewardPlayer.getLocation(), Sound.BLOCK_AMETHYST_CLUSTER_BREAK, 1.0f, 1.5f);
                
                // Remove barrier blocks
                Location bottomLoc = new Location(block.getWorld(), 
                    Integer.parseInt(rewardKey.split(",")[0]), 
                    Integer.parseInt(rewardKey.split(",")[1]), 
                    Integer.parseInt(rewardKey.split(",")[2]));
                Location topLoc = bottomLoc.clone().add(0, 1, 0);

                
                if (bottomLoc.getBlock().getType() == Material.BARRIER) {
                    bottomLoc.getBlock().setType(Material.AIR);
                }
                if (topLoc.getBlock().getType() == Material.BARRIER) {
                    topLoc.getBlock().setType(Material.AIR);
                }
                
                // Remove item display
                ItemDisplay display = rewardDisplays.get(rewardKey);
                if (display != null && !display.isDead()) {
                    display.remove();
                }
                
                // Clean up all maps using the paired key
                String pairKey = rewardBlockPairs.get(rewardKey);
                String[] keys = pairKey.split(",");
                String bottomKey = pairKey;
                String topKey = keys[0] + "," + (Integer.parseInt(keys[1]) + 1) + "," + keys[2];
                
                rewardBlocks.remove(bottomKey);
                rewardBlocks.remove(topKey);
                rewardItems.remove(bottomKey);
                rewardItems.remove(topKey);
                rewardItemTypes.remove(bottomKey);
                rewardItemTypes.remove(topKey);
                rewardBlockPairs.remove(bottomKey);
                rewardBlockPairs.remove(topKey);
                rewardDisplays.remove(bottomKey);
                event.setCancelled(true);
            } else {
                // Extra logging to understand if the block clicked is not a reward block
                
            }
        }
    }
    
    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        if (event.getBlock().getType() == Material.BARRIER) {
            String blockKey = event.getBlock().getX() + "," + event.getBlock().getY() + "," + event.getBlock().getZ();
            if (rewardBlocks.containsKey(blockKey)) {
                event.setCancelled(true);
            }
        }
    }
    
    private Component createItemComponent(ItemStack item) {
        Component itemComponent = Component.empty();
        
        if (item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
            // The display name already is a Component from the ItemMeta
            itemComponent = item.getItemMeta().displayName();
        } else {
            itemComponent = Component.text(item.getType().name().replace("_", " "));
        }
        
        if (item.hasItemMeta() && item.getItemMeta().hasLore()) {
            java.util.List<Component> lore = item.getItemMeta().lore();
            if (lore != null && !lore.isEmpty()) {
                Component loreComponent = Component.empty();
                for (int i = 0; i < lore.size(); i++) {
                    loreComponent = loreComponent.append(lore.get(i));
                    if (i < lore.size() - 1) {
                        loreComponent = loreComponent.append(Component.newline());
                    }
                }
                itemComponent = itemComponent.hoverEvent(HoverEvent.showText(loreComponent));
            }
        }
        
        return itemComponent;
    }
    
    /**
     * Static helper method to create a mini message component with an item
     */
    public static Component createMiniMessageComponent(String baseMessage, ItemStack item) {
        net.kyori.adventure.text.minimessage.MiniMessage miniMessage = net.kyori.adventure.text.minimessage.MiniMessage.miniMessage();
        Component itemComponent = Component.empty();
        
        if (item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
            // The display name already is a Component from the ItemMeta
            itemComponent = item.getItemMeta().displayName();
        } else {
            itemComponent = Component.text(item.getType().name().replace("_", " "));
        }
        
        if (item.hasItemMeta() && item.getItemMeta().hasLore()) {
            java.util.List<Component> lore = item.getItemMeta().lore();
            if (lore != null && !lore.isEmpty()) {
                Component loreComponent = Component.empty();
                for (int i = 0; i < lore.size(); i++) {
                    loreComponent = loreComponent.append(lore.get(i));
                    if (i < lore.size() - 1) {
                        loreComponent = loreComponent.append(Component.newline());
                    }
                }
                itemComponent = itemComponent.hoverEvent(net.kyori.adventure.text.event.HoverEvent.showText(loreComponent));
            }
        }
        
        return miniMessage.deserialize(baseMessage).append(itemComponent);
    }
    
    /**
     * Check if a reward block exists at the given location
     */
    public boolean isRewardBlock(Location location) {
        String blockKey = location.getBlockX() + "," + location.getBlockY() + "," + location.getBlockZ();
        return rewardBlocks.containsKey(blockKey);
    }
    
    /**
     * Get the reward item at the given location
     */
    public ItemStack getRewardItem(Location location) {
        String blockKey = location.getBlockX() + "," + location.getBlockY() + "," + location.getBlockZ();
        return rewardItems.get(blockKey);
    }
    
    /**
     * Get the reward item type at the given location
     */
    public String getRewardItemType(Location location) {
        String blockKey = location.getBlockX() + "," + location.getBlockY() + "," + location.getBlockZ();
        return rewardItemTypes.get(blockKey);
    }
    
    /**
     * Remove a reward block (both bottom and top)
     */
    public void removeRewardBlock(Location location) {
        String blockKey = location.getBlockX() + "," + location.getBlockY() + "," + location.getBlockZ();
        
        // Find the paired key if this is the top block
        String pairKey = rewardBlockPairs.get(blockKey);
        if (pairKey == null) {
            pairKey = blockKey; // If no pair, use itself as pair
        }
        
        String[] keys = pairKey.split(",");
        String bottomKey = pairKey;
        String topKey = keys[0] + "," + (Integer.parseInt(keys[1]) + 1) + "," + keys[2];
        
        // Remove blocks
        Location bottomBlockLoc = new Location(location.getWorld(),
            Integer.parseInt(keys[0]),
            Integer.parseInt(keys[1]),
            Integer.parseInt(keys[2]));
        Location topBlockLoc = bottomBlockLoc.clone().add(0, 1, 0);
        
        if (bottomBlockLoc.getBlock().getType() == Material.BARRIER) {
            bottomBlockLoc.getBlock().setType(Material.AIR);
        }
        if (topBlockLoc.getBlock().getType() == Material.BARRIER) {
            topBlockLoc.getBlock().setType(Material.AIR);
        }
        
        // Remove item display
        ItemDisplay display = rewardDisplays.get(pairKey);
        if (display != null && !display.isDead()) {
            display.remove();
        }
        
        // Clean up maps
        rewardBlocks.remove(bottomKey);
        rewardBlocks.remove(topKey);
        rewardItems.remove(bottomKey);
        rewardItems.remove(topKey);
        rewardItemTypes.remove(bottomKey);
        rewardItemTypes.remove(topKey);
        rewardBlockPairs.remove(bottomKey);
        rewardBlockPairs.remove(topKey);
        rewardDisplays.remove(bottomKey);
    }
    
    /**
     * Check if a ritual is currently in progress
     */
    public boolean isRitualInProgress() {
        return !activeRituals.isEmpty();
    }
    
    public void startRitual(Player player, Location location, ItemStack finalItem, String itemType) {
        UUID playerId = player.getUniqueId();
        
        // Check if player already has an active ritual
        if (activeRituals.containsKey(playerId)) {
            Component message = miniMessage.deserialize("<red>You already have a ritual in progress. Wait for it to complete.");
            player.sendMessage(message);
            return;
        }
        
        // Check if server is on cooldown (server-wide cooldown)
        long currentTime = System.currentTimeMillis();
        if (currentTime < serverRitualCooldownEnd) {
            long millisecondsRemaining = serverRitualCooldownEnd - currentTime;
            long minutesRemaining = millisecondsRemaining / (60 * 1000);
            Component message = miniMessage.deserialize("<red>The server must wait <white>" + minutesRemaining + " minutes<red> before the next ritual can begin.");
            player.sendMessage(message);
            return;
        }
        
        // Fetch the recipe from ItemRegistry to get recipe items
        org.m9mx.lumenV2.item.CustomItem resultCustomItem = org.m9mx.lumenV2.item.ItemRegistry.getInstance().getItem(itemType);
        final ItemStack[] recipeItems;
        
        if (resultCustomItem != null && resultCustomItem.getRecipe() != null) {
            org.m9mx.lumenV2.item.Recipe recipe = resultCustomItem.getRecipe();
            java.util.List<org.m9mx.lumenV2.item.Recipe.RecipeSlot> occupiedSlots = recipe.getOccupiedSlots();
            
            java.util.List<ItemStack> recipeItemsList = new java.util.ArrayList<>();
            for (org.m9mx.lumenV2.item.Recipe.RecipeSlot recipeSlot : occupiedSlots) {
                String slotItemId = recipeSlot.getItemId();
                
                // Check if it's a LUMEN: prefixed custom item (e.g. LUMEN:TEST)
                if (slotItemId.startsWith("LUMEN:")) {
                    String customItemId = slotItemId.substring(6); // Remove "LUMEN:" prefix
                    // Convert to lowercase to match registry keys
                    customItemId = customItemId.toLowerCase();
                    org.m9mx.lumenV2.item.CustomItem customItem = org.m9mx.lumenV2.item.ItemRegistry.getInstance().getItem(customItemId);
                    if (customItem != null) {
                        ItemStack itemStack = customItem.build();
                        itemStack.setAmount(recipeSlot.getAmount());
                        recipeItemsList.add(itemStack);
                    }
                } else {
                    // Try to get as custom item first (without LUMEN: prefix)
                    org.m9mx.lumenV2.item.CustomItem customItem = org.m9mx.lumenV2.item.ItemRegistry.getInstance().getItem(slotItemId.toLowerCase());
                    if (customItem != null) {
                        ItemStack itemStack = customItem.build();
                        itemStack.setAmount(recipeSlot.getAmount());
                        recipeItemsList.add(itemStack);
                    } else {
                        // Try as vanilla material
                        try {
                            Material vanillaMaterial = Material.valueOf(slotItemId.toUpperCase());
                            ItemStack vanillaItem = new ItemStack(vanillaMaterial, recipeSlot.getAmount());
                            recipeItemsList.add(vanillaItem);
                        } catch (IllegalArgumentException e) {
                            // Could not find item, skip
                        }
                    }
                }
            }
            recipeItems = recipeItemsList.toArray(new ItemStack[0]);
        } else {
            recipeItems = new ItemStack[0];
        }
        
        RitualSession session = new RitualSession(playerId, location, ritualDuration * 20);
        activeRituals.put(playerId, session);
        session.finalItem = finalItem;
        session.itemType = itemType;
        
        Location centerLoc = new Location(location.getWorld(), location.getBlockX() + 0.5, location.getBlockY() + ITEM_SPAWN_HEIGHT, location.getBlockZ() + 0.5);
        
        player.playSound(location, Sound.UI_TOAST_IN, 0.8f, 1.2f);
        
        BossBar bossBar = Bukkit.createBossBar("", BarColor.PURPLE, BarStyle.SOLID);
        bossBar.setProgress(1.0);
        
        Component startMsg = miniMessage.deserialize(
            "<gold>═══════════════════════════════\n" +
            "<yellow>✦ Ritual Started ✦\n" +
            "<aqua>Player: <white>" + player.getName() + "\n" +
            "<aqua>Location: <white>" + location.getBlockX() + ", " + location.getBlockY() + ", " + location.getBlockZ() + "\n" +
            "<aqua>Creating: "
        ).append(createItemComponent(finalItem)).append(miniMessage.deserialize("\n<gold>═══════════════════════════════"));
        Bukkit.broadcast(startMsg);
        
        new BukkitRunnable() {
            long startTime = System.currentTimeMillis();
            boolean recipeItemsSpawned = false;
            boolean milestone50Sent = false;
            boolean milestone25Sent = false;
            boolean milestone5Sent = false;
            
            // Calculate thresholds based on the ritual duration
            double visualCompletionTime = Math.min(ritualDuration / 2.0, 60.0); // Either half the duration or 60 seconds, whichever is smaller
            double visualProgressThreshold = visualCompletionTime / ritualDuration; // Progress percentage when visuals complete
            
            @Override
            public void run() {
                long elapsedMs = System.currentTimeMillis() - startTime;
                long elapsedTicks = elapsedMs / 50;
                
                if (elapsedTicks >= session.durationTicks || !player.isValid()) {
                    activeRituals.remove(playerId);
                    serverRitualCooldownEnd = System.currentTimeMillis() + RITUAL_COOLDOWN_MS;
                    bossBar.removeAll();
                    
                    centerLoc.getWorld().strikeLightning(centerLoc);
                    centerLoc.getWorld().createExplosion(centerLoc, 1.5f, false, false);
                    centerLoc.getWorld().playSound(centerLoc, Sound.ENTITY_GENERIC_EXPLODE, 0.8f, 0.8f);
                    
                    
                    spawnRewardBlock(centerLoc, session.finalItem, session.itemType);
                    
                    cancel();
                    return;
                }
                
                double progress = (double) elapsedTicks / session.durationTicks;
                
                long remainingTicks = session.durationTicks - elapsedTicks;
                long remainingSeconds = remainingTicks / 20;
                long hours = remainingSeconds / 3600;
                long minutes = (remainingSeconds % 3600) / 60;
                long seconds = remainingSeconds % 60;
                String timeStr = String.format("%02d:%02d:%02d", hours, minutes, seconds);
                
                // Update boss bar with item name and time
                bossBar.setTitle("§d§lRITUAL: " + (session.finalItem.hasItemMeta() && session.finalItem.getItemMeta().hasDisplayName() 
                    ? session.finalItem.getItemMeta().getDisplayName() 
                    : session.finalItem.getType().name()) + " §7" + timeStr);
                bossBar.setProgress(Math.max(0, 1.0 - progress));
                
                location.getNearbyPlayers(100).forEach(nearby -> {
                    if (!bossBar.getPlayers().contains(nearby)) {
                        bossBar.addPlayer(nearby);
                    }
                });
                
                // Milestone messages
                if (progress >= 0.50 && !milestone50Sent) {
                    milestone50Sent = true;
                    Component milestoneMsg = miniMessage.deserialize(
                        "<gray>⚡ Ritual 50% complete at <white>" + location.getBlockX() + ", " + location.getBlockY() + ", " + location.getBlockZ()
                    );
                    Bukkit.broadcast(milestoneMsg);
                }
                
                if (progress >= 0.75 && !milestone25Sent) {
                    milestone25Sent = true;
                    Component milestoneMsg = miniMessage.deserialize(
                        "<gray>⚡ Ritual 75% complete at <white>" + location.getBlockX() + ", " + location.getBlockY() + ", " + location.getBlockZ()
                    );
                    Bukkit.broadcast(milestoneMsg);
                }
                
                if (progress >= 0.95 && !milestone5Sent) {
                    milestone5Sent = true;
                    Component milestoneMsg = miniMessage.deserialize(
                        "<gray>⚡ Ritual 95% complete at <white>" + location.getBlockX() + ", " + location.getBlockY() + ", " + location.getBlockZ()
                    );
                    Bukkit.broadcast(milestoneMsg);
                }
                
                // Spawn recipe items after visual completion (when progress reaches visualProgressThreshold)
                if (progress >= visualProgressThreshold && progress < 1.0 && !recipeItemsSpawned) {
                    recipeItemsSpawned = true;
                    double remainingProgress = 1.0 - progress;
                    long itemRemainingTicks = (long)(remainingProgress * session.durationTicks);
                    spawnRecipeItemsOnce(centerLoc, recipeItems, itemRemainingTicks);
                } else if (progress < visualProgressThreshold) {
                    recipeItemsSpawned = false;
                }
                
                executeRitualStages(centerLoc, progress, visualProgressThreshold);
            }
        }.runTaskTimer(plugin, 0, 1);
    }
    
    private void executeRitualStages(Location center, double progress, double visualProgressThreshold) {
        if (progress < visualProgressThreshold) {
            // Visual buildup stage - gradually increase all elements until visual completion time
            double visualPhase = progress / visualProgressThreshold;
            
            // Draw first ring (largest) - gradually complete as visualPhase increases
            spawnRing(center, 6.0, visualPhase);
            
            // Draw second ring (medium) - starts after 20% of visual phase
            if (visualPhase > 0.2) {
                double phase = Math.min(1.0, (visualPhase - 0.2) / 0.8);
                spawnRing(center, 3.0, phase);
            }
            
            // Draw third ring (smallest) - starts after 40% of visual phase
            if (visualPhase > 0.4) {
                double phase = Math.min(1.0, (visualPhase - 0.4) / 0.6);
                spawnRing(center, 2.0, phase);
            }
            
            // Draw rotating rectangles - starts after 60% of visual phase
            if (visualPhase > 0.6) {
                double rectPhase = Math.min(1.0, (visualPhase - 0.6) / 0.4);
                spawnRotatingRectangles(center, rectPhase);
            }
        } else if (progress < 1.0) {
            // Hold completed formation until near the end
            spawnRing(center, 6.0, 1.0);
            spawnRing(center, 3.0, 1.0);
            spawnRing(center, 2.0, 1.0);
            spawnRotatingRectangles(center, 1.0);
        } else {
            // Final collapse effect
            spawnRing(center, 6.0, 0.0);
            spawnRing(center, 3.0, 0.0);
            spawnRing(center, 2.0, 0.0);
            spawnRotatingRectangles(center, 0.0);
        }
    }
    
    private void spawnRing(Location center, double radius, double intensity) {
        // Increase particle count for better visual appearance of rings
        double expandRadius = radius * intensity;
        int points = (int) (Math.max(1, 80 * intensity)); // Increased from 40 to 80 points
        for (int i = 0; i < points; i++) {
            double angle = (i / (double) points) * Math.PI * 2;
            double x = center.getX() + expandRadius * Math.cos(angle);
            double z = center.getZ() + expandRadius * Math.sin(angle);
            
            Location loc = new Location(center.getWorld(), x, center.getY() - 1, z);
            Color color = Color.fromRGB(200, 100, 255);
            // Increased count to improve visual quality
            center.getWorld().spawnParticle(Particle.DUST, loc, 1, 0, 0, 0, 0,
                new Particle.DustOptions(color, 0.7f));
        }
    }
    
    private void spawnRotatingRectangles(Location center, double intensity) {
        long tick = System.currentTimeMillis() / 50;
        double rotation1 = (tick * 0.02) % (Math.PI * 2);
        double rotation2 = -(tick * 0.02) % (Math.PI * 2);
        
        // Draw rectangles with more particles for better visual appearance
        drawRotatingRectangle(center, 1.0, rotation1, intensity, 50); // Increased from 30 to 50 points
        drawRotatingRectangle(center, 1.0, rotation1 + Math.PI / 4, intensity, 50); // Increased from 30 to 50 points
        drawRotatingRectangle(center, 2.0, rotation1, intensity, 60); // Increased from 40 to 60 points
        drawRotatingRectangle(center, 2.0, rotation1 + Math.PI / 4, intensity, 60); // Increased from 40 to 60 points
        drawRotatingRectangle(center, 4.0, rotation2, intensity, 80); // Increased from 50 to 80 points
        drawRotatingRectangle(center, 4.0, rotation2 + Math.PI / 4, intensity, 80); // Increased from 50 to 80 points
    }
    
    private void drawRotatingRectangle(Location center, double radius, double rotation, double intensity, int maxPoints) {
        // Keep particle count reasonable but increase for better visual
        int points = (int) (maxPoints * intensity * 0.8); // Slightly reduced by 20% instead of 30% for better visuals
        for (int i = 0; i < points; i++) {
            double progress = i / (double) points;
            double x, z;
            
            if (progress < 0.25) {
                double t = progress * 4;
                x = -radius + t * radius * 2;
                z = -radius;
            } else if (progress < 0.5) {
                double t = (progress - 0.25) * 4;
                x = radius;
                z = -radius + t * radius * 2;
            } else if (progress < 0.75) {
                double t = (progress - 0.5) * 4;
                x = radius - t * radius * 2;
                z = radius;
            } else {
                double t = (progress - 0.75) * 4;
                x = -radius;
                z = radius - t * radius * 2;
            }
            
            double rotX = x * Math.cos(rotation) - z * Math.sin(rotation);
            double rotZ = x * Math.sin(rotation) + z * Math.cos(rotation);
            
            Location loc = new Location(center.getWorld(), center.getX() + rotX, center.getY() - 1, center.getZ() + rotZ);
            Color color = Color.fromRGB(220, 80, 255);
            // Keep good visual quality
            center.getWorld().spawnParticle(Particle.DUST, loc, 1, 0, 0, 0, 0,
                new Particle.DustOptions(color, 0.7f));
        }
    }
    
    private void spawnRecipeItemsOnce(Location center, ItemStack[] items, long remainingTicks) {
        if (items == null || items.length == 0) {
            return;
        }
        
        java.util.List<ItemDisplay> displaysList = new java.util.ArrayList<>();
        java.util.List<java.util.List<Location>> trailsList = new java.util.ArrayList<>();
        double speedPerTick = 1.0 / Math.max(1, remainingTicks);
        
        // Filter out null items
        java.util.List<ItemStack> validItems = new java.util.ArrayList<>();
        for (ItemStack item : items) {
            if (item != null && !item.isEmpty()) {
                validItems.add(item);
            }
        }
        
        for (int idx = 0; idx < Math.min(validItems.size(), 9); idx++) {
            ItemStack itemStack = validItems.get(idx);
            final int i = idx;
            double angle = (i / 9.0) * Math.PI * 2;
            double startX = center.getX() + 6.0 * Math.cos(angle);
            double startZ = center.getZ() + 6.0 * Math.sin(angle);
            
            Location itemLoc = new Location(center.getWorld(), startX, center.getY() - 1, startZ);
            ItemDisplay display = center.getWorld().spawn(itemLoc, ItemDisplay.class);
            display.setItemStack(itemStack.clone());
            displaysList.add(display);
            
            java.util.List<Location> trail = new java.util.ArrayList<>();
            trailsList.add(trail);
            
            new BukkitRunnable() {
                double currentPhase = 0;
                final double itemStartX = startX;
                final double itemStartZ = startZ;
                final double speed = speedPerTick;
                Location lastDisplayLoc = itemLoc.clone();
                float rotation = 0;
                boolean logged = false;
                
                @Override
                public void run() {
                    if (display.isDead()) {
                        cancel();
                        return;
                    }
                    
                    currentPhase += speed;
                    if (currentPhase > 1.0) {
                        display.remove();
                        displaysList.remove(display);
                        cancel();
                        return;
                    }
                    
                    double currentX = itemStartX + (center.getX() - itemStartX) * currentPhase;
                    double currentZ = itemStartZ + (center.getZ() - itemStartZ) * currentPhase;
                    double currentY = center.getY() - 1 + currentPhase * 3;
                    
                    Location currentDisplayLoc = new Location(center.getWorld(), currentX, currentY, currentZ);
                    
                    // Store particle trail locations but reduce density significantly for performance
                    if (currentPhase > 0.05 && currentPhase % 0.05 < 0.02) { // Only store trail every ~10 ticks
                        trail.add(currentDisplayLoc);
                    }
                    
                    rotation += 5f;
                    display.teleport(currentDisplayLoc);
                    display.setRotation(rotation % 360, 0);
                    lastDisplayLoc = currentDisplayLoc;
                }
            }.runTaskTimer(plugin, 0, 1);
        }
        
        // Continuously redraw all trails until ritual ends (with reduced frequency for performance)
        new BukkitRunnable() {
            @Override
            public void run() {
                for (java.util.List<Location> trail : trailsList) {
                    for (Location loc : trail) {
                        Color color = Color.fromRGB(200, 150, 255);
                        // Reduce particle count significantly for performance
                        // Fixed spawnParticle call to match Bukkit API
                        center.getWorld().spawnParticle(Particle.DUST, loc, 1, 0, 0, 0, 0,
                            new Particle.DustOptions(color, 0.2f)); // Lower size for performance
                    }
                }
                if (displaysList.isEmpty()) {
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 0, 10); // Update trail particles less frequently for performance
    }
    
    private void spawnRewardBlock(Location location, ItemStack finalItem, String itemType) {
         Location bottomBlockLoc = location.clone().add(0, 0, 0);
         Location topBlockLoc = location.clone().add(0, 1, 0);
         
         bottomBlockLoc.getBlock().setType(Material.BARRIER);
         topBlockLoc.getBlock().setType(Material.BARRIER);
         
         String blockKey = bottomBlockLoc.getBlockX() + "," + bottomBlockLoc.getBlockY() + "," + bottomBlockLoc.getBlockZ();
         String topBlockKey = topBlockLoc.getBlockX() + "," + topBlockLoc.getBlockY() + "," + topBlockLoc.getBlockZ();
         rewardBlocks.put(blockKey, true);
         rewardBlocks.put(topBlockKey, true);
         rewardItems.put(blockKey, finalItem);
         rewardItems.put(topBlockKey, finalItem);  // Both blocks should have the same item
         rewardItemTypes.put(blockKey, itemType);
         rewardItemTypes.put(topBlockKey, itemType); // Both blocks should have the same item type
         // Track the pair: if either is clicked, use this key to remove both
         rewardBlockPairs.put(blockKey, blockKey);
         rewardBlockPairs.put(topBlockKey, blockKey);
         
         Location displayLoc = topBlockLoc.clone().add(0, 0.0, 0);
         ItemDisplay display = location.getWorld().spawn(displayLoc, ItemDisplay.class);
         display.setItemStack(finalItem);
         display.setRotation(0, 0);
         
         rewardDisplays.put(blockKey, display);
        
        // Create a visual effect to draw attention to the reward
        World world = location.getWorld();
        world.spawnParticle(Particle.END_ROD, location, 50, 0.5, 0.5, 0.5, 0.2);
        world.playSound(location, Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
        
        new BukkitRunnable() {
            float rotation = 0;
            
            @Override
            public void run() {
                if (display.isDead() || (location.getBlock().getType() != Material.BARRIER && topBlockLoc.getBlock().getType() != Material.BARRIER)) {
                    rewardBlocks.remove(blockKey);
                    rewardBlocks.remove(topBlockKey);
                    rewardItems.remove(blockKey);
                    rewardItems.remove(topBlockKey);
                    rewardItemTypes.remove(blockKey);
                    rewardItemTypes.remove(topBlockKey);
                    rewardDisplays.remove(blockKey);
                    cancel();
                    return;
                }
                
                rotation += 5f;
                display.setRotation(rotation % 360, 0);
            }
        }.runTaskTimer(plugin, 0, 1);
    }
    
    /**
     * Get the server-wide ritual cooldown end time
     */
    /**
     * Get the server-wide ritual cooldown end time
     */
    public long getServerRitualCooldownEnd() {
        return serverRitualCooldownEnd;
    }
    
    private static class RitualSession {
        UUID playerId;
        Location location;
        int durationTicks;
        ItemStack finalItem;
        String itemType;
        
        RitualSession(UUID playerId, Location location, int durationTicks) {
            this.playerId = playerId;
            this.location = location;
            this.durationTicks = durationTicks;
        }
    }
}