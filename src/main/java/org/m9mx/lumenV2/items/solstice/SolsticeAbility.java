package org.m9mx.lumenV2.items.solstice;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.boss.BarColor;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.m9mx.lumenV2.config.ConfigManager;
import org.m9mx.lumenV2.systems.CooldownBossBarManager;
import org.m9mx.lumenV2.systems.trust.TrustHelper;
import org.m9mx.lumenV2.systems.enhancement.EnhancementManager;

/**
 * Handles Solstice abilities:
 * - Passive: Day/night cycle modulation of damage
 * - Active: Sunbeam ability (right-click)
 */
public class SolsticeAbility implements Listener {
    private final JavaPlugin plugin;
    private final EnhancementManager enhancementManager;
    private final CooldownBossBarManager cooldownManager;
    private final Map<UUID, Long> sunbeamCooldowns = new HashMap<>();
    private final Map<UUID, Long> lastClickTime = new HashMap<>();
    private final Map<UUID, Long> lastCooldownMessageTime = new HashMap<>();

    private int sunbeamCooldown;
    private int sunbeamRadius;
    private double sunbeamBaseDamage;

    public SolsticeAbility(JavaPlugin plugin) {
        this.plugin = plugin;
        this.enhancementManager = new EnhancementManager();
        this.cooldownManager = CooldownBossBarManager.getInstance();
        loadConfiguration();
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    private void loadConfiguration() {
        // Load from items.yml config or use defaults
        var itemsConfig = ConfigManager.getInstance().getItemsConfig();
        
        if (itemsConfig != null && itemsConfig.get("solstice") != null) {
            // Sunbeam configuration
            this.sunbeamCooldown = itemsConfig.getInt("solstice.sunbeam.cooldown", 120);
            this.sunbeamRadius = itemsConfig.getInt("solstice.sunbeam.radius", 3);
            this.sunbeamBaseDamage = itemsConfig.get("solstice.sunbeam.base_damage") != null ? 
                itemsConfig.getConfig().getDouble("solstice.sunbeam.base_damage", 6.0) : 6.0;
        } else {
            // Use hardcoded defaults if config not found
            this.sunbeamCooldown = 120;
            this.sunbeamRadius = 3;
            this.sunbeamBaseDamage = 6.0;
        }
    }

    public void reloadConfiguration() {
        loadConfiguration();
    }

    @EventHandler(priority = org.bukkit.event.EventPriority.HIGHEST)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getDamager();
        ItemStack item = player.getInventory().getItemInMainHand();

        if (!SolsticeItem.isSolstice(item)) {
            return;
        }

        // Apply day/night damage modulation
        applyDayNightModulation(event, player);
        
        // Ensure damage is allowed through
        event.setCancelled(false);
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        org.bukkit.event.block.Action action = event.getAction();
        if (!action.equals(org.bukkit.event.block.Action.RIGHT_CLICK_AIR)) {
            return;
        }


        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();

        if (!SolsticeItem.isSolstice(item)) {
            return;
        }

        if (player.isSneaking()) {
            return;
        }

        UUID playerId = player.getUniqueId();
        long now = System.currentTimeMillis();
        long lastClick = lastClickTime.getOrDefault(playerId, 0L);
        
        if (now - lastClick < 100) {
            return;
        }
        
        lastClickTime.put(playerId, now);
        event.setCancelled(true);
        castSunbeam(player, item);
    }

    /**
     * Applies the day/night passive damage modulation.
     * Night (18000-6000 ticks): +10% damage
     * Day (6000-18000 ticks): -10% damage
     */
    private void applyDayNightModulation(EntityDamageByEntityEvent event, Player player) {
        World world = player.getWorld();
        long time = world.getTime();

        double multiplier = 1.0;

        if (time >= 6000 && time < 18000) {
            // Day time: -10% damage
            multiplier = 0.9;
        } else {
            // Night time: +10% damage
            multiplier = 1.1;
        }

        event.setDamage(event.getDamage() * multiplier);
    }

    private void castSunbeam(Player player, ItemStack item) {
        UUID playerUUID = player.getUniqueId();
        long now = System.currentTimeMillis();
        long lastCast = sunbeamCooldowns.getOrDefault(playerUUID, 0L);
        long cooldownMs = sunbeamCooldown * 1000L;

        if (now - lastCast < cooldownMs) {
            // Only show message every 1 second to avoid spam
            long lastMessage = lastCooldownMessageTime.getOrDefault(playerUUID, 0L);
            if (now - lastMessage >= 1000) {
                long remainingMs = cooldownMs - (now - lastCast);
                long seconds = remainingMs / 1000;
                player.sendMessage("§cSunbeam is on cooldown for §6" + seconds + "§c seconds");
                lastCooldownMessageTime.put(playerUUID, now);
            }
            return;
        }

        sunbeamCooldowns.put(playerUUID, now);
        cooldownManager.startCooldown(player, "solstice-sunbeam", "§6§lSolstice: Sunbeam", cooldownMs, BarColor.YELLOW);

        // Play activation sound
        player.playSound(player.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.0f, 1.0f);
        player.playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_RESONATE, 1.0f, 1.5f);

        int shardCount = enhancementManager.getShardCount(item);
        double damage = sunbeamBaseDamage + shardCount;

        org.bukkit.block.Block targetBlock = player.getTargetBlockExact(100);
        org.bukkit.Location targetLocation;
        
