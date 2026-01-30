package org.m9mx.lumenV2.command;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.ItemStack;
import org.m9mx.lumenV2.item.CustomItem;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;

/**
 * Listener for Items GUI interactions
 */
public class ItemsCommandListener implements Listener {

    private static final Map<UUID, Integer> playerPages = new HashMap<>();

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        // Check if this is the items GUI
        if (!event.getView().getTitle().contains("Items")) {
            return;
        }

        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getWhoClicked();
        int slot = event.getRawSlot();
        UUID playerId = player.getUniqueId();
        int currentPage = playerPages.getOrDefault(playerId, 0);

        // Next page button
        if (slot == ItemsCommand.getNextPageButtonSlot()) {
            playerPages.put(playerId, currentPage + 1);
            player.openInventory(ItemsCommand.createItemsGUI(currentPage + 1));
            return;
        }

        // Previous page button
        if (slot == ItemsCommand.getPrevPageButtonSlot()) {
            if (currentPage > 0) {
                playerPages.put(playerId, currentPage - 1);
                player.openInventory(ItemsCommand.createItemsGUI(currentPage - 1));
            }
            return;
        }

        // Back button
        if (slot == ItemsCommand.getBackButtonSlot()) {
            playerPages.remove(playerId);
            player.closeInventory();
            return;
        }

        // Right-click to give item in creative mode
        if (event.isRightClick() && player.getGameMode().equals(GameMode.CREATIVE)) {
            CustomItem customItem = getCustomItemAtSlot(slot, currentPage);
            if (customItem != null) {
                ItemStack itemToGive = customItem.build();
                if (itemToGive != null) {
                    player.getInventory().addItem(itemToGive);
                    player.sendMessage(MiniMessage.miniMessage().deserialize("<green>Given: " + customItem.getId()));
                }
            }
            return;
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player))
            return;

        Player player = (Player) event.getPlayer();
        if (event.getInventory().getViewers().size() <= 1) {
            playerPages.remove(player.getUniqueId());
        }
    }

    /**
     * Get the CustomItem at the given slot on the current page
     */
    private CustomItem getCustomItemAtSlot(int slot, int currentPage) {
        int[] itemSlots = ItemsCommand.getItemSlots();

        // Find which index in ITEM_SLOTS this slot is
        for (int i = 0; i < itemSlots.length; i++) {
            if (itemSlots[i] == slot) {
                // Calculate the absolute item index
                int itemIndex = (currentPage * ItemsCommand.getMaxItemsPerPage()) + i;
                return ItemsCommand.getEnabledItemAt(itemIndex);
            }
        }

        return null;
    }
}
