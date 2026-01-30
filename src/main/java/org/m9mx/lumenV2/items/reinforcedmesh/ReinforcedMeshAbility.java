package org.m9mx.lumenV2.items.reinforcedmesh;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.logging.Logger;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityPlaceEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.m9mx.lumenV2.item.ItemRegistry;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;

public class ReinforcedMeshAbility implements Listener {
    private final JavaPlugin plugin;
    private final Set<EntityType> BLACKLISTED_ENTITIES;
    private final Random random = new Random();
    private final Map<String, Long> captureTimestamps = new HashMap<>(); // Player UUID -> last capture time
    private static final Logger LOGGER = Logger.getLogger("ReinforcedMeshAbility");

    public ReinforcedMeshAbility(JavaPlugin plugin) {
        this.plugin = plugin;
        // Initialize the blacklist with entities that shouldn't be captured
        this.BLACKLISTED_ENTITIES = new HashSet<>(Arrays.asList(
            // Players
            EntityType.PLAYER,
            
            // Boss Mobs
            EntityType.ENDER_DRAGON,
            EntityType.WITHER,
            EntityType.ELDER_GUARDIAN,
            
            // Large/Dangerous Mobs
            EntityType.GUARDIAN,
            EntityType.SHULKER,
            EntityType.ENDERMAN,
            
            // Display/Decoration Entities
            EntityType.ARMOR_STAND,
            EntityType.ITEM_FRAME,
            EntityType.GLOW_ITEM_FRAME,
            EntityType.PAINTING,
            EntityType.BLOCK_DISPLAY,
            EntityType.ITEM_DISPLAY,
            EntityType.TEXT_DISPLAY,
            EntityType.INTERACTION,
            EntityType.MARKER,
            EntityType.LEASH_KNOT,
            
            // Boats
            EntityType.ACACIA_BOAT,
            EntityType.BIRCH_BOAT,
            EntityType.DARK_OAK_BOAT,
            EntityType.JUNGLE_BOAT,
            EntityType.BAMBOO_RAFT,
            EntityType.OAK_BOAT,
            EntityType.SPRUCE_BOAT,
            EntityType.CHERRY_BOAT,
            EntityType.MANGROVE_BOAT,
            EntityType.PALE_OAK_BOAT,
            
            // Chest Boats
            EntityType.ACACIA_CHEST_BOAT,
            EntityType.BIRCH_CHEST_BOAT,
            EntityType.DARK_OAK_CHEST_BOAT,
            EntityType.JUNGLE_CHEST_BOAT,
            EntityType.OAK_CHEST_BOAT,
            EntityType.SPRUCE_CHEST_BOAT,
            EntityType.BAMBOO_CHEST_RAFT,
            EntityType.CHERRY_CHEST_BOAT,
            EntityType.MANGROVE_CHEST_BOAT,
            EntityType.PALE_OAK_CHEST_BOAT,
            
            // Minecarts
            EntityType.MINECART,
            EntityType.COMMAND_BLOCK_MINECART,
            EntityType.TNT_MINECART,
            EntityType.HOPPER_MINECART,
            EntityType.CHEST_MINECART,
            EntityType.FURNACE_MINECART,
            EntityType.SPAWNER_MINECART,
            
            // Projectiles/Throwables
            EntityType.ARROW,
            EntityType.SNOWBALL,
            EntityType.EGG,
            EntityType.FIREWORK_ROCKET,
            EntityType.TRIDENT,
            EntityType.SPECTRAL_ARROW,
            EntityType.ENDER_PEARL,
            EntityType.DRAGON_FIREBALL,
            EntityType.FIREBALL,
            EntityType.SMALL_FIREBALL,
            EntityType.SHULKER_BULLET,
            EntityType.LLAMA_SPIT,
            EntityType.BREEZE_WIND_CHARGE,
            EntityType.WIND_CHARGE,
            
            // Raid Mobs & Dangerous Mobs
            EntityType.EVOKER,
            EntityType.VINDICATOR,
            EntityType.PILLAGER,
            EntityType.RAVAGER,
            EntityType.PIGLIN_BRUTE,
            EntityType.WITHER_SKELETON,
            EntityType.WARDEN,
            
            // New 1.21 Mobs
            EntityType.BREEZE,
            EntityType.BOGGED,
            EntityType.CREAKING,
            
            // Other Dangerous/Blocked Entities
            EntityType.TNT,
            EntityType.END_CRYSTAL,
            EntityType.AREA_EFFECT_CLOUD,
            EntityType.EVOKER_FANGS,
            EntityType.WITHER_SKULL,
            EntityType.EXPERIENCE_ORB,
            EntityType.LIGHTNING_BOLT,
            EntityType.ILLUSIONER,
            EntityType.FALLING_BLOCK,
            EntityType.EXPERIENCE_BOTTLE,
            EntityType.EYE_OF_ENDER,
            EntityType.LINGERING_POTION,
            EntityType.OMINOUS_ITEM_SPAWNER
        ));
    }

