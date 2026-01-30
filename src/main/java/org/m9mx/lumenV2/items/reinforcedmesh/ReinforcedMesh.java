package org.m9mx.lumenV2.items.reinforcedmesh;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Entity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.m9mx.lumenV2.item.CustomItem;
import org.m9mx.lumenV2.item.ItemAttributes;
import org.m9mx.lumenV2.item.Recipe;
import org.m9mx.lumenV2.item.RecipeType;

import io.papermc.paper.datacomponent.DataComponentTypes;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;

/**
 * Reinforced Mesh - A utility item that captures mobs and stores them inside
 * Can stack up to 16 when empty, but only 1 when full
 */
public class ReinforcedMesh extends CustomItem {
    private static final NamespacedKey REINFORCED_MESH_KEY = new NamespacedKey("lumen", "reinforced_mesh");
    private static final NamespacedKey CAPTURED_ENTITY_KEY = new NamespacedKey("lumen", "captured_entity");
    private static final Logger LOGGER = Logger.getLogger("ReinforcedMesh");

    public ReinforcedMesh() {
        super("reinforced_mesh");
    }

    @Override
    protected void initialize() {
        setMaterial(Material.ARMOR_STAND);
        setItemModel("lumen:reinforced_mesh");
        setUnbreakable(false); // Not unbreakable since it's consumable
        setEnhancable(false);
        
        setEnabled(false);

        MiniMessage miniMessage = MiniMessage.miniMessage();
        setDisplayName(miniMessage.deserialize("<white>Reinforced Mesh"));

        List<Component> lore = new ArrayList<>();
        lore.add(miniMessage.deserialize("<gray>Utility Tool"));
        lore.add(miniMessage.deserialize(" "));
        lore.add(miniMessage.deserialize("<gray>A heavy-duty, multi-layered weave"));
        lore.add(miniMessage.deserialize("<gray>designed to stabilize and compress"));
        lore.add(miniMessage.deserialize("<gray>biological matter for transport."));
        lore.add(miniMessage.deserialize(" "));
        lore.add(miniMessage.deserialize("<yellow>● <reset><gray>Right-click mob to capture."));
        lore.add(miniMessage.deserialize("<yellow>● <reset><gray>Right-click ground to release."));
        lore.add(miniMessage.deserialize(" "));
        lore.add(miniMessage.deserialize("<dark_gray>[Max Stack: 16]"));
        setLore(lore);
        
        // Set attributes (none needed for this utility item)
        ItemAttributes attrs = getAttributes();
        attrs.setAttackDamage(0.0); // No combat function
        attrs.setAttackSpeed(0.0);

        // Create expensive ritual recipe
        Recipe recipe = new Recipe("reinforced_mesh_recipe", RecipeType.RITUAL);

        // Top row: Netherite Scrap | Diamond (x4) | Netherite Scrap
        recipe.setSlot(0, 0, "NETHERITE_SCRAP", 1);
        recipe.setSlot(0, 1, "DIAMOND", 4);
        recipe.setSlot(0, 2, "NETHERITE_SCRAP", 1);

        // Middle row: Diamond (x4) | Lead (x1) | Diamond (x4)
        recipe.setSlot(1, 0, "DIAMOND", 4);
        recipe.setSlot(1, 1, "LEAD", 1);
        recipe.setSlot(1, 2, "DIAMOND", 4);

        // Bottom row: Iron Ingot (x8) | Blaze Rod (x1) | Iron Ingot (x8)
        recipe.setSlot(2, 0, "IRON_INGOT", 8);
        recipe.setSlot(2, 1, "BLAZE_ROD", 1);
        recipe.setSlot(2, 2, "IRON_INGOT", 8);

        recipe.setResult("reinforced_mesh");
        setRecipe(recipe);
    }

    @Override
    public ItemStack build() {
        ItemStack item = super.build();
        
        // Initialize the item with default values
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            PersistentDataContainer pdc = meta.getPersistentDataContainer();
            
            // Set max stack size to 16 for empty mesh
            meta.setMaxStackSize(16);
            
            // Initialize with no captured entity
            pdc.set(CAPTURED_ENTITY_KEY, PersistentDataType.STRING, "");
            
            item.setItemMeta(meta);
        }
        
