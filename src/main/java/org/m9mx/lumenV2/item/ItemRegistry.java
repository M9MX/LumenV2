package org.m9mx.lumenV2.item;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Central registry for all custom items
 * Items are automatically registered via reflection - just create a CustomItem subclass in org.m9mx.lumenV2.items
 * Use: ItemRegistry.getInstance().getItem("item_id") anywhere in the code
 */
public class ItemRegistry {
    private static ItemRegistry instance;
    private Map<String, CustomItem> items;
    
    private ItemRegistry() {
        this.items = new HashMap<>();
    }
    
    public static ItemRegistry getInstance() {
        if (instance == null) {
            instance = new ItemRegistry();
        }
        return instance;
    }
    
    /**
     * Register a custom item
     */
    public void register(CustomItem item) {
        items.put(item.getId(), item);
    }
    
    /**
     * Get a custom item by ID
     */
    public CustomItem getItemById(String id) {
        return items.get(id);
    }
    
    /**
     * Create an ItemStack for the given item ID
     */
    public org.bukkit.inventory.ItemStack createItem(String id) {
        CustomItem item = getItemById(id);
        if (item != null) {
            return item.build();
        }
        return null;
    }
    
    /**
     * Check if item exists
     */
    public boolean exists(String id) {
        return items.containsKey(id);
    }
    
    /**
     * Get item by ID (instance method wrapper)
     */
    public CustomItem getItem(String id) {
        return getItemById(id);
    }
    
    /**
     * Get all registered items
     */
    public Map<String, CustomItem> getAll() {
        return new HashMap<>(items);
    }
    
    /**
     * Get all item IDs
     */
    public Set<String> getAllItemIds() {
        return items.keySet();
    }
    
    /**
     * Get all registered item objects
     */
    public Collection<CustomItem> getAllItemsCollection() {
        return items.values();
    }
    
    /**
     * Get number of registered items
     */
    public int getItemCount() {
        return items.size();
    }
    
    // Static convenience methods
    public static CustomItem getItemStatic(String id) {
        return getInstance().getItem(id);
    }
    
    public static Collection<CustomItem> getAllItems() {
        return getInstance().getAllItemsCollection();
    }
}
