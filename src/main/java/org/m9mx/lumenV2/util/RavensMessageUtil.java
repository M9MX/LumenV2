package org.m9mx.lumenV2.util;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.event.player.PlayerInteractEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.m9mx.lumenV2.Lumen;
import org.m9mx.lumenV2.items.RavensMessage;
import org.m9mx.lumenV2.systems.protection.ItemProtectionMode;
import org.m9mx.lumenV2.data.RavensMessageDataManager;
import org.m9mx.lumenV2.item.ItemRegistry;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class RavensMessageUtil implements Listener {

    private final Lumen plugin;
    private static final Map<UUID, ItemStack> mailInProgress = new HashMap<>(); // Track mail being composed
    private static final Map<UUID, String> awaitingRecipient = new HashMap<>(); // Track players waiting for recipient name

    public RavensMessageUtil(Lumen plugin) {
        this.plugin = plugin;
    }

    /**
     * Open raven's message GUI on right-click
     */
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        ItemStack item = event.getItem();
        if (item == null) {
            return;
        }
        
        // Check if it's a closed envelope
        if (isClosedEnvelope(item)) {
            event.setCancelled(true);
            openEnvelope(event.getPlayer(), item);
            return;
        }
        
        // Check if it's a raven's message (for composing)
        if (!isRavensMessage(item)) {
            return;
        }

        event.setCancelled(true);
        openMailGUI(event.getPlayer(), item);
    }

    /**
     * Open a closed envelope and give the player the contents
     */
    private void openEnvelope(Player player, ItemStack envelope) {
        ItemMeta meta = envelope.getItemMeta();
        
        if (meta == null) {
            player.sendMessage("§cFailed to open envelope!");
            return;
        }
        
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        String serialized = pdc.get(RavensMessage.MAIL_CONTENTS_KEY, PersistentDataType.STRING);
        
        if (serialized == null || serialized.isEmpty()) {
            player.sendMessage("§cThis envelope is empty!");
            return;
        }
        
        // Deserialize the contents
        ItemStack contents = ItemDataHelper.deserializeItem(serialized);
        if (contents == null) {
            player.sendMessage("§cFailed to retrieve envelope contents!");
            return;
        }
        
        // Show item name and lore in message with color codes preserved
        Component itemNameComponent = createItemComponent(contents);
        Component message = MiniMessage.miniMessage().deserialize("<green>You received: ").append(itemNameComponent);
        player.sendMessage(message);
        
        // Give the item to the player
        player.getInventory().addItem(contents);
        
        // Find envelope slot and replace with open version
        int envelopeSlot = -1;
        if (player.getInventory().getItemInMainHand().isSimilar(envelope)) {
            envelopeSlot = 40; // Main hand slot constant
            ItemStack openEnvelope = ItemRegistry.getInstance().createItem("ravens_message");
            player.getInventory().setItemInMainHand(openEnvelope);
        } else {
            for (int i = 0; i < player.getInventory().getSize(); i++) {
                ItemStack slot = player.getInventory().getItem(i);
                if (slot != null && slot.isSimilar(envelope)) {
                    envelopeSlot = i;
                    ItemStack openEnvelope = ItemRegistry.getInstance().createItem("ravens_message");
                    player.getInventory().setItem(i, openEnvelope);
                    break;
                }
            }
        }
        
        player.playSound(player.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, SoundCategory.PLAYERS, 0.8f, 1.0f);
    }

    /**
     * Create and open the mail composition GUI
     */
    private void openMailGUI(Player player, ItemStack mail) {
        Inventory gui = Bukkit.createInventory(new MailHolder(mail), 27, "Raven's Message");

        // Fill all slots with gray glass panes except slot 11 (item) and slot 15 (confirm)
        for (int i = 0; i < 27; i++) {
            if (i != 11 && i != 15) {
                ItemStack pane = createGlassPane();
                gui.setItem(i, pane);
            }
        }

        // Load existing item from memory (mailInProgress) if any
        ItemStack previousItem = mailInProgress.get(player.getUniqueId());
        if (previousItem != null && !previousItem.isEmpty()) {
            gui.setItem(11, previousItem.clone());
        }

        // Set confirm button (slot 15)
        ItemStack confirmButton = new ItemStack(Material.LEATHER_HORSE_ARMOR);
        ItemMeta confirmMeta = confirmButton.getItemMeta();
        if (confirmMeta != null) {
            confirmMeta.setDisplayName("§aConfirm");
            confirmMeta.setItemModel(new org.bukkit.NamespacedKey("lumen", "confirm_mark"));
            confirmButton.setItemMeta(confirmMeta);
        }
        gui.setItem(15, confirmButton);

        player.openInventory(gui);
    }

    /**
     * Block clicks on glass panes, allow item placement in slot 11, confirm button in slot 15
     */
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof MailHolder)) {
            return;
        }

        // Allow clicks in player inventory
        if (event.getClickedInventory() != null && 
            event.getClickedInventory() != event.getInventory()) {
            return;
        }

        int slot = event.getSlot();

        // Confirm button (slot 15)
        if (slot == 15) {
            event.setCancelled(true);
            if (!(event.getWhoClicked() instanceof Player)) return;
            Player player = (Player) event.getWhoClicked();
            
            // Get the item in slot 11
            ItemStack itemToSend = event.getInventory().getItem(11);
            if (itemToSend == null || itemToSend.getType() == Material.AIR) {
                player.sendMessage("§cPlease place an item to send first!");
                return;
            }

            // Check if item is protected with STRICT mode
            if (ItemUtils.isProtected(itemToSend)) {
                ItemProtectionMode mode = ItemUtils.getProtectionMode(itemToSend);
                if (mode == ItemProtectionMode.STRICT) {
                    player.sendMessage("§cYou cannot send strictly protected items!");
                    return;
                }
            }

            // Store the mail data and ask for recipient
            mailInProgress.put(player.getUniqueId(), itemToSend.clone());
            awaitingRecipient.put(player.getUniqueId(), "true");
            
            player.closeInventory();
            player.sendMessage("§aType the player name to send this item to:");
            return;
        }

        // Item slot (slot 11) - allow placing items
        if (slot == 11) {
            event.setCancelled(false); // Allow all interactions
            
            // Check if trying to place a protected item with STRICT mode
            ItemStack cursorItem = event.getCursor();
            if (cursorItem != null && !cursorItem.isEmpty()) {
                if (ItemUtils.isProtected(cursorItem)) {
                    ItemProtectionMode mode = ItemUtils.getProtectionMode(cursorItem);
                    if (mode == ItemProtectionMode.STRICT) {
                        event.setCancelled(true);
                        if (event.getWhoClicked() instanceof Player) {
                            ((Player) event.getWhoClicked()).sendMessage("§cYou cannot send strictly protected items!");
                        }
                        return;
                    }
                }
            }
            return;
        }

        // Block clicks on glass panes
        event.setCancelled(true);
    }

    /**
     * Handle chat for recipient name
     */
    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();

        if (!awaitingRecipient.containsKey(playerId)) {
            return;
        }

        event.setCancelled(true);
        String recipientName = event.getMessage().trim();

        // Remove from waiting list
        awaitingRecipient.remove(playerId);
        ItemStack mailItem = mailInProgress.remove(playerId);

        if (mailItem == null) {
            player.sendMessage("§cError: Mail data lost. Please try again.");
            return;
        }

        Player recipient = Bukkit.getPlayer(recipientName);

        if (recipient != null) {
            // Player is online - send directly
            sendMailToPlayer(recipient, mailItem, player.getName());
            
            // Send message with item name and hover lore
            Component itemNameComponent = createItemComponent(mailItem);
            Component message = MiniMessage.miniMessage().deserialize("<green>Item sent to " + recipientName + ": ").append(itemNameComponent);
            player.sendMessage(message);
            
            // Sound effect for sender (quiet mystical sound)
            player.playSound(player.getLocation(), Sound.ITEM_FIRECHARGE_USE, SoundCategory.PLAYERS, 0.4f, 1.2f);
            recipient.playSound(recipient.getLocation(), Sound.BLOCK_BELL_USE, SoundCategory.PLAYERS, 0.7f, 1.0f);
        } else {
            // Player offline - save for later as wrapped envelope
            ItemStack envelope = createEnvelope(mailItem, player.getName());
            RavensMessageDataManager.savePendingMail(recipientName, envelope, player.getName());
            
            // Send message with item name and hover lore
            Component itemNameComponent = createItemComponent(mailItem);
            Component message = MiniMessage.miniMessage().deserialize("<green>Item will be delivered to " + recipientName + " when they join: ").append(itemNameComponent);
            player.sendMessage(message);
            
            // Sound effect for offline delivery (quiet mystical sound)
            player.playSound(player.getLocation(), Sound.ITEM_FIRECHARGE_USE, SoundCategory.PLAYERS, 0.4f, 1.2f);
        }

        // Remove only 1 item from the raven's message stack
        // First check main hand
        ItemStack mainHand = player.getInventory().getItemInMainHand();
        if (mainHand.getType() == Material.PAPER && isRavensMessage(mainHand)) {
            mainHand.setAmount(mainHand.getAmount() - 1);
        } else {
            // Check entire inventory
            boolean found = false;
            for (int i = 0; i < player.getInventory().getSize(); i++) {
                ItemStack item = player.getInventory().getItem(i);
                if (item != null && item.getType() == Material.PAPER && isRavensMessage(item)) {
                    item.setAmount(item.getAmount() - 1);
                    found = true;
                    break;
                }
            }
        }
    }

    /**
     * Track composition state without persisting to item (prevents duplication)
     */
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getInventory().getHolder() instanceof MailHolder)) {
            return;
        }

        MailHolder holder = (MailHolder) event.getInventory().getHolder();
        ItemStack mail = holder.getMail();

        if (mail == null || !isRavensMessage(mail)) {
            return;
        }

        // Store item from slot 11 in mailInProgress for later sending
        Player player = (Player) event.getPlayer();
        ItemStack itemInSlot = event.getInventory().getItem(11);
        
        if (itemInSlot != null && !itemInSlot.isEmpty()) {
            // Keep the item in memory until they confirm sending
            mailInProgress.put(player.getUniqueId(), itemInSlot.clone());
        } else {
            // Clear composition if empty
            mailInProgress.remove(player.getUniqueId());
        }
    }

    /**
     * Send mail directly to online player
     */
    private void sendMailToPlayer(Player recipient, ItemStack item, String senderName) {
        // Create envelope wrapper
        ItemStack envelope = createEnvelope(item, senderName);
        recipient.getInventory().addItem(envelope);
        
        // Send message with item name and hover lore
        Component itemNameComponent = createItemComponent(item);
        Component message = MiniMessage.miniMessage().deserialize("<blue>" + senderName + " sent you an item via Raven's Message: ").append(itemNameComponent);
        recipient.sendMessage(message);
    }

    /**
     * Create an envelope wrapper for the item
     */
    private ItemStack createEnvelope(ItemStack contents, String senderName) {
        ItemStack envelope = new ItemStack(Material.PAPER);
        ItemMeta meta = envelope.getItemMeta();
        
        if (meta != null) {
            // Set display name with MiniMessage
            meta.displayName(MiniMessage.miniMessage().deserialize("<white><bold>Raven's Message - From " + senderName));
            
            // Set model to closed envelope
            meta.setItemModel(new org.bukkit.NamespacedKey("lumen", "ravens_message"));
            
            // Set lore
            java.util.List<Component> loreParts = new java.util.ArrayList<>();
            loreParts.add(MiniMessage.miniMessage().deserialize("<gray>From: <white>" + senderName));
            loreParts.add(MiniMessage.miniMessage().deserialize("<gray>Right-click to open"));
            meta.lore(loreParts);
            
            envelope.setItemMeta(meta);
        }
        
        // Store the contents in PDC and mark as envelope
        if (meta != null) {
            PersistentDataContainer pdc = meta.getPersistentDataContainer();
            String serialized = ItemDataHelper.serializeItem(contents);
            pdc.set(RavensMessage.MAIL_CONTENTS_KEY, PersistentDataType.STRING, serialized == null ? "" : serialized);
            // Mark as envelope to distinguish from compose item
            pdc.set(new org.bukkit.NamespacedKey("lumen", "is_envelope"), PersistentDataType.BYTE, (byte) 1);
            envelope.setItemMeta(meta);
        }
        
        return envelope;
    }

    /**
     * Create a component with item name and hover lore
     */
    private Component createItemComponent(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        Component component;
        
        // Get display name - try Component first, fallback to legacy string
        if (meta != null && meta.displayName() != null) {
            component = meta.displayName();
        } else if (meta != null && meta.hasDisplayName()) {
            String legacyName = meta.getDisplayName();
            // Convert legacy codes (§) to Component
            component = LegacyComponentSerializer.legacySection().deserialize(legacyName);
        } else {
            component = Component.text(item.getType().toString());
        }
        
        // Add lore hover event if item has lore
        if (meta != null && meta.hasLore()) {
            Component loreComponent = Component.empty();
            for (int i = 0; i < meta.getLore().size(); i++) {
                String loreLine = meta.getLore().get(i);
                // Convert legacy codes in lore
                Component loreLineComponent = LegacyComponentSerializer.legacySection().deserialize(loreLine);
                loreComponent = loreComponent.append(loreLineComponent);
                if (i < meta.getLore().size() - 1) {
                    loreComponent = loreComponent.append(Component.newline());
                }
            }
            component = component.hoverEvent(HoverEvent.showText(loreComponent));
        }
        
        return component;
    }



    /**
     * Create a gray glass pane for GUI decoration
     */
    private ItemStack createGlassPane() {
        ItemStack pane = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = pane.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(" ");
            pane.setItemMeta(meta);
        }
        return pane;
    }

    /**
     * Check if item is a raven's message
     */
    private boolean isRavensMessage(ItemStack item) {
        if (!item.hasItemMeta()) return false;
        String id = ItemUtils.getItemId(item);
        return id != null && id.equals("ravens_message");
    }

    /**
     * Check if item is a closed envelope
     */
    private boolean isClosedEnvelope(ItemStack item) {
        if (!item.hasItemMeta() || item.getType() != Material.PAPER) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        return pdc.has(new org.bukkit.NamespacedKey("lumen", "is_envelope"), PersistentDataType.BYTE);
    }

    /**
     * Inventory holder for mail GUI
     */
    private static class MailHolder implements InventoryHolder {
        private final ItemStack mail;

        public MailHolder(ItemStack mail) {
            this.mail = mail;
        }

        public ItemStack getMail() {
            return mail;
        }

        @Override
        public Inventory getInventory() {
            return null;
        }
    }
}
