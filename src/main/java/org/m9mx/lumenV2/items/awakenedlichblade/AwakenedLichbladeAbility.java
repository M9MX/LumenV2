package org.m9mx.lumenV2.items.awakenedlichblade;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BlockIterator;
import org.bukkit.util.Vector;
import org.m9mx.lumenV2.systems.CooldownBossBarManager;
import org.m9mx.lumenV2.systems.EnhancementSystem;
import org.m9mx.lumenV2.systems.enhancement.EnhancementManager;
import org.m9mx.lumenV2.systems.trust.TrustHelper;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.plugin.java.JavaPlugin;

public class AwakenedLichbladeAbility implements Listener {
    
    private final JavaPlugin plugin;
    private final CooldownBossBarManager cooldownManager = CooldownBossBarManager.getInstance();
    
    // Track last click times to prevent rapid triggering
    private final Map<UUID, Long> lastClickTime = new HashMap<>();
    
    // Sonic Boom
    private final Map<UUID, Long> sonicBoomCooldowns = new HashMap<>();
    private double baseDamage;
    private double baseRange;
    private int sonicBoomCooldown; // in seconds
    private double damagePerShard;
    private double rangePerShard;
    private double maxRange;
    
    // Blinding Pulse
    private final Map<UUID, Long> blindingPulseCooldowns = new HashMap<>();
    private double blindingPulseRadius;
    private int blindingPulseCooldown; // in seconds
    private int baseBlindDuration; // seconds at close range
    private int minBlindDuration; // seconds at max range

    public AwakenedLichbladeAbility(JavaPlugin plugin) {
        this.plugin = plugin;
        loadConfig(); // Load configuration values
        
        // Register this class as an event listener
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }
    
    /**
     * Load configuration values from items.yml
     */
    public void loadConfig() {
        try {
            // Access the config manager and get the items config
            org.m9mx.lumenV2.config.ItemsConfig config = org.m9mx.lumenV2.config.ConfigManager.getInstance().getItemsConfig();
            
            // Sonic Boom configuration
            this.baseDamage = config.getConfig().getDouble("awakened_lichblade.sonic_boom.base_damage", 10.0);
            this.baseRange = config.getConfig().getDouble("awakened_lichblade.sonic_boom.base_range", 10.0);
            this.sonicBoomCooldown = config.getConfig().getInt("awakened_lichblade.sonic_boom.cooldown", 40);
            this.damagePerShard = config.getConfig().getDouble("awakened_lichblade.sonic_boom.damage_per_shard", 1.0);
            this.rangePerShard = config.getConfig().getDouble("awakened_lichblade.sonic_boom.range_per_shard", 2.0);
            this.maxRange = config.getConfig().getDouble("awakened_lichblade.sonic_boom.max_range", 20.0);
            
            // Blinding Pulse configuration
            this.blindingPulseRadius = config.getConfig().getDouble("awakened_lichblade.blinding_pulse.base_radius", 50.0);
            this.blindingPulseCooldown = config.getConfig().getInt("awakened_lichblade.blinding_pulse.cooldown", 80);
            this.baseBlindDuration = config.getConfig().getInt("awakened_lichblade.blinding_pulse.base_duration", 60);
            this.minBlindDuration = config.getConfig().getInt("awakened_lichblade.blinding_pulse.min_duration", 1);
            
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to load Awakened Lichblade configuration: " + e.getMessage());
            e.printStackTrace();
            // Set default values in case of error
            setDefaults();
        }
    }
    
    private void setDefaults() {
        // Default values in case config fails to load
        this.baseDamage = 10.0;
        this.baseRange = 10.0;
        this.sonicBoomCooldown = 40;
        this.damagePerShard = 1.0;
        this.rangePerShard = 2.0;
        this.maxRange = 20.0;
        this.blindingPulseRadius = 50.0;
        this.blindingPulseCooldown = 80;
        this.baseBlindDuration = 60;
        this.minBlindDuration = 1;
    }

