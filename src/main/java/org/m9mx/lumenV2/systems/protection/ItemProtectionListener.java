package org.m9mx.lumenV2.systems.protection;

import org.bukkit.Material;
import org.bukkit.Location;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.*;
import org.bukkit.event.entity.ItemDespawnEvent;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.entity.Item;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.m9mx.lumenV2.util.ItemDataHelper;
import org.m9mx.lumenV2.util.ItemUtils;
import org.m9mx.lumenV2.systems.protection.tracking.ItemProtectionTracker;
import org.m9mx.lumenV2.systems.ItemProtection;

/**
 * Listens to inventory and block interaction events to enforce item protection
 */
public class ItemProtectionListener implements Listener {

    private Plugin plugin;
    private ItemProtection itemProtectionSystem;
    private ItemProtectionTracker tracker;

    public ItemProtectionListener(Plugin plugin, ItemProtection itemProtectionSystem) {
        this.plugin = plugin;
        this.itemProtectionSystem = itemProtectionSystem;
        this.tracker = new ItemProtectionTracker(plugin);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!itemProtectionSystem.isEnabled()) {
            return;
        }

        // Skip bundle GUI entirely - let BundleUtil handle it
        boolean isBundleGUI = event.getInventory().getSize() == 27 
                && event.getView().getTitle() != null 
                && event.getView().getTitle().contains("Enhanced Bundle");
        
        if (isBundleGUI) {
            return;
        }

        ItemStack cursor = event.getCursor();
        ItemStack clicked = event.getCurrentItem();

        // Check cursor item - only block if it's a protected item
        if (cursor != null && !cursor.isEmpty() && ItemUtils.isProtected(cursor)) {
            ItemProtectionMode mode = ItemUtils.getProtectionMode(cursor);
            if (mode == ItemProtectionMode.SIMPLE || mode == ItemProtectionMode.STRICT) {
                // Block in blocked inventories (enderchest, shulker)
                if (isBlockedInventory(event)) {
                    event.setCancelled(true);
                    if (event.getWhoClicked() instanceof Player) {
                        ((Player) event.getWhoClicked()).sendMessage("§cYou cannot place protected items here!");
                    }
                    return;
                }
            }
        }

        // Check clicked item - only block if it's a protected item
        if (clicked != null && !clicked.isEmpty() && ItemUtils.isProtected(clicked)) {
            ItemProtectionMode mode = ItemUtils.getProtectionMode(clicked);
            if (mode == ItemProtectionMode.SIMPLE || mode == ItemProtectionMode.STRICT) {
                if (isBlockedInventory(event)) {
                    event.setCancelled(true);
                    if (event.getWhoClicked() instanceof Player) {
                        ((Player) event.getWhoClicked())
                                .sendMessage("§cYou cannot interact with protected items in blocked containers!");
                    }
                    return;
                }
            }
        }

        // Track for STRICT mode - when item is moved to another container
        if (clicked != null && !clicked.isEmpty() && ItemUtils.isCustomItem(clicked)) {
            ItemProtectionMode mode = ItemUtils.getProtectionMode(clicked);
            if (mode == ItemProtectionMode.STRICT && event.getWhoClicked() instanceof Player) {
                Player player = (Player) event.getWhoClicked();
                // Track when item is placed in regular containers (chests, etc.)
                if (isRegularContainer(event.getInventory().getType())) {
                    // If the inventory holder is a block state, we can get the location
                    if (event.getInventory().getHolder() instanceof BlockState) {
                        BlockState blockState = (BlockState) event.getInventory().getHolder();
                        Location loc = blockState.getLocation();
                        tracker.trackItemInContainer(clicked, loc, event.getInventory().getType().toString());
                    } else {
                        // Otherwise just track with the player
                        tracker.trackItemLocation(clicked, player);
                    }
                }
            }
        }

