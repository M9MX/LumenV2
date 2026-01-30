package org.m9mx.lumenV2.items.wardenheart;

import org.bukkit.Material;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.m9mx.lumenV2.item.CustomItem;
import org.m9mx.lumenV2.item.Recipe;
import org.m9mx.lumenV2.item.RecipeType;

import java.util.Arrays;

public class WardenHeart extends CustomItem {

    public WardenHeart() {
        super("warden_heart");
    }

    @Override
    protected void initialize() {
        setMaterial(Material.AMETHYST_SHARD);
        setDisplayName(MiniMessage.miniMessage().deserialize("<dark_purple><bold>Warden Heart"));

        setLore(Arrays.asList(
                MiniMessage.miniMessage().deserialize("<dark_aqua>Pulsing with ancient sculk energy"),
                MiniMessage.miniMessage().deserialize("<aqua>Beating with the rhythm of darkness"),
                MiniMessage.miniMessage().deserialize(" "),
                MiniMessage.miniMessage().deserialize("<dark_aqua><bold>Origin:"),
                MiniMessage.miniMessage().deserialize("<aqua>  ◦ <reset><gray>Dropped by the Warden"),
                MiniMessage.miniMessage().deserialize("<aqua>  ◦ <reset><gray>25% chance when killed"),
                MiniMessage.miniMessage().deserialize("<aqua>  ◦ <reset><gray>Only drops to players")));
        setItemModel("lumen:warden_heart");
        setUnbreakable(true);

        // Note: This item is designed to be dropped by Wardens (25% chance on player
        // kill)
        // It's used as an ingredient in the Awakened Lichblade ritual recipe
        // No crafting recipe is provided as it's a special drop item
    }
}