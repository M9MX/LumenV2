package org.m9mx.lumenV2.items.lostsoul;

import java.util.Arrays;

import org.bukkit.Material;
import org.m9mx.lumenV2.item.CustomItem;
import org.m9mx.lumenV2.systems.protection.ItemProtectionMode;

import net.kyori.adventure.text.minimessage.MiniMessage;

public class LostSoul extends CustomItem {

    public LostSoul() {
        super("lost_soul");
    }

    @Override
    protected void initialize() {
        setMaterial(Material.GLOWSTONE);
        setDisplayName(MiniMessage.miniMessage().deserialize("<dark_aqua><bold>Lost Soul"));

        setLore(Arrays.asList(
                MiniMessage.miniMessage().deserialize("<aqua>Trapped essence of a wandering soul"),
                MiniMessage.miniMessage().deserialize("<dark_aqua>Disturbed from its eternal rest"),
                MiniMessage.miniMessage().deserialize(" "),
                MiniMessage.miniMessage().deserialize("<dark_aqua><bold>Origin:"),
                MiniMessage.miniMessage().deserialize("<aqua>  ◦ <reset><gray>Chance to obtain when walking"),
                MiniMessage.miniMessage().deserialize("<aqua>  ◦ <reset><gray>on soul sand with Soul Speed")));

        setItemModel("lumen:lost_soul");
        setUnbreakable(true);
        setEnhancable(false);

        // Lost Soul has no recipe since it's obtained through gameplay mechanics
        setRecipe(null);
    }
}