    public void reloadConfiguration() {
        loadConfig();
    }

    @EventHandler(priority = org.bukkit.event.EventPriority.HIGHEST)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getDamager();
        ItemStack item = player.getInventory().getItemInMainHand();

        if (!AwakenedLichbladeItem.isAwakenedLichblade(item)) {
            return;
        }

        // Ensure damage is allowed through
        event.setCancelled(false);
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        org.bukkit.event.block.Action action = event.getAction();
        if (!action.equals(org.bukkit.event.block.Action.RIGHT_CLICK_AIR) && !action.equals(org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK)) {
            return;
        }

        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();

        if (!AwakenedLichbladeItem.isAwakenedLichblade(item)) {
            return;
        }

        if (player.isSneaking()) {
            return; // Don't trigger on sneak-right-click (likely for enhancement slot)
        }

        UUID playerId = player.getUniqueId();
        long now = System.currentTimeMillis();
        long lastClick = lastClickTime.getOrDefault(playerId, 0L);

        if (now - lastClick < 100) {
            return;
        }

        lastClickTime.put(playerId, now);
        event.setCancelled(true);
        castSonicBoom(player, item);
    }

    @EventHandler
    public void onSwapHands(PlayerSwapHandItemsEvent event) {
        Player player = event.getPlayer();
        ItemStack mainHand = event.getMainHandItem();
        ItemStack offHand = event.getOffHandItem();

        ItemStack lichblade = null;
        if (AwakenedLichbladeItem.isAwakenedLichblade(mainHand)) {
            lichblade = mainHand;
        } else if (AwakenedLichbladeItem.isAwakenedLichblade(offHand)) {
            lichblade = offHand;
        }

        if (lichblade == null) {
            return;
        }

        event.setCancelled(true);
        castBlindingPulse(player, lichblade);
    }

    private void castSonicBoom(Player player, ItemStack item) {
        // Check if item has enhancements
        int shardCount = EnhancementManager.getShardCount(item);
        
        // Calculate damage based on shard count (base 10 + 1 per shard)
        double damage = calculateEnhancedDamage(baseDamage, shardCount);
        double range = calculateEnhancedRange(baseRange, shardCount);
        
        UUID playerUUID = player.getUniqueId();
        long now = System.currentTimeMillis();
        long lastCast = sonicBoomCooldowns.getOrDefault(playerUUID, 0L);
        long cooldownMs = sonicBoomCooldown * 1000L;

        if (now - lastCast < cooldownMs) {
            long remainingMs = cooldownMs - (now - lastCast);
            long seconds = remainingMs / 1000;
            player.sendMessage("§cSonic Boom is on cooldown for §3" + seconds + "§c seconds");
            return;
        }

        // Start cooldown
        sonicBoomCooldowns.put(playerUUID, now);
        cooldownManager.startCooldown(player, "lichblade-sonic-boom", "§3§lAwakened Lichblade: Sonic Boom", cooldownMs, org.bukkit.boss.BarColor.BLUE);

        // Play activation sound
        player.playSound(player.getLocation(), Sound.ENTITY_WARDEN_SONIC_BOOM, 1.0f, 0.8f);
        player.playSound(player.getLocation(), Sound.BLOCK_SCULK_SHRIEKER_SHRIEK, 1.0f, 0.6f);

        performSonicBoom(player, range, damage);
        player.sendMessage("§3✦ §bSonic Boom cast! §7[§b" + String.format("%.1f", damage) + "§7 HP, Range: §b" + String.format("%.1f", range) + "§7 blocks, Shards: §b" + shardCount + "§7/5§3]");
    }

    private double calculateEnhancedDamage(double baseDamage, int shardCount) {
        // Each shard adds damagePerShard to the damage
        double calculatedDamage = baseDamage + (shardCount * damagePerShard);
        return Math.max(baseDamage, calculatedDamage); // Ensure it doesn't go below base damage
    }

    private double calculateEnhancedRange(double baseRange, int shardCount) {
        // Each shard adds rangePerShard to the range, capped at maxRange
        double calculatedRange = baseRange + (shardCount * rangePerShard);
        return Math.min(maxRange, calculatedRange); // Cap at maxRange
    }

    private void performSonicBoom(Player player, double range, double damage) {
        World world = player.getWorld();
        Location startLoc = player.getEyeLocation();
        Vector direction = startLoc.getDirection().normalize();
        
        // Create a line of sonic boom particles
        Set<UUID> affectedEntities = new HashSet<>();
        Location currentLoc = startLoc.clone();
        
        // Step through the path in small increments
        for (double d = 0; d < range; d += 0.5) {
            Location loc = currentLoc.add(direction.clone().multiply(0.5));
            
            // Stop if we hit a solid block
            if (loc.getBlock().getType().isSolid()) {
                break;
            }
            
            // Spawn sonic boom particles
            world.spawnParticle(
                Particle.SONIC_BOOM,
                loc,
                1, // Just one particle per position
                0.1, 0.1, 0.1, // Small offset
                0.0
            );
            
            // Check for nearby entities - increased radius from 1.0 to 2.0 blocks
            for (Entity entity : world.getNearbyEntities(loc, 2.0, 2.0, 2.0)) {
                if (entity instanceof LivingEntity && !entity.equals(player)) {
                    UUID entityUuid = entity.getUniqueId();
                    if (!affectedEntities.contains(entityUuid)) {
                        LivingEntity livingEntity = (LivingEntity) entity;
                        
                        // Check team/ally immunity
                        if (livingEntity instanceof Player && TrustHelper.shouldBlockAbilityDamage(player, livingEntity)) {
                            continue;
                        }
                        
                        affectedEntities.add(entityUuid);
                        
                        // Apply damage ignoring armor
                        double finalDamage = damage;
                        
                        // Check if entity has Resistance effect
                        if (livingEntity.hasPotionEffect(PotionEffectType.RESISTANCE)) {
                            PotionEffect resistance = livingEntity.getPotionEffect(PotionEffectType.RESISTANCE);
                            if (resistance != null && resistance.getAmplifier() >= 3) {
                                finalDamage *= 0.5; // Reduce damage by 50% if resistance 3+
                            }
                        }
                        
                        // Apply damage using the entity's damage method to respect armor properly
                        // But we'll bypass armor by dealing direct damage
                        livingEntity.damage(finalDamage, player); // Damage source is the player
                        
                        player.sendMessage("§3✦ §7Sonic Boom struck " + entity.getName() + " for §b" + String.format("%.1f", finalDamage) + "§7 HP (armor ignored)");
                        
                        // Play hit sound
                        entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_WARDEN_SONIC_BOOM, 0.8f, 1.5f);
                    }
                }
            }
        }
    }

    private void castBlindingPulse(Player player, ItemStack item) {
        UUID playerUUID = player.getUniqueId();
        long now = System.currentTimeMillis();
        long lastCast = blindingPulseCooldowns.getOrDefault(playerUUID, 0L);
        long cooldownMs = blindingPulseCooldown * 1000L;

        if (now - lastCast < cooldownMs) {
            long remainingMs = cooldownMs - (now - lastCast);
            long seconds = remainingMs / 1000;
            player.sendMessage("§cBlinding Pulse is on cooldown for §3" + seconds + "§c seconds");
            return;
        }

        // Start cooldown
        blindingPulseCooldowns.put(playerUUID, now);
        cooldownManager.startCooldown(player, "lichblade-blinding-pulse", "§3§lAwakened Lichblade: Blinding Pulse", cooldownMs, org.bukkit.boss.BarColor.BLUE);

        // Play activation sound
        player.playSound(player.getLocation(), Sound.ENTITY_WARDEN_DEATH, 1.0f, 0.7f);
        player.playSound(player.getLocation(), Sound.BLOCK_SCULK_SHRIEKER_SHRIEK, 1.0f, 0.8f);

        performBlindingPulse(player);
        player.sendMessage("§3✦ §bBlinding Pulse activated! §7[§b" + blindingPulseRadius + "§7 block radius]");
    }

    private void performBlindingPulse(Player player) {
        World world = player.getWorld();
        Location center = player.getLocation();
        double centerX = center.getX();
        double centerY = center.getY();
        double centerZ = center.getZ();

        // Create a spherical area effect
        int particleDensity = 10; // Number of particles per "layer"
        double radius = blindingPulseRadius;
        
        // Visual effect with particles
        for (double y = -radius; y <= radius; y += 2) {
            for (int i = 0; i < particleDensity; i++) {
                double angle = 2 * Math.PI * i / particleDensity;
                double r = Math.sqrt(radius * radius - y * y);
                double x = r * Math.cos(angle);
                double z = r * Math.sin(angle);
                
                Location particleLoc = new Location(world, centerX + x, centerY + y, centerZ + z);
                world.spawnParticle(
                    Particle.DUST,
                    particleLoc,
                    1,
                    0, 0, 0,
                    new Particle.DustOptions(Color.fromRGB(0, 0, 255), 1.0f) // Blue particles
                );
            }
        }

        // Apply blindness to nearby entities and players
        for (Entity entity : world.getNearbyEntities(center, radius, radius, radius)) {
            if (entity instanceof LivingEntity && !entity.equals(player)) {
                LivingEntity livingEntity = (LivingEntity) entity;
                
                // Calculate distance-based blindness duration
                double distance = entity.getLocation().distance(center);
                int duration = calculateDistanceBasedDuration(distance);
                
                // Apply blindness effect
                livingEntity.addPotionEffect(new PotionEffect(
                    PotionEffectType.BLINDNESS,
                    duration * 20, // Convert seconds to ticks
                    0 // Amplifier level
                ));
                
                // Send appropriate messages
                if (entity instanceof Player) {
                    Player targetPlayer = (Player) entity;
                    player.sendMessage("§3✦ §7Applied blindness to player " + targetPlayer.getName() + " for §b" + duration + "§7 seconds");
                    targetPlayer.sendMessage("§cYou've been affected by Blinding Pulse!");
                } else {
                    player.sendMessage("§3✦ §7Applied blindness to " + entity.getName() + " for §b" + duration + "§7 seconds");
                }
            }
        }
    }

    /**
     * Calculate distance-based blindness duration
     * Close range (0-2 blocks): baseBlindDuration seconds
     * Far range (blindingPulseRadius blocks): minBlindDuration seconds
     */
    private int calculateDistanceBasedDuration(double distance) {
        // Normalize distance to a 0-1 range based on the pulse radius
        double normalizedDistance = Math.min(1.0, distance / blindingPulseRadius);
        
        // Calculate duration as a linear interpolation between base and min duration
        double durationDiff = baseBlindDuration - minBlindDuration;
        double duration = baseBlindDuration - (durationDiff * normalizedDistance);
        
        // Return the duration as an integer, ensuring it's at least 1
        return Math.max(1, (int) Math.round(duration));
    }

    public void resetCooldown(UUID playerId) {
        // Reset cooldown for both abilities but don't affect active effects
        sonicBoomCooldowns.remove(playerId);
        blindingPulseCooldowns.remove(playerId);
        
        // Remove any active boss bars for this player
        cooldownManager.removeCooldown(playerId, "lichblade-sonic-boom");
        cooldownManager.removeCooldown(playerId, "lichblade-blinding-pulse");
    }
    
    public void resetCooldown(Player player) {
        resetCooldown(player.getUniqueId());
    }

    public void resetAllCooldowns() {
        sonicBoomCooldowns.clear();
        blindingPulseCooldowns.clear();
        cooldownManager.clearAllCooldowns();
    }
}