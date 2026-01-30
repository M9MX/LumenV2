package org.m9mx.lumenV2.items.etherealkatana;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.boss.BarColor;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Zombie;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.m9mx.lumenV2.config.ConfigManager;
import org.m9mx.lumenV2.systems.CooldownBossBarManager;
import org.m9mx.lumenV2.systems.trust.TrustHelper;
import org.m9mx.lumenV2.systems.enhancement.EnhancementManager;

/**
 * Handles Ethereal Katana abilities:
 * - Soul gain on mob/player kills
 * - Spectral Dash (right-click, 1 soul, 30s cooldown)
 * - Spectral Nova (swap hands/F key, 5 souls, 120s cooldown)
 */
public class EtherealKatanaAbility implements Listener {
    private final JavaPlugin plugin;
    private final EnhancementManager enhancementManager;
    private final CooldownBossBarManager cooldownManager;
    private final Map<UUID, Long> dashCooldowns = new HashMap<>();
    private final Map<UUID, Long> novaCooldowns = new HashMap<>();
    private final Map<UUID, Integer> mobKillCounter = new HashMap<>();
    private final Map<UUID, Long> lastKillTime = new HashMap<>();
    private final Map<UUID, Long> lastClickTime = new HashMap<>();
    private final Map<UUID, Long> lastCooldownMessageTime = new HashMap<>();

    private int dashCooldown;
    private int novaCooldown;
    private int dashSoulCost;
    private int dashRange;
    private double dashDamage;
    private int novaSoulCost;
    private int novaRadius;
    private double novaDamage;
    private double novaKnockbackMult;
    private double novaKnockbackUp;

