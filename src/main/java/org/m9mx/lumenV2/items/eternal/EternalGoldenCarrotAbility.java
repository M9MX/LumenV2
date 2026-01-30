package org.m9mx.lumenV2.items.eternal;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.m9mx.lumenV2.item.ItemRegistry;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;

/**
 * Handles the mechanics of the Eternal Golden Carrot.
 * Tracks golden carrot consumption and grants Eternal Golden Carrot at 500 eats or 1/500 RNG.
 * Cannot be obtained from normal carrots.
 */
public class EternalGoldenCarrotAbility implements Listener {
    private final JavaPlugin plugin;
    private final MiniMessage miniMessage;
    private final NamespacedKey carrotCountKey;
    private final NamespacedKey carrotAttemptKey;

    public EternalGoldenCarrotAbility(JavaPlugin plugin) {
        this.plugin = plugin;
        this.miniMessage = MiniMessage.miniMessage();
        this.carrotCountKey = new NamespacedKey(plugin, "golden_carrot_eaten_count");
        this.carrotAttemptKey = new NamespacedKey(plugin, "golden_carrot_total_attempts");
    }

    @EventHandler
    public void onPlayerItemConsume(PlayerItemConsumeEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        // Check if it's an eternal golden carrot (has the custom item ID)
        if (isEternalGoldenCarrot(item)) {
            // Eternal golden carrot can be eaten infinitely, don't increment counter
            // Put the item back in mainhand after consumption
            
            // Schedule task to put item back (happens after event completes)
            Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () -> {
                ItemStack eternalCarrot = ItemRegistry.getInstance().createItem("eternal_golden_carrot");
                if (eternalCarrot != null) {
                    player.getInventory().setItemInMainHand(eternalCarrot);
                }
            }, 1L);
            
            return;
        }

        // Only track golden carrots (NOT normal carrots)
        if (item.getType() != Material.GOLDEN_CARROT) {
            return;
        }
        
        int currentCount = getCarrotCount(player);
        int totalAttempts = getTotalAttempts(player);
        
        currentCount += 1;
        totalAttempts += 1;
        
        setCarrotCount(player, currentCount);
        setTotalAttempts(player, totalAttempts);

        // Check if trigger condition is met
        boolean triggered = currentCount >= 500;
        int attemptNumber = totalAttempts;
        
        // Single RNG check (1/500 chance)
        if (!triggered && Math.random() < (1.0 / 500.0)) {
            triggered = true;
            attemptNumber = totalAttempts;
        }

        if (triggered) {
            grantEternalGoldenCarrot(player, currentCount, attemptNumber);
            setCarrotCount(player, 0); // Reset counter
            setTotalAttempts(player, 0); // Reset attempts
        }
    }

    /**
     * Check if an item is the eternal golden carrot by verifying custom item ID
     */
    private boolean isEternalGoldenCarrot(ItemStack item) {
        if (!item.hasItemMeta()) {
            return false;
        }

        PersistentDataContainer pdc = item.getItemMeta().getPersistentDataContainer();
        NamespacedKey itemIdKey = new NamespacedKey("lumen", "item_id");
        String itemId = pdc.get(itemIdKey, PersistentDataType.STRING);

        return "eternal_golden_carrot".equals(itemId);
    }

    /**
     * Get the golden carrot eaten count for a player
     */
    private int getCarrotCount(Player player) {
        PersistentDataContainer pdc = player.getPersistentDataContainer();
        Integer count = pdc.get(carrotCountKey, PersistentDataType.INTEGER);
        return count != null ? count : 0;
    }

    /**
     * Set the golden carrot eaten count for a player
     */
    private void setCarrotCount(Player player, int count) {
        PersistentDataContainer pdc = player.getPersistentDataContainer();
        pdc.set(carrotCountKey, PersistentDataType.INTEGER, count);
    }

    /**
     * Get total attempts for a player (cumulative)
     */
    private int getTotalAttempts(Player player) {
        PersistentDataContainer pdc = player.getPersistentDataContainer();
        Integer attempts = pdc.get(carrotAttemptKey, PersistentDataType.INTEGER);
        return attempts != null ? attempts : 0;
    }

    /**
     * Set total attempts for a player
     */
    private void setTotalAttempts(Player player, int attempts) {
        PersistentDataContainer pdc = player.getPersistentDataContainer();
        pdc.set(carrotAttemptKey, PersistentDataType.INTEGER, attempts);
    }

    /**
     * Grant eternal golden carrot to player and display celebration
     */
    private void grantEternalGoldenCarrot(Player player, int totalEaten, int attemptNumber) {
        // Create eternal golden carrot item from registry (applies all tags automatically)
        ItemStack eternalCarrot = ItemRegistry.getInstance().createItem("eternal_golden_carrot");
        
        if (eternalCarrot == null) {
            return;
        }

        // Give item to player inventory
        player.getInventory().addItem(eternalCarrot);

        // Play celebratory sounds
        player.playSound(player.getLocation(), "ui.toast.challenge_complete", 1.0F, 1.0F);
        player.playSound(player.getLocation(), "entity.player.levelup", 0.8F, 1.2F);

        // Send message
        Component message = miniMessage.deserialize(
                "<gold><bold>✨ You have obtained an <yellow>Eternal Golden Carrot<gold>! " +
                "Lucky attempt <yellow>" + attemptNumber + "<gold>/<yellow>500");
        player.sendMessage(message);

        // Broadcast to nearby players
        for (Player nearby : Bukkit.getOnlinePlayers()) {
            if (nearby.getWorld().equals(player.getWorld()) &&
                    nearby.getLocation().distance(player.getLocation()) <= 32 &&
                    !nearby.equals(player)) {
                Component broadcast = miniMessage.deserialize(
                        "<gold>" + player.getName() + " <yellow>has obtained an <gold><bold>Eternal Golden Carrot!");
                nearby.sendMessage(broadcast);
            }
        }
    }

    /**
     * Reset cooldowns for a specific player (for commands, etc.)
     */
    public void resetCarrotCount(Player player) {
        setCarrotCount(player, 0);
    }

    /**
     * Get the carrot count for a player
     */
    public int getPlayerCarrotCount(Player player) {
        return getCarrotCount(player);
    }
}