        return item;
    }

    /**
     * Check if an ItemStack is a Reinforced Mesh
     */
    public static boolean isReinforcedMesh(ItemStack item) {
        if (item == null || item.getType() != Material.ARMOR_STAND) return false;
        if (!item.hasItemMeta()) return false;
        Object model = item.getData(DataComponentTypes.ITEM_MODEL);
        if (model == null) return false;
        return model.toString().contains("reinforced_mesh");
    }

    /**
     * Check if the mesh contains a captured entity
     */
    public static boolean hasCapturedEntity(ItemStack item) {
        if (!isReinforcedMesh(item) || !item.hasItemMeta()) return false;
        PersistentDataContainer pdc = item.getItemMeta().getPersistentDataContainer();
        String capturedEntity = pdc.get(new NamespacedKey("lumen", "captured_entity"), PersistentDataType.STRING);
        return capturedEntity != null && !capturedEntity.isEmpty();
    }

    /**
     * Get the captured entity type from the mesh
     */
    public static EntityType getCapturedEntity(ItemStack item) {
        if (!hasCapturedEntity(item)) return null;
        PersistentDataContainer pdc = item.getItemMeta().getPersistentDataContainer();
        String entityStr = pdc.get(new NamespacedKey("lumen", "captured_entity"), PersistentDataType.STRING);
        try {
            return EntityType.valueOf(entityStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * Set the captured entity in the mesh
     */
    public static void setCapturedEntity(ItemStack item, EntityType entityType) {
        if (!isReinforcedMesh(item) || !item.hasItemMeta()) return;
        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        
        if (entityType == null) {
            pdc.set(new NamespacedKey("lumen", "captured_entity"), PersistentDataType.STRING, "");
            // Set max stack size back to 16 when empty
            meta.setMaxStackSize(16);
        } else {
            pdc.set(new NamespacedKey("lumen", "captured_entity"), PersistentDataType.STRING, entityType.name());
            // Limit stack size to 1 when filled
            meta.setMaxStackSize(1);
        }
        
        item.setItemMeta(meta);
    }
    
    /**
     * Get a fresh Reinforced Mesh item
     */
    public static ItemStack getReinforcedMeshItem() {
        ReinforcedMesh meshItem = new ReinforcedMesh();
        return meshItem.build();
    }
    
    /**
     * Store entity NBT data in the mesh using NBT only
     */
    public static void storeEntityNBT(ItemStack item, Entity entity) {
        if (!isReinforcedMesh(item) || !item.hasItemMeta()) return;
        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        
        try {
            LOGGER.info("[ReinforcedMesh] Starting NBT storage for entity: " + entity.getType());
            
            Object nmsEntity = entity.getClass().getMethod("getHandle").invoke(entity);
            Class<?> nbtClass = Class.forName("net.minecraft.nbt.CompoundTag");
            Object nbt = nbtClass.getDeclaredConstructor().newInstance();
            
            // Save entity to NBT
            LOGGER.info("[ReinforcedMesh] Saving entity data to NBT...");
            // Use reflection to set entity UUID and type
            try {
                Object uuid = entity.getUniqueId();
                nbt.getClass().getMethod("putUUID", String.class, java.util.UUID.class).invoke(nbt, "UUID", uuid);
                nbt.getClass().getMethod("putString", String.class, String.class).invoke(nbt, "id", "minecraft:" + entity.getType().name().toLowerCase());
                LOGGER.info("[ReinforcedMesh] Added entity UUID and type to NBT");
            } catch (Exception e) {
                LOGGER.warning("[ReinforcedMesh] Could not add UUID/type to NBT: " + e.getMessage());
            }
            
            LOGGER.info("[ReinforcedMesh] Entity NBT prepared: " + nbt.toString());
            
            // Serialize NBT to Base64 using write instead of writeCompressed
            LOGGER.info("[ReinforcedMesh] Serializing NBT to Base64...");
            java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
            java.io.DataOutputStream dos = new java.io.DataOutputStream(baos);
            Class<?> nbtIoClass = Class.forName("net.minecraft.nbt.NbtIo");
            
            // Try write method instead
            try {
                nbtIoClass.getMethod("write", nbtClass, java.io.DataOutput.class).invoke(null, nbt, dos);
                LOGGER.info("[ReinforcedMesh] Used write method");
            } catch (NoSuchMethodException e1) {
                // Try writeAny
                try {
                    nbtIoClass.getMethod("writeAny", nbtClass, java.io.DataOutput.class).invoke(null, nbt, dos);
                    LOGGER.info("[ReinforcedMesh] Used writeAny method");
                } catch (NoSuchMethodException e2) {
                    LOGGER.warning("[ReinforcedMesh] No suitable write method found");
                }
            }
            
            String encodedNBT = java.util.Base64.getEncoder().encodeToString(baos.toByteArray());
            LOGGER.info("[ReinforcedMesh] NBT serialized. Size: " + encodedNBT.length() + " chars");
            
            pdc.set(new NamespacedKey("lumen", "entity_nbt"), PersistentDataType.STRING, encodedNBT);
            item.setItemMeta(meta);
            LOGGER.info("[ReinforcedMesh] NBT stored successfully!");
        } catch (Exception e) {
            LOGGER.severe("[ReinforcedMesh] Error storing NBT: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Get stored NBT data from the mesh
     */
    public static String getStoredEntityNBT(ItemStack item) {
        if (!isReinforcedMesh(item) || !item.hasItemMeta()) return null;
        PersistentDataContainer pdc = item.getItemMeta().getPersistentDataContainer();
        return pdc.get(new NamespacedKey("lumen", "entity_nbt"), PersistentDataType.STRING);
    }
    
    /**
     * Apply stored NBT data to an entity using NBT only
     */
    public static void applyEntityNBT(Entity entity, String encodedNBT) {
        if (encodedNBT == null || encodedNBT.isEmpty()) {
            LOGGER.warning("[ReinforcedMesh] No NBT data to apply for entity: " + entity.getType());
            return;
        }
        try {
            LOGGER.info("[ReinforcedMesh] Starting NBT restore for entity: " + entity.getType());
            LOGGER.info("[ReinforcedMesh] NBT data size: " + encodedNBT.length() + " chars");
            
            // Decode from Base64
            LOGGER.info("[ReinforcedMesh] Decoding NBT from Base64...");
            byte[] decodedBytes = java.util.Base64.getDecoder().decode(encodedNBT);
            LOGGER.info("[ReinforcedMesh] Decoded to " + decodedBytes.length + " bytes");
            
            java.io.ByteArrayInputStream bais = new java.io.ByteArrayInputStream(decodedBytes);
            java.io.DataInputStream dis = new java.io.DataInputStream(bais);
            
            // Read NBT
            LOGGER.info("[ReinforcedMesh] Reading NBT from stream...");
            Class<?> nbtIoClass = Class.forName("net.minecraft.nbt.NbtIo");
            Class<?> nbtClass = Class.forName("net.minecraft.nbt.CompoundTag");
            
            Object nbt = null;
            // Try read method
            try {
                nbt = nbtIoClass.getMethod("read", java.io.DataInput.class).invoke(null, dis);
                LOGGER.info("[ReinforcedMesh] Used read method");
            } catch (NoSuchMethodException e1) {
                // Try readAny
                try {
                    nbt = nbtIoClass.getMethod("readAny", java.io.DataInput.class).invoke(null, dis);
                    LOGGER.info("[ReinforcedMesh] Used readAny method");
                } catch (NoSuchMethodException e2) {
                    LOGGER.warning("[ReinforcedMesh] No suitable read method found");
                }
            }
            
            if (nbt != null) {
                LOGGER.info("[ReinforcedMesh] NBT deserialized: " + nbt.toString());
                
                // Load entity from NBT
                LOGGER.info("[ReinforcedMesh] Loading entity from NBT...");
                Object nmsEntity = entity.getClass().getMethod("getHandle").invoke(entity);
                nmsEntity.getClass().getMethod("load", nbtClass).invoke(nmsEntity, nbt);
                LOGGER.info("[ReinforcedMesh] Entity loaded successfully!");
            } else {
                LOGGER.warning("[ReinforcedMesh] Failed to read NBT data");
            }
        } catch (Exception e) {
            LOGGER.severe("[ReinforcedMesh] Error applying NBT: " + e.getMessage());
            e.printStackTrace();
        }
    }
}