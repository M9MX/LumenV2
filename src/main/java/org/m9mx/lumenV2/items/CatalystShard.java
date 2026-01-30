package org.m9mx.lumenV2.items;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.m9mx.lumenV2.item.CustomItem;
import org.m9mx.lumenV2.item.Recipe;
import org.m9mx.lumenV2.item.RecipeType;
import org.m9mx.lumenV2.systems.EnhancementSystem;
import java.util.Arrays;

/**
 * Catalyst Shard - used for item enhancements
 */
public class CatalystShard extends CustomItem {

    public CatalystShard() {
        super("catalyst_shard");
    }

    @Override
    protected void initialize() {
        setMaterial(Material.AMETHYST_SHARD);
        setDisplayName(Component.text("Catalyst Shard", TextColor.color(0xB24BF3), TextDecoration.BOLD));
        setLore(Arrays.asList(
                Component.text("Can be used to upgrade items", TextColor.color(0x9D4EDD), TextDecoration.BOLD)));
        setItemModel("lumen:catalyst_shard");

        // Only enable this item if the enhancement system is enabled
        setEnabled(EnhancementSystem.getInstance().isEnabled());

        // Create recipe
        Recipe recipe = new Recipe("catalyst_shard_recipe", RecipeType.NORMAL);

        // Grid layout (0,1,2 = top row; 3,4,5 = middle row; 6,7,8 = bottom row)
        // Row 0
        recipe.setSlot(0, 0, "GOLD_BLOCK", 8);
        recipe.setSlot(0, 1, "GOLDEN_APPLE", 1);
        recipe.setSlot(0, 2, "GOLD_BLOCK", 8);

        // Row 1
        recipe.setSlot(1, 0, "BLAZE_ROD", 8);
        recipe.setSlot(1, 1, "ECHO_SHARD", 1);
        recipe.setSlot(1, 2, "BLAZE_ROD", 8);

        // Row 2
        recipe.setSlot(2, 0, "GOLD_BLOCK", 8);
        recipe.setSlot(2, 1, "GOLDEN_APPLE", 1);
        recipe.setSlot(2, 2, "GOLD_BLOCK", 8);

        recipe.setResult("catalyst_shard");
        setRecipe(recipe);
    }
}
