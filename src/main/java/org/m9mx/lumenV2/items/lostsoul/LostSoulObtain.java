package org.m9mx.lumenV2.items.lostsoul;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.potion.PotionEffectType;
import org.m9mx.lumenV2.Lumen;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class LostSoulObtain implements Listener {

    private static LostSoulObtain instance;
    private final Set<UUID> soulCooldown = new HashSet<>();
    private final Lumen plugin;

    public LostSoulObtain() {
        this.plugin = Lumen.getPlugin(Lumen.class);
        instance = this;
    }

    public static LostSoulObtain getInstance() {
        return instance;
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        Location from = event.getFrom();
        Location to = event.getTo();

        // Check if the player actually moved to a different block
        if (to.getBlockX() == from.getBlockX() && to.getBlockZ() == from.getBlockZ()) {
            return;
        }

        Block standingOn = player.getLocation().getBlock().getRelative(0, -1, 0);

        // Check if the block underneath is soul sand or soul soil
        if (isSoulSandBlock(standingOn.getType())) {
            PlayerInventory inventory = player.getInventory();
            ItemStack boots = inventory.getBoots();

            // Check if the player is wearing boots with Soul Speed enchantment
            if (boots != null && boots.hasItemMeta() && 
                boots.getItemMeta().hasEnchant(org.bukkit.enchantments.Enchantment.SOUL_SPEED)) {
                
                if (!soulCooldown.contains(player.getUniqueId())) {
                    if (Math.random() < 0.03) {
                        giveLostSoul(player);

                            soulCooldown.add(player.getUniqueId());
                            Bukkit.getScheduler().runTaskLater(plugin,
                                () -> soulCooldown.remove(player.getUniqueId()),
                                20 * 30 // 30 seconds
                        );
                    }
                }
            }
        }
    }

    private boolean isSoulSandBlock(Material material) {
        return material == Material.SOUL_SAND || material == Material.SOUL_SOIL;
    }

    private void giveLostSoul(Player player) {
        // Get the LostSoul item from the registry
        org.m9mx.lumenV2.item.ItemRegistry registry = org.m9mx.lumenV2.item.ItemRegistry.getInstance();
        org.m9mx.lumenV2.item.CustomItem lostSoulItem = registry.getItem("lost_soul");
        
        if (lostSoulItem != null) {
            ItemStack lostSoulStack = lostSoulItem.build();
            player.getInventory().addItem(lostSoulStack);
            
            // Send a message to the player
            player.sendMessage(org.bukkit.ChatColor.AQUA + "You disturbed a soul and captured a " + 
                              org.bukkit.ChatColor.DARK_AQUA + "Lost Soul" + 
                              org.bukkit.ChatColor.AQUA + "!");
            
            // Play a sound to notify the player
            player.playSound(player.getLocation(), "entity.ghast.scream", 0.4F, 1.2F);
        } else {
            // Log error if item not found
            org.bukkit.Bukkit.getLogger().warning("Could not find lost_soul item in registry");
        }
    }
}