    public EtherealKatanaAbility(JavaPlugin plugin) {
        this.plugin = plugin;
        this.enhancementManager = new EnhancementManager();
        this.cooldownManager = CooldownBossBarManager.getInstance();
        loadConfiguration();
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    private void loadConfiguration() {
        // Load from items.yml config or use defaults
        var itemsConfig = ConfigManager.getInstance().getItemsConfig();

        if (itemsConfig != null && itemsConfig.get("ethereal_katana") != null) {
            // Spectral Dash configuration
            this.dashSoulCost = itemsConfig.getInt("ethereal_katana.dash.soul_cost", 1);
            this.dashCooldown = itemsConfig.getInt("ethereal_katana.dash.cooldown", 30);
            this.dashRange = itemsConfig.getInt("ethereal_katana.dash.base_range", 8);
            this.dashDamage = itemsConfig.get("ethereal_katana.dash.base_damage") != null
                    ? itemsConfig.getConfig().getDouble("ethereal_katana.dash.base_damage", 2.0)
                    : 2.0;

            // Spectral Nova configuration
            this.novaSoulCost = itemsConfig.getInt("ethereal_katana.nova.soul_cost", 5);
            this.novaCooldown = itemsConfig.getInt("ethereal_katana.nova.cooldown", 120);
            this.novaRadius = itemsConfig.getInt("ethereal_katana.nova.base_radius", 6);
            this.novaDamage = itemsConfig.get("ethereal_katana.nova.base_damage") != null
                    ? itemsConfig.getConfig().getDouble("ethereal_katana.nova.base_damage", 4.0)
                    : 4.0;
            this.novaKnockbackMult = itemsConfig.get("ethereal_katana.nova.knockback_multiplier") != null
                    ? itemsConfig.getConfig().getDouble("ethereal_katana.nova.knockback_multiplier", 2.5)
                    : 2.5;
            this.novaKnockbackUp = itemsConfig.get("ethereal_katana.nova.knockback_upward") != null
                    ? itemsConfig.getConfig().getDouble("ethereal_katana.nova.knockback_upward", 0.8)
                    : 0.8;

        } else {
            // Use hardcoded defaults if config not found
            this.dashSoulCost = 1;
            this.dashCooldown = 30;
            this.dashRange = 8;
            this.dashDamage = 2.0;
            this.novaSoulCost = 5;
            this.novaCooldown = 120;
            this.novaRadius = 6;
            this.novaDamage = 4.0;
            this.novaKnockbackMult = 2.5;
            this.novaKnockbackUp = 0.8;
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

        if (!EtherealKatanaItem.isEtherealKatana(item)) {
            return;
        }

        // Ensure damage is allowed through
        event.setCancelled(false);
    }

    @EventHandler
    public void onEntityKilled(EntityDeathEvent event) {
        LivingEntity entity = event.getEntity();
        Player killer = entity.getKiller();

        if (killer == null) {
            return;
        }

        UUID killerId = killer.getUniqueId();
        long now = System.currentTimeMillis();
        long lastKill = lastKillTime.getOrDefault(killerId, 0L);

        if (now - lastKill < 100) {
            return;
        }

        lastKillTime.put(killerId, now);

        ItemStack item = killer.getInventory().getItemInMainHand();

        if (!EtherealKatanaItem.isEtherealKatana(item)) {
            return;
        }

        int currentSouls = EtherealKatanaItem.getSouls(item);

        if (currentSouls >= 20) {
            return;
        }

        if (entity instanceof Player) {
            EtherealKatanaItem.addSouls(item, 5);
            killer.getInventory().setItemInMainHand(item);
            killer.sendMessage("§5✦ §7Defeated player: §d+5 §5souls");
            // Play player kill sound
            killer.playSound(killer.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.2f);
            killer.playSound(killer.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.0f, 1.5f);
        } else if (killer.getGameMode() == GameMode.CREATIVE && entity instanceof Zombie
                && entity.getCustomName() != null) {
            String zombieName = entity.getCustomName().toLowerCase();
            if (zombieName.contains("spectral") && zombieName.contains("zombie")) {
                EtherealKatanaItem.addSouls(item, 5);
                killer.getInventory().setItemInMainHand(item);
                killer.sendMessage("§5✦ §7Slayed Spectral Zombie: §d+5 §5souls");
                // Play special zombie kill sound
                killer.playSound(killer.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.0f, 1.5f);
            }
        } else {
            int counter = mobKillCounter.getOrDefault(killerId, 0) + 1;
            mobKillCounter.put(killerId, counter);

            if (counter >= 10) {
                EtherealKatanaItem.addSouls(item, 1);
                killer.getInventory().setItemInMainHand(item);
                mobKillCounter.put(killerId, 0);
                killer.sendMessage("§5✦ §7Mob kill streak: §d+1 §5soul");
                // Play mob streak soul gain sound
                killer.playSound(killer.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.3f);
                killer.playSound(killer.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.8f, 1.3f);
            } else if (counter == 5 || counter == 9) {
                killer.sendMessage("§7Mob kills: " + counter + "/10");
                // Play progress sound
                killer.playSound(killer.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 0.5f, 1.0f);
            }
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        org.bukkit.event.block.Action action = event.getAction();
        if (!action.equals(org.bukkit.event.block.Action.RIGHT_CLICK_AIR)) {
            return;
        }
        if (event.getPlayer().isSneaking()) {
            return;
        }

        ItemStack item = event.getPlayer().getInventory().getItemInMainHand();
        if (!EtherealKatanaItem.isEtherealKatana(item)) {
            return;
        }

        UUID playerId = event.getPlayer().getUniqueId();
        long now = System.currentTimeMillis();
        long lastClick = lastClickTime.getOrDefault(playerId, 0L);

        if (now - lastClick < 100) {
            return;
        }

        lastClickTime.put(playerId, now);
        event.setCancelled(true);
        castSpectralDash(event.getPlayer(), item);
    }

    @EventHandler
    public void onSwapHands(PlayerSwapHandItemsEvent event) {
        Player player = event.getPlayer();
        ItemStack mainHand = event.getMainHandItem();
        ItemStack offHand = event.getOffHandItem();

        ItemStack katana = null;
        if (EtherealKatanaItem.isEtherealKatana(mainHand)) {
            katana = mainHand;
        } else if (EtherealKatanaItem.isEtherealKatana(offHand)) {
            katana = offHand;
        }

        if (katana == null) {
            return;
        }

        event.setCancelled(true);
        castSpectralNova(event.getPlayer(), katana);
    }

    private void castSpectralDash(Player player, ItemStack item) {
        if (player.isSneaking()) {
            return;
        }

        UUID playerUUID = player.getUniqueId();
        long now = System.currentTimeMillis();
        long lastDash = dashCooldowns.getOrDefault(playerUUID, 0L);
        long cooldownMs = dashCooldown * 1000L;

        if (now - lastDash < cooldownMs) {
            // Only show message every 1 second to avoid spam
            long lastMessage = lastCooldownMessageTime.getOrDefault(playerUUID, 0L);
            if (now - lastMessage >= 1000) {
                long remainingMs = cooldownMs - (now - lastDash);
                long seconds = remainingMs / 1000;
                player.sendMessage("§cSpectral Dash is on cooldown for §5" + seconds + "§c seconds");
                lastCooldownMessageTime.put(playerUUID, now);
            }
            return;
        }

        if (!EtherealKatanaItem.consumeSouls(item, dashSoulCost)) {
            player.sendMessage(
                    "§cNot enough souls! Need §5" + dashSoulCost + "§c soul" + (dashSoulCost > 1 ? "s" : ""));
            return;
        }

        dashCooldowns.put(playerUUID, now);
        cooldownManager.startCooldown(player, "ethereal-dash", "§d§lEthereal Katana: Dash", cooldownMs,
                BarColor.PURPLE);

        // Play activation sound
        player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.5f);
        player.playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_RESONATE, 1.0f, 1.8f);

        int shardCount = enhancementManager.getShardCount(item);
        int range = dashRange + shardCount;
        double damage = dashDamage + shardCount;

        performDash(player, item, range, damage);
        int remainingSouls = EtherealKatanaItem.getSouls(item);
        player.sendMessage("§5✦ §dSpectral Dash! §7[§d" + range + "§7 blocks, §d" + String.format("%.1f", damage)
                + "§7 HP damage] §7Souls: §d" + remainingSouls + "§7/20");
    }

    private void performDash(Player player, ItemStack item, int range, double damage) {
        // Use the item's actual attack damage attribute instead of hardcoded value
        double actualDamage = getItemAttackDamage(item);
        World world = player.getWorld();
        Vector direction = player.getEyeLocation().getDirection().normalize();
        double velocityMult = range / 8.0;

        Set<UUID> damagedEntities = new HashSet<>();
        Vector initialVelocity = player.getVelocity().clone();

        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (ticks >= 5) {
                    Vector finalVelocity = new Vector(
                            direction.getX() * velocityMult * 1.3,
                            Math.max(0.15, direction.getY() * velocityMult * 1.3),
                            direction.getZ() * velocityMult * 1.3);
                    player.setVelocity(finalVelocity);
                    cancel();
                    return;
                }

                Vector dashVelocity = direction.clone().multiply(velocityMult * 1.3);
                dashVelocity.setY(Math.max(0.15, dashVelocity.getY()));
                player.setVelocity(dashVelocity);

                world.spawnParticle(
                        Particle.DUST,
                        player.getLocation(),
                        5,
                        0.3, 0.3, 0.3,
                        new Particle.DustOptions(org.bukkit.Color.PURPLE, 1.0f));

                Location current = player.getLocation();
                for (Entity entity : world.getNearbyEntities(current, 2, 2, 2)) {
                    if (entity instanceof LivingEntity && !entity.equals(player)) {
                        UUID entityId = entity.getUniqueId();
                        if (!damagedEntities.contains(entityId)) {
                            LivingEntity living = (LivingEntity) entity;

                            if (living instanceof Player) {
                                // Check team/ally immunity
                                if (TrustHelper.shouldBlockAbilityDamage(player, living)) {
                                    ticks++;
                                    return;
                                }
                            }

                            damagedEntities.add(entityId);
                            double actualDamage = damage;

                            if (living.hasPotionEffect(PotionEffectType.RESISTANCE)) {
                                PotionEffect resistanceEffect = living.getPotionEffect(PotionEffectType.RESISTANCE);
                                if (resistanceEffect != null && resistanceEffect.getAmplifier() >= 3) {
                                    actualDamage = damage * 0.5;
                                }
                            }

                            double finalHealth = Math.max(0, living.getHealth() - actualDamage);
                            living.setHealth(finalHealth);
                            player.sendMessage("§5✦ §7Struck " + entity.getName() + " for §d"
                                    + String.format("%.1f", actualDamage) + "§7 HP");

                            // Play hit sound
                            living.getWorld().playSound(living.getLocation(), Sound.ENTITY_PLAYER_ATTACK_KNOCKBACK,
                                    0.8f, 1.5f);
                            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_STRONG, 0.6f, 1.2f);
                        }
                    }
                }

                ticks++;
            }
        }.runTaskTimer(plugin, 0, 1);
    }

    private void castSpectralNova(Player player, ItemStack item) {
        UUID playerUUID = player.getUniqueId();
        long now = System.currentTimeMillis();
        long lastNova = novaCooldowns.getOrDefault(playerUUID, 0L);
        long cooldownMs = novaCooldown * 1000L;

        if (now - lastNova < cooldownMs) {
            long remainingMs = cooldownMs - (now - lastNova);
            long seconds = remainingMs / 1000;
            player.sendMessage("§cSpectral Nova is on cooldown for §5" + seconds + "§c seconds");
            return;
        }

        if (!EtherealKatanaItem.consumeSouls(item, novaSoulCost)) {
            player.sendMessage("§cNot enough souls! Need §5" + novaSoulCost + "§c souls");
            return;
        }

        player.getInventory().setItemInMainHand(item);
        novaCooldowns.put(playerUUID, now);
        cooldownManager.startCooldown(player, "ethereal-nova", "§d§lEthereal Katana: Nova", cooldownMs,
                BarColor.PURPLE);

        // Play activation sound
        player.playSound(player.getLocation(), Sound.ENTITY_WARDEN_SONIC_BOOM, 1.0f, 1.0f);
        player.playSound(player.getLocation(), Sound.BLOCK_AMETHYST_CLUSTER_BREAK, 1.0f, 0.8f);

        int shardCount = enhancementManager.getShardCount(item);
        int radius = novaRadius + shardCount;
        double damage = novaDamage + shardCount;

        performNova(player, radius, damage);
        int remainingSouls = EtherealKatanaItem.getSouls(item);
        player.sendMessage("§5✦ §dSpectral Nova! §7[§d" + radius + "§7 block radius, §d" + String.format("%.1f", damage)
                + "§7 HP damage] §7Souls: §d" + remainingSouls + "§7/20");
    }

    private void performNova(Player player, int radius, double damage) {
        World world = player.getWorld();
        Location center = player.getLocation();
        double playerY = player.getLocation().getY();

        spawnNovaParticles(world, center, radius, playerY);
        damageNovaEntities(player, center, playerY, radius, damage);
        player.playSound(center, Sound.ENTITY_WARDEN_AMBIENT, 1.0f, 1.0f);
    }

    private void spawnNovaParticles(World world, Location center, int radius, double playerY) {
        new BukkitRunnable() {
            int particleTicks = 0;

            @Override
            public void run() {
                if (particleTicks >= 20) {
                    cancel();
                    return;
                }

                for (double angle = 0; angle < 360; angle += 10) {
                    double radians = Math.toRadians(angle);
                    double x = center.getX() + radius * Math.cos(radians);
                    double z = center.getZ() + radius * Math.sin(radians);
                    Location particleLocation = new Location(world, x, playerY, z);

                    world.spawnParticle(
                            Particle.DUST,
                            particleLocation,
                            2,
                            0.1, 0.1, 0.1,
                            new Particle.DustOptions(org.bukkit.Color.PURPLE, 1.0f));
                }

                particleTicks++;
            }
        }.runTaskTimer(plugin, 0, 1);
    }

    private void damageNovaEntities(Player player, Location center, double playerY, int radius, double damage) {
        World world = center.getWorld();
        Set<UUID> damagedEntities = new HashSet<>();

        for (Entity entity : world.getNearbyEntities(center, radius, 3, radius)) {
            if (entity instanceof LivingEntity && !entity.equals(player)) {
                double entityY = entity.getLocation().getY();
                if (entityY >= playerY && entityY <= playerY + 2) {
                    UUID entityId = entity.getUniqueId();

                    if (!damagedEntities.contains(entityId)) {
                        LivingEntity living = (LivingEntity) entity;

                        if (living instanceof Player) {
                            // Check team/ally immunity
                            if (TrustHelper.shouldBlockAbilityDamage(player, living)) {
                                continue;
                            }
                        }

                        damagedEntities.add(entityId);
                        double actualDamage = damage;

                        if (living.hasPotionEffect(PotionEffectType.RESISTANCE)) {
                            PotionEffect resistanceEffect = living.getPotionEffect(PotionEffectType.RESISTANCE);
                            if (resistanceEffect != null && resistanceEffect.getAmplifier() >= 3) {
                                actualDamage = damage * 0.5;
                            }
                        }

                        double finalHealth = Math.max(0, living.getHealth() - actualDamage);
                        living.setHealth(finalHealth);

                        Vector knockback = entity.getLocation().subtract(center).toVector().normalize()
                                .multiply(novaKnockbackMult);
                        knockback.setY(novaKnockbackUp);
                        entity.setVelocity(knockback);

                        player.sendMessage("§5✦ §7Blasted " + entity.getName() + " for §d"
                                + String.format("%.1f", actualDamage) + "§7 HP");

                        // Play blast hit sound
                        living.getWorld().playSound(living.getLocation(), Sound.BLOCK_AMETHYST_CLUSTER_BREAK, 0.8f,
                                1.0f);
                        player.playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_BREAK, 0.6f, 0.8f);
                    }
                }
            }
        }
    }

    public void resetCooldown(UUID playerId) {
        dashCooldowns.put(playerId, 0L);
        novaCooldowns.put(playerId, 0L);
        cooldownManager.removeCooldown(playerId, "ethereal-dash");
        cooldownManager.removeCooldown(playerId, "ethereal-nova");
    }

    public void resetCooldown(Player player) {
        if (player != null) {
            resetCooldown(player.getUniqueId());
        }
    }

    /**
     * Get the attack damage from an item's attributes
     */
    private double getItemAttackDamage(ItemStack item) {
        if (!item.hasItemMeta())
            return 1.0; // Default punch damage

        var meta = item.getItemMeta();
        if (meta != null && meta.hasAttributeModifiers()) {
            var attributes = meta.getAttributeModifiers(org.bukkit.attribute.Attribute.ATTACK_DAMAGE);
            for (var modifier : attributes) {
                if (modifier.getOperation() == org.bukkit.attribute.AttributeModifier.Operation.ADD_NUMBER) {
                    return modifier.getAmount();
                }
            }
        }

        return 1.0; // Default if no modifier found
    }

    public void resetAllCooldowns() {
        dashCooldowns.clear();
        novaCooldowns.clear();
        cooldownManager.clearAllCooldowns();
    }
}
