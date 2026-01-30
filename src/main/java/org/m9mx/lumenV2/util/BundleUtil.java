package org.m9mx.lumenV2.util;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.m9mx.lumenV2.Lumen;
import org.m9mx.lumenV2.items.EnhancedBundle;
import org.m9mx.lumenV2.systems.protection.ItemProtectionMode;
import org.m9mx.lumenV2.util.ItemUtils;

import java.util.ArrayList;
import java.util.List;

public class BundleUtil implements Listener {

    private final Lumen plugin;
    private static final int[] CENTRAL_SLOTS = {3, 4, 5, 12, 13, 14, 21, 22, 23};

    public BundleUtil(Lumen plugin) {
        this.plugin = plugin;
    }

    /**
     * Open bundle GUI on right-click
     */
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        ItemStack item = event.getItem();
        if (item == null || !isEnhancedBundle(item)) {
            return;
        }

        event.setCancelled(true);
        openBundleGUI(event.getPlayer(), item);
    }

    /**
     * Create and open the bundle GUI
     */
    private void openBundleGUI(Player player, ItemStack bundle) {
        Inventory gui = Bukkit.createInventory(new BundleHolder(bundle), 27, "Enhanced Bundle");

        // Fill non-central slots with glass panes
        for (int i = 0; i < 27; i++) {
            if (!isCentralSlot(i)) {
                ItemStack pane = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
                ItemMeta meta = pane.getItemMeta();
                meta.setDisplayName(" ");
                pane.setItemMeta(meta);
                gui.setItem(i, pane);
            }
        }

        // Load saved items from bundle tags
        loadSavedItems(gui, bundle);

        player.openInventory(gui);
    }

    /**
     * Load items from bundle's PDC into GUI slots
     */
    private void loadSavedItems(Inventory gui, ItemStack bundle) {
        ItemMeta meta = bundle.getItemMeta();
        if (meta == null) return;

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        String serialized = pdc.get(EnhancedBundle.BUNDLE_CONTENTS_KEY, PersistentDataType.STRING);

        if (serialized != null && !serialized.isEmpty()) {
            List<ItemStack> items = ItemDataHelper.deserializeItemList(serialized);
            for (int i = 0; i < Math.min(items.size(), 9); i++) {
                if (items.get(i) != null) {
                    gui.setItem(CENTRAL_SLOTS[i], items.get(i));
                }
            }
        }
    }

    /**
     * Block clicks on glass panes only (GUI slots 0-26)
     */
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof BundleHolder)) {
            return;
        }

        // Check if clicking in player inventory
        if (event.getClickedInventory() != null && 
            event.getClickedInventory() != event.getInventory()) {
            // Check if player is trying to shift-click an item from their inventory to the bundle
            // while the bundle GUI is open
            if (event.getClick().isShiftClick()) {
                ItemStack itemToMove = event.getCurrentItem();
                
                if (itemToMove != null && !itemToMove.isEmpty()) {
                    // Check if the item is a protected item
                    if (ItemUtils.isProtected(itemToMove)) {
                        ItemProtectionMode mode = ItemUtils.getProtectionMode(itemToMove);
                        if (mode == ItemProtectionMode.SIMPLE || mode == ItemProtectionMode.STRICT) {
                            event.setCancelled(true);
                            if (event.getWhoClicked() instanceof Player) {
                                ((Player) event.getWhoClicked()).sendMessage("§cYou cannot place protected items in the bundle!");
                            }
                            return;
                        }
                    }
                    
                    // Check if the item is another Enhanced Bundle
                    if (isEnhancedBundle(itemToMove)) {
                        event.setCancelled(true);
                        if (event.getWhoClicked() instanceof Player) {
                            ((Player) event.getWhoClicked()).sendMessage("§cYou cannot place a bundle inside another bundle!");
                        }
                        return;
                    }
                }
            }
            // Allow all other clicks in player inventory
            return;
        }

        int slot = event.getSlot();
        
        // Block clicks on glass panes in GUI (non-central slots)
        if (!isCentralSlot(slot)) {
            event.setCancelled(true);
            return;
        }

        // Handle shift-clicking (moving items between bundle and player inventory)
        if (event.getClick().isShiftClick()) {
            // If player is shift-clicking FROM the bundle (from bundle to player inventory)
            if (event.getInventory().getHolder() instanceof BundleHolder && 
                event.getClickedInventory() == event.getInventory()) {
                
                ItemStack itemBeingMoved = event.getCurrentItem();
                
                if (itemBeingMoved != null && !itemBeingMoved.isEmpty()) {
                    // Check if the item is a protected item
                    if (ItemUtils.isProtected(itemBeingMoved)) {
                        ItemProtectionMode mode = ItemUtils.getProtectionMode(itemBeingMoved);
                        if (mode == ItemProtectionMode.SIMPLE || mode == ItemProtectionMode.STRICT) {
                            // Allow taking protected items OUT of bundle - this is the intended behavior
                            // Don't cancel the event, just let it proceed
                        }
                    }
                    // Check if the item is another Enhanced Bundle
                    // Allow taking bundles out of bundles - this is the intended behavior
                }
                // For shift-clicking from bundle to player inventory, we don't cancel the event
                // regardless of whether the item is protected or another bundle, because taking items out is allowed
            }
        }

        // Check if the item being added to the bundle is protected (for non-shift clicks)
        ItemStack cursorItem = event.getCursor(); // Item in player's cursor
        ItemStack clickedItem = event.getCurrentItem(); // Item in the bundle slot
        
        // If player is trying to put an item into the bundle (cursor has item, slot is empty or has space)
        // But not for shift-clicks (handled above)
        if (!event.getClick().isShiftClick() && cursorItem != null && !cursorItem.isEmpty()) {
            // Check if the item is a protected item
            if (ItemUtils.isProtected(cursorItem)) {
                ItemProtectionMode mode = ItemUtils.getProtectionMode(cursorItem);
                if (mode == ItemProtectionMode.SIMPLE || mode == ItemProtectionMode.STRICT) {
                    event.setCancelled(true);
                    if (event.getWhoClicked() instanceof Player) {
                        ((Player) event.getWhoClicked()).sendMessage("§cYou cannot place protected items in the bundle!");
                    }
                    return;
                }
            }
            
            // Check if the item is another Enhanced Bundle
            if (isEnhancedBundle(cursorItem)) {
                event.setCancelled(true);
                if (event.getWhoClicked() instanceof Player) {
                    ((Player) event.getWhoClicked()).sendMessage("§cYou cannot place a bundle inside another bundle!");
                }
                return;
            }
        }
        
        // If player is trying to take an item out of the bundle and it's protected (for non-shift clicks)
        // For shift-clicks, we already handled this above
        if (!event.getClick().isShiftClick() && clickedItem != null && !clickedItem.isEmpty()) {
            // Check if the item is a protected item
            if (ItemUtils.isProtected(clickedItem)) {
                ItemProtectionMode mode = ItemUtils.getProtectionMode(clickedItem);
                if (mode == ItemProtectionMode.SIMPLE || mode == ItemProtectionMode.STRICT) {
                    // Allow taking out protected items - this is the intended behavior
                }
            }
            // Allow taking out other bundles - this is the intended behavior
        }
    }

    /**
     * Save items from bundle GUI to bundle's PDC when closed
     */
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getInventory().getHolder() instanceof BundleHolder)) {
            return;
        }

        BundleHolder holder = (BundleHolder) event.getInventory().getHolder();
        ItemStack bundle = holder.getBundle();

        if (bundle == null || !isEnhancedBundle(bundle)) {
            return;
        }

        // Collect items from central slots
        List<ItemStack> items = new ArrayList<>();
        for (int slot : CENTRAL_SLOTS) {
            ItemStack item = event.getInventory().getItem(slot);
            if (item != null && !item.isEmpty() && !isGlassPane(item)) {
                items.add(item.clone());
            }
        }

        // Serialize and save to bundle's PDC
        String serialized = ItemDataHelper.serializeItemList(items);
        
        ItemMeta meta = bundle.getItemMeta();
        if (meta != null) {
            meta.getPersistentDataContainer().set(
                    EnhancedBundle.BUNDLE_CONTENTS_KEY,
                    PersistentDataType.STRING,
                    serialized == null ? "" : serialized
            );
            bundle.setItemMeta(meta);
        }

        // Update bundle in player's inventory
        Player player = (Player) event.getPlayer();
        updateBundleInInventory(player, bundle);
    }

    /**
     * Update the bundle item in the player's inventory
     */
    private void updateBundleInInventory(Player player, ItemStack bundle) {
        // Check main hand
        if (isSameBundleId(player.getInventory().getItemInMainHand(), bundle)) {
            player.getInventory().setItemInMainHand(bundle);
            return;
        }

        // Check off hand
        if (isSameBundleId(player.getInventory().getItemInOffHand(), bundle)) {
            player.getInventory().setItemInOffHand(bundle);
            return;
        }

        // Check inventory
        for (int i = 0; i < player.getInventory().getSize(); i++) {
            if (isSameBundleId(player.getInventory().getItem(i), bundle)) {
                player.getInventory().setItem(i, bundle);
                return;
            }
        }
    }

    /**
     * Check if two items are the same bundle (by item ID)
     */
    private boolean isSameBundleId(ItemStack item, ItemStack bundle) {
        if (item == null || bundle == null) return false;
        String itemId = ItemUtils.getItemId(item);
        String bundleId = ItemUtils.getItemId(bundle);
        return itemId != null && itemId.equals(bundleId) && itemId.equals("enhanced_bundle");
    }

    /**
     * Check if slot is in the central 3x3 area
     */
    private boolean isCentralSlot(int slot) {
        for (int s : CENTRAL_SLOTS) {
            if (s == slot) return true;
        }
        return false;
    }

    /**
     * Check if item is a glass pane (GUI decoration)
     */
    private boolean isGlassPane(ItemStack item) {
        return item != null && item.getType().name().contains("GLASS_PANE");
    }

    /**
     * Check if item is an enhanced bundle
     */
    private boolean isEnhancedBundle(ItemStack item) {
        if (!item.hasItemMeta()) return false;
        String id = ItemUtils.getItemId(item);
        return id != null && id.equals("enhanced_bundle");
    }

    /**
     * Inventory holder for bundle GUI
     */
    private static class BundleHolder implements InventoryHolder {
        private final ItemStack bundle;

        public BundleHolder(ItemStack bundle) {
            this.bundle = bundle;
        }

        public ItemStack getBundle() {
            return bundle;
        }

        @Override
        public Inventory getInventory() {
            return null;
        }
    }
}