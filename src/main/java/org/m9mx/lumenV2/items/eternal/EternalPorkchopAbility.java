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
 * Handles the mechanics of the Eternal Porkchop.
 * Tracks pork consumption and grants Eternal Porkchop at 500 eats or 1/500 RNG.
 */
public class EternalPorkchopAbility implements Listener {
    private final JavaPlugin plugin;
    private final MiniMessage miniMessage;
    private final NamespacedKey porkCountKey;
    private final NamespacedKey porkAttemptKey;

    public EternalPorkchopAbility(JavaPlugin plugin) {
        this.plugin = plugin;
        this.miniMessage = MiniMessage.miniMessage();
        this.porkCountKey = new NamespacedKey(plugin, "pork_eaten_count");
        this.porkAttemptKey = new NamespacedKey(plugin, "pork_total_attempts");
    }

    @EventHandler
    public void onPlayerItemConsume(PlayerItemConsumeEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        // Check if it's an eternal porkchop (has the custom item ID)
        if (isEternalPorkchop(item)) {
            // Eternal porkchop can be eaten infinitely, don't increment counter
            // Put the item back in mainhand after consumption
            
            // Schedule task to put item back (happens after event completes)
            Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () -> {
                ItemStack eternalPork = ItemRegistry.getInstance().createItem("eternal_porkchop");
                if (eternalPork != null) {
                    player.getInventory().setItemInMainHand(eternalPork);
                }
            }, 1L);
            
            return;
        }

        // Only track raw pork and cooked pork
        int increment = 0;
        double triggerChance = 0;
        
        if (item.getType() == Material.PORKCHOP) {
            // Raw pork: +1 count, 0.2% chance
            increment = 1;
            triggerChance = 1.0 / 500.0;
        } else if (item.getType() == Material.COOKED_PORKCHOP) {
            // Cooked pork: +2 count, 0.4% chance
            increment = 2;
            triggerChance = 1.0 / 250.0;
        } else {
            // Other food - ignore
            return;
        }
        
        int currentCount = getPorkCount(player);
        int totalAttempts = getTotalAttempts(player);
        
        currentCount += increment;
        totalAttempts += increment;
        
        setPorkCount(player, currentCount);
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
            grantEternalPorkchop(player, currentCount, attemptNumber);
            setPorkCount(player, 0); // Reset counter
            setTotalAttempts(player, 0); // Reset attempts
        }
    }

    /**
     * Check if an item is the eternal porkchop by verifying custom item ID
     */
    private boolean isEternalPorkchop(ItemStack item) {
        if (!item.hasItemMeta()) {
            return false;
        }

        PersistentDataContainer pdc = item.getItemMeta().getPersistentDataContainer();
        NamespacedKey itemIdKey = new NamespacedKey("lumen", "item_id");
        String itemId = pdc.get(itemIdKey, PersistentDataType.STRING);

        return "eternal_porkchop".equals(itemId);
    }

    /**
     * Get the pork eaten count for a player
     */
    private int getPorkCount(Player player) {
        PersistentDataContainer pdc = player.getPersistentDataContainer();
        Integer count = pdc.get(porkCountKey, PersistentDataType.INTEGER);
        return count != null ? count : 0;
    }

    /**
     * Set the pork eaten count for a player
     */
    private void setPorkCount(Player player, int count) {
        PersistentDataContainer pdc = player.getPersistentDataContainer();
        pdc.set(porkCountKey, PersistentDataType.INTEGER, count);
    }

    /**
     * Get total attempts for a player (cumulative)
     */
    private int getTotalAttempts(Player player) {
        PersistentDataContainer pdc = player.getPersistentDataContainer();
        Integer attempts = pdc.get(porkAttemptKey, PersistentDataType.INTEGER);
        return attempts != null ? attempts : 0;
    }

    /**
     * Set total attempts for a player
     */
    private void setTotalAttempts(Player player, int attempts) {
        PersistentDataContainer pdc = player.getPersistentDataContainer();
        pdc.set(porkAttemptKey, PersistentDataType.INTEGER, attempts);
    }

    /**
     * Grant eternal porkchop to player and display celebration
     */
    private void grantEternalPorkchop(Player player, int totalEaten, int attemptNumber) {
        // Create eternal porkchop item from registry (applies all tags automatically)
        ItemStack eternalPork = ItemRegistry.getInstance().createItem("eternal_porkchop");
        
        if (eternalPork == null) {
            return;
        }

        // Give item to player inventory
        player.getInventory().addItem(eternalPork);

        // Play celebratory sounds
        player.playSound(player.getLocation(), "ui.toast.challenge_complete", 1.0F, 1.0F);
        player.playSound(player.getLocation(), "entity.player.levelup", 0.8F, 1.2F);

        // Send message
        Component message = miniMessage.deserialize(
                "<gold><bold>✨ You have obtained an <yellow>Eternal Porkchop<gold>! " +
                "Lucky attempt <yellow>" + attemptNumber + "<gold>/<yellow>500");
        player.sendMessage(message);

        // Broadcast to nearby players
        for (Player nearby : Bukkit.getOnlinePlayers()) {
            if (nearby.getWorld().equals(player.getWorld()) &&
                    nearby.getLocation().distance(player.getLocation()) <= 32 &&
                    !nearby.equals(player)) {
                Component broadcast = miniMessage.deserialize(
                        "<gold>" + player.getName() + " <yellow>has obtained an <gold><bold>Eternal Porkchop!");
                nearby.sendMessage(broadcast);
            }
        }
    }

    /**
     * Reset cooldowns for a specific player (for commands, etc.)
     */
    public void resetPorkCount(Player player) {
        setPorkCount(player, 0);
    }

    /**
     * Get the pork count for a player
     */
    public int getPlayerPorkCount(Player player) {
        return getPorkCount(player);
    }
}
