package org.m9mx.lumenV2.command;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.m9mx.lumenV2.item.CustomItem;
import org.m9mx.lumenV2.item.ItemRegistry;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;

/**
 * /items command - Shows all enabled items in a paginated GUI
 * Similar layout to Recipe Book GUI
 */
public class ItemsCommand implements CommandExecutor, TabCompleter {

    private static final int[] ITEM_SLOTS = {
        10, 11, 12, 13, 14, 15, 16,
        19, 20, 21, 22, 23, 24, 25,
        28, 29, 30, 31, 32, 33, 34
    };
    private static final int BACK_BUTTON_SLOT = 47;
    private static final int NEXT_PAGE_BUTTON_SLOT = 50;
    private static final int PREV_PAGE_BUTTON_SLOT = 52;
    private static final int MAX_ITEMS_PER_PAGE = 21;

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cOnly players can use this command");
            return true;
        }

        Player player = (Player) sender;
        int page = 0;

        // Parse page number if provided
        if (args.length > 0) {
            try {
                page = Integer.parseInt(args[0]) - 1; // Convert to 0-indexed
                if (page < 0) {
                    page = 0;
                }
            } catch (NumberFormatException e) {
                player.sendMessage("§cInvalid page number");
                return true;
            }
        }

        Inventory itemsGUI = createItemsGUI(page);
        player.openInventory(itemsGUI);
        return true;
    }

    /**
     * Create the items GUI for a specific page
     */
    public static Inventory createItemsGUI(int page) {
        if (page < 0) page = 0;
        Inventory gui = Bukkit.createInventory(null, 54, Component.text("Items").decorate(TextDecoration.BOLD));

        // Fill with gray glass panes
        ItemStack grayPane = createGrayPane();
        for (int i = 0; i < 54; i++) {
            gui.setItem(i, grayPane);
        }

        // Get all enabled items
        List<CustomItem> enabledItems = getEnabledItems();

        // Calculate pages
        int totalPages = Math.max(1, (enabledItems.size() + MAX_ITEMS_PER_PAGE - 1) / MAX_ITEMS_PER_PAGE);
        int adjustedPage = Math.min(page, totalPages - 1);

        // Display items for current page
        int startIndex = adjustedPage * MAX_ITEMS_PER_PAGE;
        int endIndex = Math.min(startIndex + MAX_ITEMS_PER_PAGE, enabledItems.size());

        for (int i = startIndex; i < endIndex; i++) {
            int slotIndex = i - startIndex;
            CustomItem item = enabledItems.get(i);
            ItemStack displayItem = item.build();
            gui.setItem(ITEM_SLOTS[slotIndex], displayItem);
        }

        // Set back button (not functional in this context, but kept for consistency)
        gui.setItem(BACK_BUTTON_SLOT, createBackButton());

        // Set pagination buttons only if there are more than 21 items
        if (enabledItems.size() > MAX_ITEMS_PER_PAGE) {
            if (adjustedPage > 0) {
                gui.setItem(PREV_PAGE_BUTTON_SLOT, createPrevPageButton());
            } else {
                gui.setItem(PREV_PAGE_BUTTON_SLOT, createGrayPane());
            }

            if (adjustedPage < totalPages - 1) {
                gui.setItem(NEXT_PAGE_BUTTON_SLOT, createNextPageButton());
            } else {
                gui.setItem(NEXT_PAGE_BUTTON_SLOT, createGrayPane());
            }
        }

        return gui;
    }

    /**
     * Get all enabled items (not disabled in any way)
     */
    private static List<CustomItem> getEnabledItems() {
        List<CustomItem> items = new ArrayList<>();

        for (CustomItem item : ItemRegistry.getAllItems()) {
            // Only include items that are enabled
            if (!item.isEnabled()) continue;
            
            // Check if item is disabled in config
            if (!isItemEnabled(item.getId())) continue;
            
            items.add(item);
        }

        return items;
    }
    
    /**
     * Check if an item is enabled in items.yml
     */
    private static boolean isItemEnabled(String itemId) {
        try {
            return org.m9mx.lumenV2.config.ConfigManager.getInstance().getItemsConfig().getBoolean(itemId + ".enabled", true);
        } catch (Exception e) {
            return true; // Default to enabled if config not found
        }
    }

    /**
     * Create a gray glass pane
     */
    private static ItemStack createGrayPane() {
        ItemStack pane = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta meta = pane.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text(" "));
            pane.setItemMeta(meta);
        }
        return pane;
    }

    /**
     * Create the back button (Barrier block)
     */
    private static ItemStack createBackButton() {
        ItemStack button = new ItemStack(Material.BARRIER);
        ItemMeta meta = button.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text("Back", TextColor.color(0xFF0000), TextDecoration.BOLD));
            List<Component> lore = new ArrayList<>();
            lore.add(Component.text("Close menu", TextColor.color(0xAAAAAA)));
            meta.lore(lore);
            button.setItemMeta(meta);
        }
        return button;
    }

    /**
     * Create the next page button
     */
    private static ItemStack createNextPageButton() {
        ItemStack button = new ItemStack(Material.ARROW);
        ItemMeta meta = button.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text("Next Page", TextColor.color(0x4ECDC4), TextDecoration.BOLD));
            List<Component> lore = new ArrayList<>();
            lore.add(Component.text("Go to next page", TextColor.color(0xAAAAAA)));
            meta.lore(lore);
            button.setItemMeta(meta);
        }
        return button;
    }

    /**
     * Create the previous page button
     */
    private static ItemStack createPrevPageButton() {
        ItemStack button = new ItemStack(Material.ARROW);
        ItemMeta meta = button.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text("Previous Page", TextColor.color(0x4ECDC4), TextDecoration.BOLD));
            List<Component> lore = new ArrayList<>();
            lore.add(Component.text("Go to previous page", TextColor.color(0xAAAAAA)));
            meta.lore(lore);
            button.setItemMeta(meta);
        }
        return button;
    }

    /**
     * Get the item slots
     */
    public static int[] getItemSlots() {
        return ITEM_SLOTS;
    }

    /**
     * Get the back button slot
     */
    public static int getBackButtonSlot() {
        return BACK_BUTTON_SLOT;
    }

    /**
     * Get the next page button slot
     */
    public static int getNextPageButtonSlot() {
        return NEXT_PAGE_BUTTON_SLOT;
    }

    /**
     * Get the previous page button slot
     */
    public static int getPrevPageButtonSlot() {
        return PREV_PAGE_BUTTON_SLOT;
    }

    /**
     * Get max items per page
     */
    public static int getMaxItemsPerPage() {
        return MAX_ITEMS_PER_PAGE;
    }

    /**
     * Get total number of enabled items
     */
    public static int getTotalEnabledItems() {
        return getEnabledItems().size();
    }

    /**
     * Get the enabled item at the given index
     */
    public static CustomItem getEnabledItemAt(int index) {
        List<CustomItem> items = getEnabledItems();
        if (index >= 0 && index < items.size()) {
            return items.get(index);
        }
        return null;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return new ArrayList<>();
    }
}
