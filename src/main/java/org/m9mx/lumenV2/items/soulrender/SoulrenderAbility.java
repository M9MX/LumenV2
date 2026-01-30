package org.m9mx.lumenV2.items.soulrender;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.AbstractHorse;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Skeleton;
import org.bukkit.entity.SkeletonHorse;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.event.vehicle.VehicleExitEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.m9mx.lumenV2.config.ConfigManager;
import org.m9mx.lumenV2.systems.CooldownBossBarManager;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;

/**
 * Handles the abilities of the Soulrender item.
 * Right-click: Summons a skeleton horse (reusing DeadCompanionAbility horse summon logic)
 * Passive: Drop player skull when killing a player
 */
public class SoulrenderAbility implements Listener {
    private final Map<UUID, Long> horseCooldowns = new HashMap<>();
    private final Map<UUID, Long> horseMountTime = new HashMap<>();
    private final Map<UUID, AbstractHorse> activeHorses = new HashMap<>(); // Track active horses

    private final NamespacedKey isTeammateKey;

    private final Random random = new Random();
    private final JavaPlugin plugin;
    private final MiniMessage miniMessage;
    private final CooldownBossBarManager cooldownBossBarManager;

    public SoulrenderAbility(JavaPlugin plugin) {
        this.plugin = plugin;
        this.miniMessage = MiniMessage.miniMessage();
        this.cooldownBossBarManager = CooldownBossBarManager.getInstance();
        this.isTeammateKey = new NamespacedKey(plugin, "is_teammate");
    }

    // Dynamic config getters - read fresh values on each use
    private int getHorseDuration() {
        return ConfigManager.getInstance().getItemsConfig().getInt("soulrender.horse.duration", 90);
    }

    private int getHorseCooldown() {
        return ConfigManager.getInstance().getItemsConfig().getInt("soulrender.horse.cooldown", 180);
    }

    private double getHorseHealth() {
        return ConfigManager.getInstance().getItemsConfig().getConfig().getDouble("soulrender.horse.health", 25.0);
    }

    private double getHorseSpeed() {
        return ConfigManager.getInstance().getItemsConfig().getConfig().getDouble("soulrender.horse.speed", 16.0);
    }

    /**
     * Resets cooldowns for a specific player
     * 
     * @param playerId The player's UUID
     */
    public void resetCooldowns(UUID playerId) {
        horseCooldowns.put(playerId, 0L);

        // Remove any active cooldown boss bars for this player
        cooldownBossBarManager.removeCooldown(playerId, "soulrender-horse");
    }

    /**
     * Manually reset cooldowns for a player via command or other systems
     * 
     * @param player The player whose cooldowns should be reset
     */
    public void resetCooldowns(Player player) {
        resetCooldowns(player.getUniqueId());
    }

    /**
     * Gets the remaining cooldown times for a player
     * 
     * @param playerId The player's UUID
     * @return Array of [horseCooldownRemaining]
     */
    public long[] getCooldownsForPlayer(UUID playerId) {
        long currentTime = System.currentTimeMillis() / 1000;
        long horseCooldown = horseCooldowns.getOrDefault(playerId, 0L);

        long horseRemaining = Math.max(0, horseCooldown - currentTime);

        return new long[] { horseRemaining };
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();

        if (!SoulrenderItem.isSoulrender(item)) {
            return;
        }

        if (event.getAction() == Action.RIGHT_CLICK_BLOCK || event.getAction() == Action.RIGHT_CLICK_AIR) {
            event.setCancelled(true);
            handleHorseSummon(player);
        }
    }

