package org.m9mx.lumenV2.items.eternal;

import org.bukkit.Material;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.m9mx.lumenV2.item.CustomItem;

import java.util.Arrays;

public class EternalPorkchop extends CustomItem {

    public EternalPorkchop() {
        super("eternal_porkchop");
    }

    @Override
    protected void initialize() {
        setMaterial(Material.COOKED_PORKCHOP);
        setDisplayName(MiniMessage.miniMessage().deserialize("<gold><bold>Eternal Porkchop"));

        setLore(Arrays.asList(
                MiniMessage.miniMessage().deserialize("<yellow>An infinite source of sustenance"),
                MiniMessage.miniMessage().deserialize("<gold>Never diminishing, always delicious"),
                MiniMessage.miniMessage().deserialize(" "),
                MiniMessage.miniMessage().deserialize("<gold><bold>Obtain by eating pork:"),
                MiniMessage.miniMessage().deserialize("<yellow>  ◦ <reset><gray>Raw Porkchop: 1/500 chance per bite"),
                MiniMessage.miniMessage().deserialize("<yellow>  ◦ <reset><gray>Cooked Porkchop: 1/250 chance per bite"),
                MiniMessage.miniMessage().deserialize("<yellow>  ◦ <reset><gray>Guaranteed at 500 count")));
        
        setItemModel("lumen:eternal_porkchop");
        setUnbreakable(true);
        setEnhancable(false);
        
        // No recipe - special drop
        setRecipe(null);
    }
}