    /**
     * Handles right-clicking on entities to capture them
     */
    @EventHandler
    public void onEntityInteract(PlayerInteractEntityEvent event) {
        if (event.getRightClicked() instanceof Player) {
            // Don't allow capturing players
            return;
        }

        ItemStack item = event.getPlayer().getInventory().getItemInMainHand();

        // Check if player is holding the Reinforced Mesh
        if (!ReinforcedMesh.isReinforcedMesh(item)) {
            item = event.getPlayer().getInventory().getItemInOffHand();
            if (!ReinforcedMesh.isReinforcedMesh(item)) {
                return;
            }
        }

        // Get the registered ReinforcedMesh item to check if it's enabled
        ReinforcedMesh meshItem = (ReinforcedMesh) ItemRegistry.getItemStatic("reinforced_mesh");
        if (meshItem == null || !meshItem.isEnabled()) {
            // Cancel the event if the item is not registered or is disabled
            event.setCancelled(true);
            return;
        }

        // Ensure the interaction is with the main hand
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }

        Entity clickedEntity = event.getRightClicked();

        // Check if the entity is blacklisted
        if (BLACKLISTED_ENTITIES.contains(clickedEntity.getType())) {
            event.getPlayer().sendMessage("This entity cannot be captured by the Reinforced Mesh!");
            event.setCancelled(true);
            return;
        }

        if (!(clickedEntity instanceof LivingEntity)) {
            // Don't capture non-living entities
            event.getPlayer().sendMessage("Only living entities can be captured!");
            event.setCancelled(true);
            return;
        }



        // Don't capture if the mesh already has an entity
        if (ReinforcedMesh.hasCapturedEntity(item)) {
            event.getPlayer().sendMessage("This Reinforced Mesh already contains a captured entity!");
            event.setCancelled(true);
            return;
        }

