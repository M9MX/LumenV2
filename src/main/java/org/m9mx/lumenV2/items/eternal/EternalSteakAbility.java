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
 * Handles the mechanics of the Eternal Steak.
 * Tracks normal steak consumption and grants Eternal Steak at 5000 eats or 1/5000 RNG.
 */
public class EternalSteakAbility implements Listener {
    private final JavaPlugin plugin;
    private final MiniMessage miniMessage;
    private final NamespacedKey steakCountKey;
    private final NamespacedKey steakAttemptKey;

    public EternalSteakAbility(JavaPlugin plugin) {
        this.plugin = plugin;
        this.miniMessage = MiniMessage.miniMessage();
        this.steakCountKey = new NamespacedKey(plugin, "steak_eaten_count");
        this.steakAttemptKey = new NamespacedKey(plugin, "steak_total_attempts");
    }

    @EventHandler
    public void onPlayerItemConsume(PlayerItemConsumeEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        // Check if it's an eternal steak (has the custom item ID)
        if (isEternalSteak(item)) {
            // Eternal steak can be eaten infinitely, don't increment counter
            // Put the item back in mainhand after consumption
            
            // Schedule task to put item back (happens after event completes)
            Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () -> {
                ItemStack eternalSteak = ItemRegistry.getInstance().createItem("eternal_steak");
                if (eternalSteak != null) {
                    player.getInventory().setItemInMainHand(eternalSteak);
                }
            }, 1L);
            
            return;
        }

        // Only track raw beef and cooked beef
        int increment = 0;
        double triggerChance = 0;
        
        if (item.getType() == Material.BEEF) {
            // Raw beef: +1 count, 0.2% chance
            increment = 1;
            triggerChance = 1.0 / 500.0;
        } else if (item.getType() == Material.COOKED_BEEF) {
            // Cooked beef: +2 count, 0.4% chance
            increment = 2;
            triggerChance = 1.0 / 250.0;
        } else {
            // Other food - ignore
            return;
        }
        
        int currentCount = getSteakCount(player);
        int totalAttempts = getTotalAttempts(player);
        
        currentCount += increment;
        totalAttempts += increment;
        
        setSteakCount(player, currentCount);
        setTotalAttempts(player, totalAttempts);

        // Check if trigger condition is met
        boolean triggered = currentCount >= 500;
        int attemptNumber = totalAttempts;
        
        // Single RNG check
        if (!triggered && Math.random() < triggerChance) {
            triggered = true;
            attemptNumber = totalAttempts;
        }

        if (triggered) {
            grantEternalSteak(player, currentCount, attemptNumber);
            setSteakCount(player, 0); // Reset counter
            setTotalAttempts(player, 0); // Reset attempts
        }
    }

    /**
     * Check if an item is the eternal steak by verifying custom item ID
     */
    private boolean isEternalSteak(ItemStack item) {
        if (!item.hasItemMeta()) {
            return false;
        }

        PersistentDataContainer pdc = item.getItemMeta().getPersistentDataContainer();
        NamespacedKey itemIdKey = new NamespacedKey("lumen", "item_id");
        String itemId = pdc.get(itemIdKey, PersistentDataType.STRING);

        return "eternal_steak".equals(itemId);
    }

    /**
     * Get the steak eaten count for a player
     */
    private int getSteakCount(Player player) {
        PersistentDataContainer pdc = player.getPersistentDataContainer();
        Integer count = pdc.get(steakCountKey, PersistentDataType.INTEGER);
        return count != null ? count : 0;
    }

    /**
     * Set the steak eaten count for a player
     */
    private void setSteakCount(Player player, int count) {
        PersistentDataContainer pdc = player.getPersistentDataContainer();
        pdc.set(steakCountKey, PersistentDataType.INTEGER, count);
    }

    /**
     * Get total attempts for a player (cumulative)
     */
    private int getTotalAttempts(Player player) {
        PersistentDataContainer pdc = player.getPersistentDataContainer();
        Integer attempts = pdc.get(steakAttemptKey, PersistentDataType.INTEGER);
        return attempts != null ? attempts : 0;
    }

    /**
     * Set total attempts for a player
     */
    private void setTotalAttempts(Player player, int attempts) {
        PersistentDataContainer pdc = player.getPersistentDataContainer();
        pdc.set(steakAttemptKey, PersistentDataType.INTEGER, attempts);
    }

    /**
     * Grant eternal steak to player and display celebration
     */
    private void grantEternalSteak(Player player, int totalEaten, int attemptNumber) {
        // Create eternal steak item from registry (applies all tags automatically)
        ItemStack eternalSteak = ItemRegistry.getInstance().createItem("eternal_steak");
        
        if (eternalSteak == null) {
            return;
        }

        // Give item to player inventory
        player.getInventory().addItem(eternalSteak);

        // Play celebratory sounds
        player.playSound(player.getLocation(), "ui.toast.challenge_complete", 1.0F, 1.0F);
        player.playSound(player.getLocation(), "entity.player.levelup", 0.8F, 1.2F);

        // Send message
        Component message = miniMessage.deserialize(
                "<gold><bold>✨ You have obtained an <yellow>Eternal Steak<gold>! " +
                "Lucky attempt <yellow>" + attemptNumber + "<gold>/<yellow>500");
        player.sendMessage(message);

        // Broadcast to nearby players
        for (Player nearby : Bukkit.getOnlinePlayers()) {
            if (nearby.getWorld().equals(player.getWorld()) &&
                    nearby.getLocation().distance(player.getLocation()) <= 32 &&
                    !nearby.equals(player)) {
                Component broadcast = miniMessage.deserialize(
                        "<gold>" + player.getName() + " <yellow>has obtained an <gold><bold>Eternal Steak!");
                nearby.sendMessage(broadcast);
            }
        }
    }

    /**
     * Reset cooldowns for a specific player (for commands, etc.)
     */
    public void resetSteakCount(Player player) {
        setSteakCount(player, 0);
    }

    /**
     * Get the steak count for a player
     */
    public int getPlayerSteakCount(Player player) {
        return getSteakCount(player);
    }
}