        if (targetBlock != null) {
            targetLocation = targetBlock.getLocation();
        } else {
            // If no block is targeted, use the location player is looking at
            targetLocation = player.getEyeLocation().add(player.getEyeLocation().getDirection().multiply(10));
        }

        createSunbeamCylinder(targetLocation, damage, player, shardCount);
        player.sendMessage("§6✦ §eSunbeam cast! §7[§e" + String.format("%.1f", damage) + "§7 HP, §e" + shardCount + "§7/5 shards§6]");
    }

    /**
     * Creates a sunbeam cylinder from the height limit down to the clicked block Y-2
     */
    private void createSunbeamCylinder(org.bukkit.Location centerLocation, double damage, Player caster, int shardCount) {
        World world = centerLocation.getWorld();
        int centerX = centerLocation.getBlockX();
        int centerZ = centerLocation.getBlockZ();
        
        // Calculate the top Y level (height limit - 1 to stay within bounds)
        int topY = world.getMaxHeight() - 1;
        // Calculate the bottom Y level (clicked block Y - 2)
        int bottomY = Math.max(centerLocation.getBlockY() - 2, world.getMinHeight());
        
        // Damage radius
        double radius = sunbeamRadius;
        
        // Create the cylinder instantly instead of over time for performance
        // Spawn particles and damage entities in the cylinder
        for (int y = topY; y >= bottomY; y--) {
            // Spawn particles in a circle at the current Y level
            for (double angle = 0; angle < 360; angle += 15) {
                double radians = Math.toRadians(angle);
                double x = centerX + radius * Math.cos(radians);
                double z = centerZ + radius * Math.sin(radians);
                
                org.bukkit.Location particleLocation = new org.bukkit.Location(world, x, y, z);
                world.spawnParticle(
                    Particle.FALLING_DUST,
                    particleLocation,
                    1,
                    0, 0, 0,  // Offset (none)
                    org.bukkit.Material.YELLOW_CONCRETE.createBlockData() // Material for the particle
                );
            }
        }
        
        // Track already-damaged entities to prevent multiple hits
        Set<UUID> damagedEntities = new HashSet<>();
        
        // Damage all entities in the cylindrical area
        // We'll check in segments to cover the whole cylinder effectively
        for (int y = topY; y >= bottomY; y -= 2) { // Check every 2 blocks vertically
            for (LivingEntity entity : world.getNearbyLivingEntities(
                    new org.bukkit.Location(world, centerX, y, centerZ), 
                    radius, Math.min(2, topY - y + 1), radius)) {
                
                UUID entityId = entity.getUniqueId();
                
                // Skip if already damaged
                if (damagedEntities.contains(entityId)) {
                    continue;
                }
                
                // Skip the caster
                if (entityId.equals(caster.getUniqueId())) {
                    continue;
                }
                
                // Skip trusted players (using protection system)
                if (entity instanceof Player) {
                    // Check team/ally immunity
                    if (TrustHelper.shouldBlockAbilityDamage(caster, entity)) {
                        continue;
                    }
                }
                
                // Mark as damaged and apply damage
                damagedEntities.add(entityId);
                
                // Check if entity has Resistance 3+ (reduces damage by half)
                double actualDamage = damage;
                if (entity.hasPotionEffect(PotionEffectType.RESISTANCE)) {
                    PotionEffect resistanceEffect = entity.getPotionEffect(PotionEffectType.RESISTANCE);
                    if (resistanceEffect != null && resistanceEffect.getAmplifier() >= 3) {
                        actualDamage = damage * 0.5;
                    }
                }
                
                double finalHealth = Math.max(0, entity.getHealth() - actualDamage);
                entity.setHealth(finalHealth);
                
                // Send damage message to player
                String entityName = entity.getName();
                if (entity.getCustomName() != null) {
                    entityName = entity.getCustomName();
                }
                caster.sendMessage("§6✦ §7Damaged §e" + entityName + " §7for §e" + String.format("%.1f", actualDamage) + "§7 HP");
                
                // Play hit sound
                entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_PLAYER_ATTACK_STRONG, 0.8f, 1.5f);
                caster.playSound(caster.getLocation(), Sound.ENTITY_PLAYER_ATTACK_KNOCKBACK, 0.6f, 1.2f);
            }
        }
        
        // Spawn visual effects
        spawnSunbeamEffect(centerLocation);
    }

    /**
     * Spawns visual/audio effects for the sunbeam.
     */
    private void spawnSunbeamEffect(org.bukkit.Location location) {
        World world = location.getWorld();

        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (ticks >= 20) {
                    cancel();
                    return;
                }

                world.spawnParticle(
                    Particle.GLOW,
                    location,
                    10,
                    sunbeamRadius,
                    sunbeamRadius,
                    sunbeamRadius,
                    0.5
                );

                world.spawnParticle(
                    Particle.SMALL_FLAME,
                    location,
                    5,
                    sunbeamRadius * 0.5,
                    sunbeamRadius * 0.5,
                    sunbeamRadius * 0.5,
                    0.2
                );

                ticks++;
            }
        }.runTaskTimer(plugin, 0, 1);
    }

    public void resetCooldown(UUID playerId) {
        sunbeamCooldowns.put(playerId, 0L);
        cooldownManager.removeCooldown(playerId, "solstice-sunbeam");
    }

    public void resetCooldown(Player player) {
        if (player != null) {
            resetCooldown(player.getUniqueId());
        }
    }

    public void resetAllCooldowns() {
        sunbeamCooldowns.clear();
        cooldownManager.clearAllCooldowns();
    }
}