        // Track for STRICT mode - when cursor item is moved
        if (cursor != null && !cursor.isEmpty() && ItemUtils.isCustomItem(cursor)) {
            ItemProtectionMode mode = ItemUtils.getProtectionMode(cursor);
            if (mode == ItemProtectionMode.STRICT && event.getWhoClicked() instanceof Player) {
                Player player = (Player) event.getWhoClicked();
                // Track when item is placed in regular containers (chests, etc.)
                if (isRegularContainer(event.getInventory().getType())) {
                    // If the inventory holder is a block state, we can get the location
                    if (event.getInventory().getHolder() instanceof BlockState) {
                        BlockState blockState = (BlockState) event.getInventory().getHolder();
                        Location loc = blockState.getLocation();
                        tracker.trackItemInContainer(cursor, loc, event.getInventory().getType().toString());
                    } else {
                        // Otherwise just track with the player
                        tracker.trackItemLocation(cursor, player);
                    }
                }
            }
        }
    }

    @EventHandler
    public void onInventoryMoveItem(InventoryMoveItemEvent event) {
        if (!itemProtectionSystem.isEnabled()) {
            return;
        }

        ItemStack item = event.getItem();

        if (item != null && !item.isEmpty() && ItemUtils.isStrictlyProtected(item)) {
            // Track when protected items are moved between inventories
            // We'll log the source and destination locations
            InventoryHolder destination = event.getDestination().getHolder();
            if (destination instanceof Player) {
                Player player = (Player) destination;
                tracker.trackItemLocation(item, player);
            } else if (destination instanceof org.bukkit.block.BlockState) {
                // For container inventories like chests, track the location
                BlockState blockState = (BlockState) destination;
                Location loc = blockState.getLocation();
                tracker.trackItemInContainer(item, loc, event.getDestination().getType().toString());
            }
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        ItemStack item = event.getItem();

        if (item == null || item.isEmpty() || !ItemUtils.isCustomItem(item)) {
            return;
        }

        ItemProtectionMode mode = ItemUtils.getProtectionMode(item);
        if (mode == ItemProtectionMode.NONE) {
            return;
        }

        // Track for STRICT mode
        if (mode == ItemProtectionMode.STRICT) {
            tracker.trackItemLocation(item, event.getPlayer());
        }
    }

    @EventHandler
    public void onInventoryOpen(InventoryOpenEvent event) {
        InventoryType type = event.getInventory().getType();

        // Check for protected items in blocked containers (STRICT mode only - tracking)
        if (event.getPlayer() instanceof Player) {
            Player player = (Player) event.getPlayer();
            for (ItemStack item : event.getInventory().getContents()) {
                if (item != null && !item.isEmpty() && ItemUtils.isCustomItem(item)) {
                    ItemProtectionMode mode = ItemUtils.getProtectionMode(item);
                    if (mode == ItemProtectionMode.STRICT) {
                        // If the inventory holder is a block state, track it as in container
                        if (event.getInventory().getHolder() instanceof BlockState) {
                            BlockState blockState = (BlockState) event.getInventory().getHolder();
                            Location loc = blockState.getLocation();
                            tracker.trackItemInContainer(item, loc, event.getInventory().getType().toString());
                        } else {
                            tracker.trackItemLocation(item, player);
                        }
                    }
                }
            }
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        // When player closes inventory, track any strict items still in it
        if (event.getPlayer() instanceof Player) {
            Player player = (Player) event.getPlayer();
            for (ItemStack item : event.getInventory().getContents()) {
                if (item != null && !item.isEmpty() && ItemUtils.isCustomItem(item)) {
                    ItemProtectionMode mode = ItemUtils.getProtectionMode(item);
                    if (mode == ItemProtectionMode.STRICT) {
                        // If the inventory holder is a block state, track it as in container
                        if (event.getInventory().getHolder() instanceof BlockState) {
                            BlockState blockState = (BlockState) event.getInventory().getHolder();
                            Location loc = blockState.getLocation();
                            tracker.trackItemInContainer(item, loc, event.getInventory().getType().toString());
                        } else {
                            tracker.trackItemLocation(item, player);
                        }
                    }
                }
            }
        }
    }

    @EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        if (!itemProtectionSystem.isEnabled()) {
            return;
        }

        ItemStack droppedItem = event.getItemDrop().getItemStack();

        if (droppedItem != null && !droppedItem.isEmpty() && ItemUtils.isStrictlyProtected(droppedItem)) {
            tracker.trackItemLocation(droppedItem, event.getPlayer());
        }
    }

    @EventHandler
    public void onItemDespawn(ItemDespawnEvent event) {
        if (!itemProtectionSystem.isEnabled()) {
            return;
        }

        ItemStack despawnedItem = event.getEntity().getItemStack();

        if (despawnedItem != null && !despawnedItem.isEmpty() && ItemUtils.isStrictlyProtected(despawnedItem)) {
            if (itemProtectionSystem.isLoggingEnabled()) {
                plugin.getLogger().warning("STRICT protected item despawned at: "
                        + event.getEntity().getLocation().getWorld().getName() + " "
                        + event.getEntity().getLocation().getBlockX() + ", "
                        + event.getEntity().getLocation().getBlockY() + ", "
                        + event.getEntity().getLocation().getBlockZ());
            }

            // Update tracker to show item despawned
            tracker.trackItemDespawn(despawnedItem, event.getEntity().getLocation());
        }
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (!itemProtectionSystem.isEnabled()) {
            return;
        }

        if (!(event.getEntity() instanceof Item)) {
            return;
        }

        Item itemEntity = (Item) event.getEntity();
        ItemStack item = itemEntity.getItemStack();

        if (item != null && !item.isEmpty() && ItemUtils.isStrictlyProtected(item)) {
            // Check if damage is from fire/lava
            if (event.getCause() == EntityDamageEvent.DamageCause.FIRE
                    || event.getCause() == EntityDamageEvent.DamageCause.LAVA
                    || event.getCause() == EntityDamageEvent.DamageCause.FIRE_TICK) {

                if (itemProtectionSystem.isLoggingEnabled()) {
                    plugin.getLogger().warning("STRICT protected item burned at: "
                            + itemEntity.getLocation().getWorld().getName() + " "
                            + itemEntity.getLocation().getBlockX() + ", "
                            + itemEntity.getLocation().getBlockY() + ", "
                            + itemEntity.getLocation().getBlockZ());
                }

                tracker.trackItemBurned(item, itemEntity.getLocation(), event.getCause().toString());
            }
        }
    }

    @EventHandler
    public void onEntityExplode(EntityExplodeEvent event) {
        if (!itemProtectionSystem.isEnabled()) {
            return;
        }

        // Check all items in the explosion area
        for (org.bukkit.entity.Entity entity : event.getEntity().getNearbyEntities(5, 5, 5)) {
            if (!(entity instanceof Item)) {
                continue;
            }

            Item itemEntity = (Item) entity;
            ItemStack item = itemEntity.getItemStack();

            if (item != null && !item.isEmpty() && ItemUtils.isStrictlyProtected(item)) {
                if (itemProtectionSystem.isLoggingEnabled()) {
                    plugin.getLogger().warning("STRICT protected item exploded at: "
                            + itemEntity.getLocation().getWorld().getName() + " "
                            + itemEntity.getLocation().getBlockX() + ", "
                            + itemEntity.getLocation().getBlockY() + ", "
                            + itemEntity.getLocation().getBlockZ());
                }

                tracker.trackItemExploded(item, itemEntity.getLocation());
            }
        }
    }

    /**
     * Check if an inventory is blocked for protected items
     */
    private boolean isBlockedInventory(InventoryClickEvent event) {
        InventoryType type = event.getInventory().getType();
        
        // Check standard blocked inventory types
        return type == InventoryType.ENDER_CHEST || type == InventoryType.SHULKER_BOX;
    }

    /**
     * Check if an inventory type is a regular container where items should be
     * tracked
     */
    private boolean isRegularContainer(InventoryType type) {
        return type == InventoryType.CHEST
                || type == InventoryType.DISPENSER
                || type == InventoryType.DROPPER
                || type == InventoryType.HOPPER
                || type == InventoryType.BARREL
                || type == InventoryType.SHULKER_BOX;
    }
}
