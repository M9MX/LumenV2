package org.m9mx.lumenV2.item;

import java.util.List;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.m9mx.lumenV2.Lumen;
import org.m9mx.lumenV2.config.ConfigManager;
import org.m9mx.lumenV2.systems.protection.ItemProtectionMode;

import io.papermc.paper.datacomponent.DataComponentTypes;
import net.kyori.adventure.text.Component;

public abstract class CustomItem {
    
    protected String id;
    protected Material material;
    protected String itemModel; // e.g., "lumen:test"
    protected Component displayName;
    protected List<Component> lore;
    protected boolean unbreakable;
    protected ItemAttributes attributes;
    protected Recipe recipe;
    protected boolean enhancable;
    protected ItemProtectionMode protectionMode;
    protected boolean enabled;
    protected org.bukkit.plugin.Plugin plugin; // Add plugin field to access logger
    
    public CustomItem(String id) {
        // Get the plugin instance to access logger
        this.plugin = org.bukkit.Bukkit.getPluginManager().getPlugin("LumenV2");
        this.id = id;
        this.protectionMode = ItemProtectionMode.NONE;
        this.enhancable = false;
        this.unbreakable = false;
        this.enabled = true; // Will be overridden by config loading
        this.attributes = new ItemAttributes();
        initialize();
        loadEnabledStatusFromConfig(); // Load enabled status after initialization
    }
    
    /**
     * Load the enabled status from the items.yml configuration
     */
    private void loadEnabledStatusFromConfig() {
        try {
            boolean configEnabled = ConfigManager.getInstance().getItemsConfig().getBoolean(this.id + ".enabled", true);
            this.enabled = configEnabled;
        } catch (Exception e) {
            // If config isn't loaded yet or any other error occurs, keep default value
            this.enabled = true;
        }
    }
    
    /**
     * Reload the enabled status from configuration
     */
    public void reloadEnabledStatus() {
        try {
            boolean configEnabled = ConfigManager.getInstance().getItemsConfig().getBoolean(this.id + ".enabled", true);
            this.enabled = configEnabled;
        } catch (Exception e) {
            if (plugin != null) {
                plugin.getLogger().warning("Could not reload enabled status for item: " + this.id);
            }
        }
    }
    
    /**
     * Override this to set your item properties
     */
    protected abstract void initialize();
    
    /**
     * Create the ItemStack for this custom item
     */
    public ItemStack build() {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        
        if (meta != null) {
            if (displayName != null) {
                meta.displayName(displayName);
            }
            if (lore != null && !lore.isEmpty()) {
                meta.lore(lore);
            }
            meta.setUnbreakable(unbreakable);
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            
            // Apply attributes if they are set
            applyAttributes(meta);
            
            // Save item ID in Persistent Data Container (replaces NBT tags)
            NamespacedKey itemIdKey = new NamespacedKey("lumen", "item_id");
            PersistentDataContainer pdc = meta.getPersistentDataContainer();
            pdc.set(itemIdKey, PersistentDataType.STRING, this.id);
            
            item.setItemMeta(meta);
        }
        
        // Set item model using data component API
        if (itemModel != null && !itemModel.isEmpty()) {
            NamespacedKey modelKey = NamespacedKey.fromString(itemModel);
            if (modelKey != null) {
                item.setData(DataComponentTypes.ITEM_MODEL, modelKey);
            }
        }
        
        return item;
    }
    
    /**
     * Apply attributes to the item meta
     * Uses player base stats: 1.0 attack damage, 4.0 attack speed
     */
    private void applyAttributes(ItemMeta meta) {
        if (attributes.getAttackDamage() != 0) {
            // Player base attack damage is 1.0, so modifier = target - 1.0
            double damageModifier = attributes.getAttackDamage() - 1.0;
            meta.addAttributeModifier(Attribute.ATTACK_DAMAGE, 
                new AttributeModifier(
                    NamespacedKey.minecraft("lumen_base_damage"),
                    damageModifier,
                    AttributeModifier.Operation.ADD_NUMBER,
                    EquipmentSlotGroup.MAINHAND
                )
            );
        }
        
        if (attributes.getAttackSpeed() != 0) {
            // Player base attack speed is 4.0, so modifier = target - 4.0
            double speedModifier = attributes.getAttackSpeed() - 4.0;
            meta.addAttributeModifier(Attribute.ATTACK_SPEED,
                new AttributeModifier(
                    NamespacedKey.minecraft("lumen_base_speed"),
                    speedModifier,
                    AttributeModifier.Operation.ADD_NUMBER,
                    EquipmentSlotGroup.MAINHAND
                )
            );
        }
    }
    
    /**
     * Get the ID of this custom item
     */
    public String getId() {
        return id;
    }
    
    /**
     * Get the material of this custom item
     */
    public Material getMaterial() {
        return material;
    }
    
    /**
     * Get the display name of this custom item
     */
    public Component getDisplayName() {
        return displayName;
    }
    
    /**
     * Get the lore of this custom item
     */
    public List<Component> getLore() {
        return lore;
    }
    
    /**
     * Get the recipe for this custom item
     */
    public Recipe getRecipe() {
        return recipe;
    }
    
    /**
     * Get whether this item is unbreakable
     */
    public boolean isUnbreakable() {
        return unbreakable;
    }
    
    /**
     * Get the attributes of this custom item
     */
    public ItemAttributes getAttributes() {
        return attributes;
    }
    
    /**
     * Get whether this item can be enhanced
     */
    public boolean isEnhancable() {
        return enhancable;
    }
    
    /**
     * Get the protection mode for this item
     */
    public ItemProtectionMode getProtectionMode() {
        return protectionMode;
    }
    
    /**
     * Get whether this item is enabled
     */
    public boolean isEnabled() {
        return enabled;
    }
    
    /**
     * Set whether this item is enabled
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    
    /**
     * Set the material for this custom item
     */
    protected void setMaterial(Material material) {
        this.material = material;
    }
    
    /**
     * Set the display name for this custom item
     */
    protected void setDisplayName(Component displayName) {
        this.displayName = displayName;
    }
    
    /**
     * Set the lore for this custom item
     */
    protected void setLore(List<Component> lore) {
        this.lore = lore;
    }
    
    /**
     * Set the item model for this custom item
     */
    protected void setItemModel(String itemModel) {
        this.itemModel = itemModel;
    }
    
    /**
     * Set whether this item is unbreakable
     */
    protected void setUnbreakable(boolean unbreakable) {
        this.unbreakable = unbreakable;
    }
    
    /**
     * Set whether this item can be enhanced
     */
    protected void setEnhancable(boolean enhancable) {
        this.enhancable = enhancable;
    }
    
    /**
     * Set the protection mode for this item
     */
    protected void setProtectionMode(ItemProtectionMode protectionMode) {
        this.protectionMode = protectionMode;
    }
    
    /**
     * Set the recipe for this custom item
     */
    protected void setRecipe(Recipe recipe) {
        this.recipe = recipe;
    }
    
    /**
     * Check if this item has a recipe
     */
    public boolean hasRecipe() {
        return recipe != null;
    }
    
    /**
     * Check if this item's recipe is enabled
     */
    public boolean isRecipeEnabled() {
        if (!hasRecipe()) {
            return false;
        }
        
        // Check if the recipe is enabled in the forge system
        try {
            return org.m9mx.lumenV2.systems.ForgeSystem.getInstance().isRecipeEnabled(this.id);
        } catch (Exception e) {
            // If forge system isn't loaded yet, default to true
            return true;
        }
    }
    
    /**
     * Get the item model for this custom item
     */
    public String getItemModel() {
        return itemModel;
    }
}