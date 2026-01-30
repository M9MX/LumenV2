package org.m9mx.lumenV2.items.lumeguide;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.m9mx.lumenV2.item.CustomItem;
import org.m9mx.lumenV2.item.Recipe;
import org.m9mx.lumenV2.item.RecipeType;
import org.m9mx.lumenV2.systems.protection.ItemProtectionMode;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;

/**
 * Lumen Guide - A written book containing all plugin mechanics and how to use them
 */
public class LumenGuideItem extends CustomItem {

    public LumenGuideItem() {
        super("lumen_guide");
    }

    @Override
    protected void initialize() {
        setMaterial(Material.WRITTEN_BOOK);
        setItemModel("lumen:lumen_guide");
        setUnbreakable(false);
        setEnhancable(false);
        setProtectionMode(ItemProtectionMode.NONE);

        MiniMessage miniMessage = MiniMessage.miniMessage();
        setDisplayName(miniMessage.deserialize("<gold><bold>Lumen Guide"));

        List<Component> lore = new ArrayList<>();
        lore.add(miniMessage.deserialize("<yellow>A comprehensive guide to all Lumen mechanics"));
        setLore(lore);

        // Create normal mode recipe - cool & easy to craft
        Recipe recipe = new Recipe("lumen_guide_recipe", RecipeType.NORMAL);
        
        // Grid layout (0,1,2 = top row; 3,4,5 = middle row; 6,7,8 = bottom row)
        // Row 0: Amethyst Shard, Lapis, Amethyst Shard
        recipe.setSlot(0, 0, "AMETHYST_SHARD", 1);
        recipe.setSlot(0, 1, "LAPIS_LAZULI", 2);
        recipe.setSlot(0, 2, "AMETHYST_SHARD", 1);
        
        // Row 1: Lapis, Book, Lapis
        recipe.setSlot(1, 0, "LAPIS_LAZULI", 2);
        recipe.setSlot(1, 1, "BOOK", 1);
        recipe.setSlot(1, 2, "LAPIS_LAZULI", 2);
        
        // Row 2: Amethyst Shard, Lapis, Amethyst Shard
        recipe.setSlot(2, 0, "AMETHYST_SHARD", 1);
        recipe.setSlot(2, 1, "LAPIS_LAZULI", 2);
        recipe.setSlot(2, 2, "AMETHYST_SHARD", 1);
        
        recipe.setResult("lumen_guide");
        setRecipe(recipe);
    }

    @Override
    public ItemStack build() {
        ItemStack item = super.build();
        BookMeta meta = (BookMeta) item.getItemMeta();

        if (meta == null) return item;

        meta.setTitle("Lumen Guide");
        meta.setAuthor("Lumen");

        // Page 1: Introduction & Commands
        meta.addPage("§6§l ═ LUMEN GUIDE ═\n\n" +
            "§6Welcome to Lumen!\n\n" +
            "§7This guide explains all\n" +
            "mechanics & how to use them.\n\n" +
            "§3§lMAIN COMMANDS:\n" +
            "§7• §7§l/items §r§7- View items\n" +
            "§7• §7§l/trust §r§7- Teams"
        );

        // Page 2: Forge Overview
        meta.addPage("§6§l ═══ FORGE ═══\n\n" +
            "§6Create items in forge!\n" +
            "Click a smithing table\n\n" +
            "§3§lFORGE HAS 2 MODES:\n\n" +
            "§7• §7§lNORMAL §r§7- Reusable\n" +
            "craft items many times\n\n" +
            "§7• §7§lRITUAL §r§7- Special\n" +
            "weapons & unique items"
        );

        // Page 3: Normal Mode
        meta.addPage("§6§l ═ NORMAL MODE ═\n\n" +
            "§6For reusable items!\n\n" +
            "§3§lHow to Craft:\n" +
            "§71. Smithing table\n" +
            "§72. Select NORMAL mode\n" +
            "§73. Place materials\n" +
            "§74. Click CRAFT\n\n" +
            "§7Quick crafting for\n" +
            "tools & supplies"
        );

        // Page 4: Ritual Mode
        meta.addPage("§6§l ═ RITUAL MODE ═\n\n" +
            "§6Powerful one-time items!\n\n" +
            "§3§lHow to Craft:\n" +
            "§71. Smithing table\n" +
            "§72. Select RITUAL mode\n" +
            "§73. Place materials\n" +
            "§74. Click CRAFT\n\n" +
            "§7Takes ~30 minutes\n" +
            "§7Everyone will know!"
        );

        // Page 5: Enhancement System - Catalyst Shards
        meta.addPage("§6§l ═ ENHANCEMENT ═\n\n" +
            "§6Enhance with Shards!\n\n" +
            "§3§lHow to Enhance:\n" +
            "§71. Hold item + shards\n" +
            "§72. Shift+Right click\n" +
            "§73. Max 5 shards\n\n" +
            "§3§lBoost Effect:\n" +
            "§7Damage, speed, & range\n" +
            "increase per shard"
        );

        // Page 6: Using Items & Abilities
        meta.addPage("§6§l ═ ABILITIES ═\n\n" +
            "§6Activate with keys:\n\n" +
            "§3§lAbility Keys:\n" +
            "§7• §7§lRight Click §r§7- Main\n" +
            "§7• §7§lSwap Hands §r§7- Alt\n" +
            "§7• §7§lQ §r§7- Third ability\n" +
            "§7• §7§lShift+Q §r§7- Drop\n\n" +
            "§7Check /items for details"
        );

        // Page 7: Trust System - Teams
        meta.addPage("§6§l ═ TRUST ═\n\n" +
            "§6Work with a team!\n\n" +
            "§3§lTEAM COMMANDS:\n" +
            "§7• §7§l/trust team create\n" +
            "§7• §7§l/trust team invite\n" +
            "§7• §7§l/trust team info\n" +
            "§7• §7§l/trust team leave"
        );

        // Page 8: Trust System - Allies
        meta.addPage("§6§l ═ ALLIES ═\n\n" +
            "§6Form alliances!\n\n" +
            "§3§lALLY COMMANDS:\n" +
            "§7• §7§l/trust allies add\n" +
            "§7• §7§l/trust allies accept\n" +
            "§7• §7§l/trust allies list\n" +
            "§7• §7§l/trust allies remove"
        );

        item.setItemMeta(meta);
        return item;
    }
}