        // Capture the entity with a slight delay
        Player player = event.getPlayer();
        Entity entity = clickedEntity;
        plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, () -> {
            if (captureEntity(player, entity)) {
                // Item will be reset by the API
            }
        }, 10L); // 0.5 seconds (10 ticks)
    }

    /**
     * Handles right-clicking on blocks to release captured entities
     */
    @EventHandler
    public void onBlockInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK && event.getAction() != Action.RIGHT_CLICK_AIR) {
            return;
        }

        ItemStack item = event.getPlayer().getInventory().getItemInMainHand();

        // Check if player is holding the Reinforced Mesh
        if (!ReinforcedMesh.isReinforcedMesh(item)) {
            item = event.getPlayer().getInventory().getItemInOffHand();
            if (!ReinforcedMesh.isReinforcedMesh(item)) {
                return;
            }
        }

        // Get the registered ReinforcedMesh item to check if it's enabled
        ReinforcedMesh meshItem = (ReinforcedMesh) ItemRegistry.getItemStatic("reinforced_mesh");
        if (meshItem == null || !meshItem.isEnabled()) {
            // Cancel the event if the item is not registered or is disabled
            event.setCancelled(true);
            return;
        }

        // Prevent placing armor stands with the Reinforced Mesh
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            Block clickedBlock = event.getClickedBlock();
            if (clickedBlock != null && event.getHand() == EquipmentSlot.HAND) {
                // Cancel the event to prevent armor stand placement
                event.setCancelled(true);

                // Release the captured entity if there is one
                if (ReinforcedMesh.hasCapturedEntity(item)) {
                    releaseEntity(event.getPlayer(), item, clickedBlock.getLocation());
                } else {
                    event.getPlayer().sendMessage("This Reinforced Mesh is empty!");
                }
            }
        }
    }

    /**
     * Prevents armor stand placement when using ARMOR_STAND material
     */
    @EventHandler
    public void onArmorStandPlace(EntityPlaceEvent event) {
        if (!(event.getEntity() instanceof ArmorStand)) {
            return;
        }

        Player player = event.getPlayer();
        if (player == null) {
            return;
        }

        ItemStack item = player.getInventory().getItemInMainHand();
        if (ReinforcedMesh.isReinforcedMesh(item)) {
            // Check if the ReinforcedMesh item is enabled
            ReinforcedMesh meshItem = (ReinforcedMesh) ItemRegistry.getItemStatic("reinforced_mesh");
            if (meshItem == null || !meshItem.isEnabled()) {
                return; // Don't cancel if the item is disabled
            }
            
            // Cancel placing armor stands with the Reinforced Mesh
            event.setCancelled(true);
        } else {
            item = player.getInventory().getItemInOffHand();
            if (ReinforcedMesh.isReinforcedMesh(item)) {
                // Check if the ReinforcedMesh item is enabled
                ReinforcedMesh meshItem = (ReinforcedMesh) ItemRegistry.getItemStatic("reinforced_mesh");
                if (meshItem == null || !meshItem.isEnabled()) {
                    return; // Don't cancel if the item is disabled
                }
                
                event.setCancelled(true);
            }
        }
    }

    /**
     * Captures an entity into the Reinforced Mesh
     */
    private boolean captureEntity(Player player, Entity entityToCapture) {
        LOGGER.info("[ReinforcedMeshAbility] Starting capture for player: " + player.getName() + ", entity: " + entityToCapture.getType());
        
        // Double-check if the entity is blacklisted (redundant but safe)
        if (BLACKLISTED_ENTITIES.contains(entityToCapture.getType())) {
            LOGGER.warning("[ReinforcedMeshAbility] Entity is blacklisted: " + entityToCapture.getType());
            return false;
        }

        // Anti-dupe: Check cooldown (prevent spam capture)
        String playerUUID = player.getUniqueId().toString();
        long currentTime = System.currentTimeMillis();
        long lastCaptureTime = captureTimestamps.getOrDefault(playerUUID, 0L);
        
        if (currentTime - lastCaptureTime < 500) { // 500ms cooldown
            LOGGER.warning("[ReinforcedMeshAbility] Capture cooldown active for player: " + player.getName());
            return false;
        }
        
        LOGGER.info("[ReinforcedMeshAbility] Cooldown passed, proceeding with capture");
        captureTimestamps.put(playerUUID, currentTime);

        // Get the entity type
        EntityType entityType = entityToCapture.getType();
        
        // Create new mesh with captured entity
        LOGGER.info("[ReinforcedMeshAbility] Creating new mesh item...");
        ItemStack newMesh = ReinforcedMesh.getReinforcedMeshItem();
        
        // Set captured entity
        LOGGER.info("[ReinforcedMeshAbility] Setting captured entity type: " + entityType);
        ReinforcedMesh.setCapturedEntity(newMesh, entityType);
        
        // Store entity NBT data
        LOGGER.info("[ReinforcedMeshAbility] Storing NBT data for entity...");
        ReinforcedMesh.storeEntityNBT(newMesh, entityToCapture);
        
        // Generate random custom model data
        int customModelData = random.nextInt(Integer.MAX_VALUE);
        LOGGER.info("[ReinforcedMeshAbility] Generated custom model data: " + customModelData);
        setCustomModelData(newMesh, customModelData);
        
        // Update lore to show captured entity
        LOGGER.info("[ReinforcedMeshAbility] Updating lore...");
        updateMeshLore(newMesh, entityType);
        
        // Remove the entity from the world
        LOGGER.info("[ReinforcedMeshAbility] Removing entity from world...");
        entityToCapture.remove();
        
        // Remove one from hand and add the new mesh
        LOGGER.info("[ReinforcedMeshAbility] Updating player inventory...");
        ItemStack handItem = player.getInventory().getItemInMainHand();
        if (ReinforcedMesh.isReinforcedMesh(handItem)) {
            if (handItem.getAmount() > 1) {
                LOGGER.info("[ReinforcedMeshAbility] Reducing stack amount from " + handItem.getAmount() + " to " + (handItem.getAmount() - 1));
                handItem.setAmount(handItem.getAmount() - 1);
            } else {
                LOGGER.info("[ReinforcedMeshAbility] Replacing main hand item with new mesh");
                player.getInventory().setItemInMainHand(newMesh);
                player.sendMessage("Successfully captured " + entityType.getName() + " in the Reinforced Mesh!");
                LOGGER.info("[ReinforcedMeshAbility] Capture complete!");
                return true;
            }
        }
        
        // Add new mesh to inventory
        LOGGER.info("[ReinforcedMeshAbility] Adding new mesh to player inventory");
        player.getInventory().addItem(newMesh);
        player.sendMessage("Successfully captured " + entityType.getName() + " in the Reinforced Mesh!");
        LOGGER.info("[ReinforcedMeshAbility] Capture complete!");

        return true;
    }

    /**
     * Releases the captured entity from the Reinforced Mesh
     */
    private void releaseEntity(Player player, ItemStack mesh, Location location) {
        LOGGER.info("[ReinforcedMeshAbility] Starting release for player: " + player.getName());
        
        // Get the captured entity type
        EntityType entityType = ReinforcedMesh.getCapturedEntity(mesh);
        if (entityType == null) {
            LOGGER.warning("[ReinforcedMeshAbility] Mesh is empty, nothing to release");
            player.sendMessage("This Reinforced Mesh is empty!");
            return;
        }

        LOGGER.info("[ReinforcedMeshAbility] Releasing entity: " + entityType);

        // Check if location has a solid block and 2 air blocks above
        if (!isValidReleaseLocation(location)) {
            LOGGER.warning("[ReinforcedMeshAbility] Invalid release location at " + location);
            player.sendMessage("§cNeed a solid block with 2 air blocks above to release!");
            return;
        }

        LOGGER.info("[ReinforcedMeshAbility] Location is valid, spawning entity...");

        // Adjust spawn location to center on block face and up 2 blocks
        Location spawnLocation = location.clone().add(0.5, 2, 0.5);
        
        // Spawn the entity at the location (EMPTY - will be filled by NBT only)
        LOGGER.info("[ReinforcedMeshAbility] Spawning entity at: " + spawnLocation);
        LOGGER.info("[ReinforcedMeshAbility] Using NBT ONLY for entity restoration - no other data sources");
        Entity spawnedEntity = location.getWorld().spawnEntity(spawnLocation, entityType);
        
        // Restore entity NBT data (ONLY method - all properties come from NBT)
        LOGGER.info("[ReinforcedMeshAbility] Retrieving stored NBT data...");
        String nbtData = ReinforcedMesh.getStoredEntityNBT(mesh);
        LOGGER.info("[ReinforcedMeshAbility] NBT data present: " + (nbtData != null && !nbtData.isEmpty()));
        
        if (nbtData != null && !nbtData.isEmpty()) {
            LOGGER.info("[ReinforcedMeshAbility] Applying NBT data (ONLY restoration method)...");
            ReinforcedMesh.applyEntityNBT(spawnedEntity, nbtData);
            LOGGER.info("[ReinforcedMeshAbility] Entity fully restored from NBT - all properties loaded");
        } else {
            LOGGER.warning("[ReinforcedMeshAbility] No NBT data available - entity spawned with defaults");
        }

        // Reset the mesh item to empty/stackable version using ItemRegistry
        LOGGER.info("[ReinforcedMeshAbility] Creating reset mesh item...");
        ItemStack resetMesh = ReinforcedMesh.getReinforcedMeshItem();
        
        // Replace the current mesh in inventory with reset version
        LOGGER.info("[ReinforcedMeshAbility] Replacing mesh in inventory with empty version...");
        if (Objects.equals(player.getInventory().getItemInMainHand(), mesh)) {
            LOGGER.info("[ReinforcedMeshAbility] Replacing main hand mesh");
            player.getInventory().setItemInMainHand(resetMesh);
        } else if (Objects.equals(player.getInventory().getItemInOffHand(), mesh)) {
            LOGGER.info("[ReinforcedMeshAbility] Replacing off hand mesh");
            player.getInventory().setItemInOffHand(resetMesh);
        }

        player.sendMessage("Successfully released " + entityType.getName() + " from the Reinforced Mesh!");
        LOGGER.info("[ReinforcedMeshAbility] Release complete!");
    }
    
    /**
     * Check if location is valid for entity release
     * Must have a solid block clicked and 2 air blocks above
     */
    private boolean isValidReleaseLocation(Location location) {
        if (location == null || location.getBlock() == null) return false;
        
        // Check if clicked block is solid
        if (location.getBlock().isEmpty()) return false;
        
        // Check if there are 2 air blocks above
        Block airBlock1 = location.clone().add(0, 1, 0).getBlock();
        Block airBlock2 = location.clone().add(0, 2, 0).getBlock();
        
        return airBlock1.isEmpty() && airBlock2.isEmpty();
    }
    
    /**
     * Set custom model data on the mesh
     */
    private void setCustomModelData(ItemStack item, int customModelData) {
        if (!item.hasItemMeta()) return;
        ItemMeta meta = item.getItemMeta();
        meta.setCustomModelData(customModelData);
        item.setItemMeta(meta);
    }
    
    /**
     * Update the lore to show the captured entity (or empty if null)
     */
    private void updateMeshLore(ItemStack item, EntityType entityType) {
        if (!item.hasItemMeta()) return;
        ItemMeta meta = item.getItemMeta();
        List<Component> lore = new ArrayList<>();
        
        MiniMessage miniMessage = MiniMessage.miniMessage();
        
        // Base lore
        lore.add(miniMessage.deserialize("<gray>Utility Tool"));
        lore.add(miniMessage.deserialize(" "));
        lore.add(miniMessage.deserialize("<gray>A heavy-duty, multi-layered weave"));
        lore.add(miniMessage.deserialize("<gray>designed to stabilize and compress"));
        lore.add(miniMessage.deserialize("<gray>biological matter for transport."));
        lore.add(miniMessage.deserialize(" "));
        
        // Captured entity info
        if (entityType != null) {
            lore.add(miniMessage.deserialize("<gold>Contains: <yellow>" + entityType.getName()));
        } else {
            lore.add(miniMessage.deserialize("<gray>Empty"));
        }
        
        lore.add(miniMessage.deserialize(" "));
        lore.add(miniMessage.deserialize("<yellow>● <reset><gray>Right-click mob to capture."));
        lore.add(miniMessage.deserialize("<yellow>● <reset><gray>Right-click ground to release."));
        lore.add(miniMessage.deserialize(" "));
        lore.add(miniMessage.deserialize("<dark_gray>[Max Stack: 16]"));
        
        meta.lore(lore);
        item.setItemMeta(meta);
    }
}