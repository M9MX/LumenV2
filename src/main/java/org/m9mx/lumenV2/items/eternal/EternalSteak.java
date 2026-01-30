package org.m9mx.lumenV2.items.eternal;

import org.bukkit.Material;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.m9mx.lumenV2.item.CustomItem;

import java.util.Arrays;

public class EternalSteak extends CustomItem {

    public EternalSteak() {
        super("eternal_steak");
    }

    @Override
    protected void initialize() {
        setMaterial(Material.COOKED_BEEF);
        setDisplayName(MiniMessage.miniMessage().deserialize("<gold><bold>Eternal Steak"));

        setLore(Arrays.asList(
                MiniMessage.miniMessage().deserialize("<yellow>An infinite source of sustenance"),
                MiniMessage.miniMessage().deserialize("<gold>Never diminishing, always nourishing"),
                MiniMessage.miniMessage().deserialize(" "),
                MiniMessage.miniMessage().deserialize("<gold><bold>Obtain by eating beef:"),
                MiniMessage.miniMessage().deserialize("<yellow>  ◦ <reset><gray>Raw Beef: 1/500 chance per bite"),
                MiniMessage.miniMessage().deserialize("<yellow>  ◦ <reset><gray>Cooked Beef: 1/250 chance per bite"),
                MiniMessage.miniMessage().deserialize("<yellow>  ◦ <reset><gray>Guaranteed at 500 count")));
        
        setItemModel("lumen:eternal_steak");
        setUnbreakable(true);
        setEnhancable(false);
        
        // No recipe - special drop
        setRecipe(null);
    }
}
