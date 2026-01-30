package org.m9mx.lumenV2.systems.enhancement;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.m9mx.lumenV2.item.ItemRegistry;
import org.m9mx.lumenV2.systems.EnhancementSystem;
import org.m9mx.lumenV2.util.ItemUtils;

import io.papermc.paper.datacomponent.DataComponentTypes;

/**
 * Listens to player interactions for enhancement GUI
 */
public class EnhancementListener implements Listener {

    private final EnhancementSystem enhancementSystem;

    public EnhancementListener(org.bukkit.plugin.Plugin plugin, EnhancementSystem enhancementSystem) {
        this.enhancementSystem = enhancementSystem;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (!enhancementSystem.isEnabled()) {
            return;
        }

        // Check for shift+right-click
        if (!event.getPlayer().isSneaking()
                || event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        ItemStack item = event.getItem();
        if (item == null || item.isEmpty() || !ItemUtils.isCustomItem(item)) {
            return;
        }

        // Get the item ID from the item
        String itemId = ItemUtils.getItemId(item);
        if (itemId == null || !org.m9mx.lumenV2.util.ItemDataHelper.isEnhancable(itemId)) {
            return;
        }

        event.setCancelled(true);

        // Open the enhancement GUI
        openEnhancementGUI(event.getPlayer(), item);
    }

    /**
     * Open the enhancement GUI for a player with an item
     */
    private void openEnhancementGUI(Player player, ItemStack enhancableItem) {
        Inventory gui = EnhancementGUI.createGUI(enhancableItem);
        player.openInventory(gui);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!enhancementSystem.isEnabled()) {
            return;
        }

        // Check if this is an enhancement GUI
        if (!isEnhancementGUI(event.getInventory())) {
            return;
        }

        int slot = event.getRawSlot();
        ItemStack cursor = event.getCursor();

        // Allow player inventory slots (27+) to work normally
        if (slot >= 27) {
            return;
        }

        // Block all GUI slots except the catalyst slot (13)
        if (slot != EnhancementGUI.getCatalystSlot()) {
            event.setCancelled(true);
            return;
        }

        // Catalyst slot (13) - allow placing catalyst shards only
        if (slot == EnhancementGUI.getCatalystSlot()) {
            if (cursor != null && !cursor.isEmpty()) {
                // Check if it's a catalyst shard
                if (isCatalystShard(cursor)) {
                    event.setCancelled(false);
                } else {
                    event.setCancelled(true);
                }
            }
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!enhancementSystem.isEnabled()) {
            return;
        }

        // Check if this is an enhancement GUI
        if (!isEnhancementGUI(event.getInventory())) {
            return;
        }

        Inventory gui = event.getInventory();
        ItemStack catalystSlotItem = gui.getItem(EnhancementGUI.getCatalystSlot());

        // Count shards in the catalyst slot
        int shardCount = 0;
        if (catalystSlotItem != null && !catalystSlotItem.isEmpty() && isCatalystShard(catalystSlotItem)) {
            shardCount = catalystSlotItem.getAmount();
        }

        // The enhancable item is in the player's hand
        Player player = (Player) event.getPlayer();
        ItemStack itemInHand = player.getInventory().getItemInMainHand();

        if (itemInHand != null && !itemInHand.isEmpty() && ItemUtils.isCustomItem(itemInHand)) {
            // Get the maximum allowed shards
            int maxShards = enhancementSystem.getMaxShards();
            
            // Check if there are excess shards
            if (shardCount > maxShards) {
                // Calculate excess shards
                int excessShards = shardCount - maxShards;
                
                // Create excess shards item to return to player
                ItemStack excessShardsItem = EnhancementGUI.getCatalystShard();
                excessShardsItem.setAmount(excessShards);
                
                // Try to give excess shards to player
                if (player.getInventory().addItem(excessShardsItem).isEmpty()) {
                    // Successfully added all excess shards to inventory
                    player.sendMessage("§aReturned " + excessShards + " excess catalyst shards to your inventory.");
                } else {
                    // Inventory full, drop items at player location
                    player.getWorld().dropItem(player.getLocation(), excessShardsItem);
                    player.sendMessage("§cYour inventory was full! " + excessShards + " excess catalyst shards were dropped on the ground.");
                }
                
                // Save only the maximum allowed shards to the item
                EnhancementManager.setShardCount(itemInHand, maxShards);
            } else {
                // Save the shard count to the item's NBT data (within limit)
                EnhancementManager.setShardCount(itemInHand, shardCount);
            }
            
            player.getInventory().setItemInMainHand(itemInHand);
        }
    }

    /**
     * Check if an item is a catalyst shard
     */
    private boolean isCatalystShard(ItemStack item) {
        // Check if the item is the catalyst shard from our registry
        if (item.hasItemMeta()) {
            String itemId = ItemUtils.getItemId(item);
            return "catalyst_shard".equals(itemId);
        }
        return false;
    }

    /**
     * Check if an inventory is an enhancement GUI
     */
    private boolean isEnhancementGUI(Inventory inventory) {
        return inventory.getHolder() == null && inventory.getSize() == 27 && !inventory.getViewers().isEmpty()
                && inventory.getViewers().get(0) instanceof Player;
    }
}