    // Handle when player interacts with entities (like trying to mount the horse)
    @EventHandler
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        if (event.getRightClicked() instanceof AbstractHorse) {
            AbstractHorse horse = (AbstractHorse) event.getRightClicked();
            Player player = event.getPlayer();

            // Check if this is one of our active horses
            if (activeHorses.containsValue(horse)) {
                // Prevent players from mounting the horse manually if it's already active
                event.setCancelled(true);
            }
        }
    }

    // Handle when the horse takes damage (to prevent other players from killing it)
    @EventHandler
    public void onHorseDamage(EntityDamageByEntityEvent event) {
        if (event.getEntity() instanceof AbstractHorse) {
            AbstractHorse horse = (AbstractHorse) event.getEntity();

            // Check if this is one of our active horses
            if (activeHorses.containsValue(horse)) {
                // For now, we'll just cancel the damage to prevent others from killing it
                event.setCancelled(true);
            }
        }
    }

    // Handle when player dismounts from the horse
    @EventHandler
    public void onPlayerDismount(PlayerToggleSneakEvent event) {
        Player player = event.getPlayer();

        // Check if player is currently riding an active horse
        if (player.isInsideVehicle()) {
            Entity vehicle = player.getVehicle();
            if (vehicle instanceof AbstractHorse) {
                UUID playerId = player.getUniqueId();

                // Check if this is one of our active horses
                if (activeHorses.containsKey(playerId) && activeHorses.get(playerId).equals(vehicle)) {
                    handleHorseDismount(player, (AbstractHorse) vehicle);
                }
            }
        }
    }

    // Alternative event to catch when a player leaves a vehicle
    @EventHandler
    public void onVehicleExit(VehicleExitEvent event) {
        if (event.getVehicle() instanceof AbstractHorse) {
            AbstractHorse horse = (AbstractHorse) event.getVehicle();
            // Check if this is one of our active horses
            for (Map.Entry<UUID, AbstractHorse> entry : activeHorses.entrySet()) {
                if (entry.getValue().equals(horse)) {
                    Player player = (Player) event.getExited();
                    handleHorseDismount(player, horse);
                    break;
                }
            }
        }
    }

    // Handle player death when Soulrender is the killer
    @EventHandler
    public void onPlayerKilledWithSoulrender(EntityDeathEvent event) {
        if (event.getEntity() instanceof Player && event instanceof PlayerDeathEvent) {
            Player killedPlayer = (Player) event.getEntity();
            
            // Check if the damager was a player using Soulrender
            if (killedPlayer.getLastDamageCause() instanceof EntityDamageByEntityEvent damageEvent) {
                if (damageEvent.getDamager() instanceof Player killer && 
                    damageEvent.getDamager().getWorld().equals(killedPlayer.getWorld()) &&
                    damageEvent.getDamager().getLocation().distanceSquared(killedPlayer.getLocation()) <= 64) { // Within 8 blocks
                    
                    ItemStack weapon = killer.getInventory().getItemInMainHand();
                    if (SoulrenderItem.isSoulrender(weapon)) {
                        // Drop a player skull with the victim's name
                        ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
                        // Set the skull to the player's head
                        skull.editMeta(meta -> {
                            if (meta instanceof org.bukkit.inventory.meta.SkullMeta skullMeta) {
                                skullMeta.setOwnerProfile(killedPlayer.getPlayerProfile());
                            }
                        });
                        
                        // Set display name to [Victim Name]
                        skull.editMeta(meta -> {
                            meta.displayName(miniMessage.deserialize("<white>[" + killedPlayer.getName() + "]"));
                        });
                        
                        // Drop the skull at the location of the killed player
                        killedPlayer.getWorld().dropItemNaturally(killedPlayer.getLocation(), skull);
                        
                        // Play sound effects when a player skull is dropped
                        killer.playSound(killer.getLocation(), "entity.wither.spawn", 0.7F, 0.8F);
                        killer.playSound(killer.getLocation(), "entity.ender_dragon.death", 0.6F, 1.2F);
                    }
                }
            }
        }
    }

    private void handleHorseSummon(Player player) {
        UUID playerId = player.getUniqueId();
        long currentTime = System.currentTimeMillis() / 1000;
        long horseCooldown = horseCooldowns.getOrDefault(playerId, 0L);

        if (currentTime < horseCooldown) {
            long remaining = horseCooldown - currentTime;
            Component message = miniMessage
                    .deserialize("<red>The Soulrender is still on cooldown! " + remaining + " seconds remaining.");
            player.sendMessage(message);
            return;
        }

        // Check if player already has a horse active
        if (activeHorses.containsKey(playerId)) {
            // Player clicked again while riding, despawn the horse
            AbstractHorse horse = activeHorses.get(playerId);
            if (horse != null && horse.isValid()) {
                handleHorseDismount(player, horse);
            }
            return;
        }

        // Calculate remaining time if player dismounts early (for cooldown calculation)
        Long mountTime = horseMountTime.get(playerId);
        if (mountTime != null) {
            long timeRidden = currentTime - mountTime;
            if (timeRidden < getHorseDuration()) {
                long oldCooldown = horseCooldowns.getOrDefault(playerId, 0L);
                long timeSinceCooldownStart = currentTime - (oldCooldown - getHorseCooldown());
                long remainingCooldown = getHorseCooldown() - timeSinceCooldownStart;

                // Apply the formula: remainingCooldown = oldCooldown - 2 * (duration -
                // timeRidden)
                long newCooldown = Math.max(0, remainingCooldown - 2 * (getHorseDuration() - timeRidden));

                horseCooldowns.put(playerId, currentTime + newCooldown);
            }
            horseMountTime.remove(playerId); // Remove the old mount time
        }

        // Summon skeleton horse
        Location location = player.getLocation();
        SkeletonHorse horse = (SkeletonHorse) player.getWorld().spawnEntity(location, EntityType.SKELETON_HORSE);
        horse.setTamed(true); // Must be tamed to be rideable
        horse.setAdult();
        horse.setAI(true);
        horse.setCanPickupItems(false);

        // Set horse health and speed from config
        horse.getAttribute(Attribute.MAX_HEALTH).setBaseValue(getHorseHealth());
        horse.setHealth(getHorseHealth());

        // Set horse movement speed from config (convert from blocks/second to attribute
        // value)
        double horseSpeedAttribute = convertSpeedToAttribute(getHorseSpeed());
        horse.getAttribute(Attribute.MOVEMENT_SPEED).setBaseValue(horseSpeedAttribute);

        // Add a saddle to the horse
        horse.getInventory().setSaddle(new ItemStack(Material.SADDLE));

        // Make the player mount the horse immediately
        horse.addPassenger(player);

        // Play sound when horse is summoned
        player.playSound(player.getLocation(), "entity.horse.ambient", 0.8F, 1.0F);
        
        // Play additional sound for atmosphere
        player.playSound(player.getLocation(), "entity.skeleton_horse.ambient", 0.8F, 1.0F);

        // Store mount time and active horse
        horseMountTime.put(playerId, currentTime);
        activeHorses.put(playerId, horse);

        // Start particle trail for the horse
        startHorseParticleTrail(horse);

        // Show cooldown boss bar for horse ability (full cooldown duration)
        // Using BLUE for Soulrender theme with dark_aqua title using legacy color codes
        String title = "§3§lSoulrender: Horse Summon";  // §3 is dark aqua, §l is bold
        cooldownBossBarManager.startCooldown(player, "soulrender-horse", title, getHorseCooldown() * 1000L,
                org.bukkit.boss.BarColor.BLUE);

        // Set up horse despawn when dismounted or after duration
        new BukkitRunnable() {
            @Override
            public void run() {
                if (horse.isValid()) {
                    // Check if player is still riding
                    if (horse.getPassengers().contains(player)) {
                        // Player is still riding, remove them and despawn
                        horse.eject();
                    }

                    // Calculate cooldown based on time ridden
                    Long mountTime = horseMountTime.get(playerId);
                    if (mountTime != null) {
                        long timeRidden = System.currentTimeMillis() / 1000 - mountTime;

                        // Calculate new cooldown based on time ridden
                        // Formula: remainingCooldown = cooldown - 2 * (duration - timeRidden)
                        long remainingTime = Math.max(0, getHorseDuration() - timeRidden);
                        long newCooldown = Math.max(0, getHorseCooldown() - 2 * remainingTime);

                        // Update cooldown
                        horseCooldowns.put(playerId, System.currentTimeMillis() / 1000 + newCooldown);

                        // Since there's no updateCooldown method, we'll just remove and restart the
                        // boss bar
                        cooldownBossBarManager.removeCooldown(player, "soulrender-horse");
                        String updatedTitle = "§3§lSoulrender: Horse Summon";  // §3 is dark aqua, §l is bold
                        cooldownBossBarManager.startCooldown(player, "soulrender-horse", updatedTitle,
                                newCooldown * 1000L, org.bukkit.boss.BarColor.BLUE);

                        // Remove from tracking
                        horseMountTime.remove(playerId);
                    }

                    horse.remove();

                    // Remove from active horses
                    activeHorses.remove(playerId);

                    Component message = miniMessage
                            .deserialize("<white>Your skeleton horse has returned to the nether.");
                    player.sendMessage(message);

                    // Update the Soulrender item's lore when horse despawns
                    Player p = Bukkit.getPlayer(playerId);
                    if (p != null) {
                        ItemStack itemInHand = p.getInventory().getItemInMainHand();
                        if (itemInHand.hasItemMeta()) {
                            org.bukkit.inventory.meta.ItemMeta meta = itemInHand.getItemMeta();
                            PersistentDataContainer pdc = meta.getPersistentDataContainer();
                            NamespacedKey itemIdKey = new NamespacedKey("lumen", "item_id");
                            String storedId = pdc.get(itemIdKey, PersistentDataType.STRING);

                            if (storedId != null && storedId.equals("soulrender")) {
                                // In LumenV2, we would need to update the item lore differently
                                // For now, we'll just send a message
                            }
                        }
                    }
                }
            }
        }.runTaskLater(plugin, getHorseDuration() * 20L); // 20 ticks per second

        Component message = miniMessage.deserialize("<white>You have summoned a skeleton horse!");
        player.sendMessage(message);
    }

    private void handleHorseDismount(Player player, AbstractHorse horse) {
        UUID playerId = player.getUniqueId();
        long currentTime = System.currentTimeMillis() / 1000;

        // Get the mount time to calculate how long the player was riding
        Long mountTime = horseMountTime.get(playerId);
        if (mountTime != null) {
            long timeRidden = currentTime - mountTime;

            // Calculate new cooldown based on time ridden
            // Formula: remainingCooldown = cooldown - 2 * (duration - timeRidden)
            long remainingTime = Math.max(0, getHorseDuration() - timeRidden);
            long newCooldown = Math.max(0, getHorseCooldown() - 2 * remainingTime);

            // Update cooldown
            horseCooldowns.put(playerId, currentTime + newCooldown);

            // Since there's no updateCooldown method, we'll just remove and restart the
            // boss bar
            cooldownBossBarManager.removeCooldown(player, "soulrender-horse");
            String title = "§3§lSoulrender: Horse Summon";  // §3 is dark aqua, §l is bold
            cooldownBossBarManager.startCooldown(player, "soulrender-horse", title, newCooldown * 1000L,
                    org.bukkit.boss.BarColor.BLUE);

            // Remove from tracking
            horseMountTime.remove(playerId);
            activeHorses.remove(playerId);

            // Remove the horse
            horse.remove();

            Component message = miniMessage.deserialize("<white>Your skeleton horse has returned to the nether.");
            player.sendMessage(message);
        }
    }

    private void startHorseParticleTrail(AbstractHorse horse) {
        // Schedule a repeating task to create particle trail behind the horse
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!horse.isValid()) {
                    // Horse is no longer valid, cancel this task
                    cancel();
                    return;
                }

                Location horseLoc = horse.getLocation();
                World world = horseLoc.getWorld();

                if (world != null) {
                    // Create a soul-like trail particle behind the horse
                    // Offset the location slightly behind and below the horse
                    Location particleLoc = horseLoc.clone().subtract(
                            horseLoc.getDirection().multiply(1.5) // 1.5 blocks behind the horse
                    );
                    particleLoc.add(0, -0.5, 0); // slightly below

                    // Spawn soul fire flame particles for a soul-like effect
                    world.spawnParticle(
                            Particle.SOUL_FIRE_FLAME,
                            particleLoc,
                            2, // number of particles
                            0.2, 0.2, 0.2, // offset (x, y, z)
                            0.1 // extra data (speed)
                    );

                    // Also add some smoke particles for a fuller effect
                    world.spawnParticle(
                            Particle.SMOKE,
                            particleLoc,
                            1, // number of particles
                            0.1, 0.1, 0.1, // offset (x, y, z)
                            0.05 // extra data (speed)
                    );
                }

                // Check if this horse is still in the active horses map
                if (!activeHorses.containsValue(horse)) {
                    // Horse is no longer active, cancel this task
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 0L, 2L); // Every 2 ticks (10x per second)
    }

    private double convertSpeedToAttribute(double blocksPerSecond) {
        // Convert from blocks per second to movement attribute value
        // Default walking speed is 0.1 (4.317 blocks/second)
        // So the conversion is: desired_speed / 43.17 to get the attribute value
        // For 16 b/s: 16 / 43.17 ≈ 0.37
        // For 6 b/s: 6 / 43.17 ≈ 0.14
        return blocksPerSecond / 43.17;
    }
}