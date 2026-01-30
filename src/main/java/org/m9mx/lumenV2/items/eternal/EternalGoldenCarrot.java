package org.m9mx.lumenV2.items.eternal;

import org.bukkit.Material;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.m9mx.lumenV2.item.CustomItem;

import java.util.Arrays;

public class EternalGoldenCarrot extends CustomItem {

    public EternalGoldenCarrot() {
        super("eternal_golden_carrot");
    }

    @Override
    protected void initialize() {
        setMaterial(Material.GOLDEN_CARROT);
        setDisplayName(MiniMessage.miniMessage().deserialize("<gold><bold>Eternal Golden Carrot"));

        setLore(Arrays.asList(
                MiniMessage.miniMessage().deserialize("<yellow>An infinite source of vitality"),
                MiniMessage.miniMessage().deserialize("<gold>Never diminishing, always golden"),
                MiniMessage.miniMessage().deserialize(" "),
                MiniMessage.miniMessage().deserialize("<gold><bold>Obtain by eating golden carrots:"),
                MiniMessage.miniMessage().deserialize("<yellow>  ◦ <reset><gray>Golden Carrot: 1/500 chance per bite"),
                MiniMessage.miniMessage().deserialize("<yellow>  ◦ <reset><gray>Guaranteed at 500 count")));
        
        setItemModel("lumen:eternal_golden_carrot");
        setUnbreakable(true);
        setEnhancable(false);
        
        // No recipe - special drop
        setRecipe(null);
    }
}
