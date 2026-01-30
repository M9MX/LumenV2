package org.m9mx.lumenV2.items.lotusblossom;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.bukkit.boss.BarColor;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.AxisAngle4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.m9mx.lumenV2.Lumen;
import org.m9mx.lumenV2.config.ConfigManager;
import org.m9mx.lumenV2.systems.CooldownBossBarManager;
import org.m9mx.lumenV2.systems.trust.TrustHelper;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Handles Lotus Blossom abilities:
 * - Right-Click: Blossom Shield (creates 16 rotating spore blossom displays around player in 2 circles for 10 seconds,
 *   halves damage taken and reflects 1.5x damage back to attacker)
 */
public class LotusBlossomAbility implements Listener {
    private final JavaPlugin plugin;
    private final CooldownBossBarManager cooldownManager;
    private final Map<UUID, Long> shieldCooldowns = new HashMap<>();
    private final Map<UUID, BlossomShieldTask> activeShields = new HashMap<>();
    private final Map<UUID, Long> lastClickTime = new HashMap<>();
    private final Map<UUID, Long> lastCooldownMessageTime = new HashMap<>();

    private int shieldDuration;
    private int shieldCooldown;
    private double damageReduction;
    private double thornsMultiplier;

    public LotusBlossomAbility(JavaPlugin plugin) {
        this.plugin = plugin;
        this.cooldownManager = CooldownBossBarManager.getInstance();
        loadConfiguration();
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    private void loadConfiguration() {
        // Load from items.yml config or use defaults
        var itemsConfig = ConfigManager.getInstance().getItemsConfig();

        if (itemsConfig != null && itemsConfig.get("lotus_blossom") != null) {
            // Blossom Shield configuration
            this.shieldDuration = itemsConfig.getInt("lotus_blossom.shield.duration", 10);
            this.shieldCooldown = itemsConfig.getInt("lotus_blossom.shield.cooldown", 60);
            this.damageReduction = itemsConfig.get("lotus_blossom.shield.damage_reduction") != null ?
                    itemsConfig.getConfig().getDouble("lotus_blossom.shield.damage_reduction", 0.5) : 0.5;
            this.thornsMultiplier = itemsConfig.get("lotus_blossom.shield.thorns_multiplier") != null ?
                    itemsConfig.getConfig().getDouble("lotus_blossom.shield.thorns_multiplier", 1.5) : 1.5;
        } else {
            // Use hardcoded defaults if config not found
            this.shieldDuration = 10;
            this.shieldCooldown = 60;
            this.damageReduction = 0.5;
            this.thornsMultiplier = 1.5;
        }
    }

    public void reloadConfiguration() {
        loadConfiguration();
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (!event.getAction().equals(Action.RIGHT_CLICK_AIR) && !event.getAction().equals(Action.RIGHT_CLICK_BLOCK)) {
            return;
        }

        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();

        if (!LotusBlossomItem.isLotusBlossom(item)) {
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
        activateBlossomShield(player, item);
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getEntity();
        UUID playerId = player.getUniqueId();

        // Check if player has an active shield
        if (activeShields.containsKey(playerId)) {
            // Calculate the damage that should be absorbed by the shield
            // If damageReduction is 0.5 (50%), then player should take (1 - 0.5) = 0.5 (50%) of the damage
            double finalDamageMultiplier = 1.0 - damageReduction;
            double reducedDamage = event.getDamage() * finalDamageMultiplier;
            event.setDamage(reducedDamage);
        }
    }

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player) || !(event.getEntity() instanceof Player)) {
            return;
        }

        Player attacker = (Player) event.getDamager();
        Player defender = (Player) event.getEntity();
        UUID defenderId = defender.getUniqueId();

        // Check if the defending player has an active shield
        if (activeShields.containsKey(defenderId)) {
            // Check if attacker and defender are teammates/allies
            if (TrustHelper.shouldBlockAbilityDamage(defender, attacker)) {
                // Don't reflect damage against teammates/allies
                return;
            }

            double originalDamage = event.getDamage(); // Use getDamage() not getFinalDamage() for reflection
            double reflectedDamage = originalDamage * thornsMultiplier;

            // Apply reflected damage to attacker after a short delay
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                // Apply damage to the attacker using EntityDamageEvent to ensure armor applies
                attacker.damage(reflectedDamage, defender); // Specify the source of damage (defender) to ensure it's not treated as environmental damage
            }, 1L);

            // Show visual feedback for reflected damage
            attacker.getWorld().playSound(attacker.getLocation(), Sound.ENTITY_SHULKER_HURT_CLOSED, 0.8f, 1.0f);
        }
    }

    private void activateBlossomShield(Player player, ItemStack item) {
        UUID playerUUID = player.getUniqueId();
        long now = System.currentTimeMillis();
        long lastShield = shieldCooldowns.getOrDefault(playerUUID, 0L);
        long cooldownMs = shieldCooldown * 1000L;

        if (now - lastShield < cooldownMs) {
            // Only show message every 1 second to avoid spam
            long lastMessage = lastCooldownMessageTime.getOrDefault(playerUUID, 0L);
            if (now - lastMessage >= 1000) {
                long remainingMs = cooldownMs - (now - lastShield);
                long seconds = remainingMs / 1000;
                player.sendMessage("§cBlossom Shield is on cooldown for §d" + seconds + "§c seconds");
                lastCooldownMessageTime.put(playerUUID, now);
            }
            return;
        }

        // Start the shield effect first
        BlossomShieldTask shieldTask = new BlossomShieldTask(player, shieldDuration);
        shieldTask.runTaskTimer(plugin, 0, 2); // Run every 2 ticks (0.1 seconds)
        activeShields.put(playerUUID, shieldTask);

        // Start the duration boss bar (showing effect duration)
        String durationTitle = "§d§lLotus Blossom: Shield Active";
        cooldownManager.startCooldown(player, "lotus-shield-duration", durationTitle, shieldDuration * 1000L, BarColor.GREEN);

        // Play activation sound
        player.playSound(player.getLocation(), Sound.BLOCK_RESPAWN_ANCHOR_SET_SPAWN, 0.8f, 1.2f);
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.6f, 1.5f);

        player.sendMessage("§d✦ §5Blossom Shield activated! §7[§d" + shieldDuration + "§7 seconds]");
    }

    /**
     * Task that manages the blossom shield visual effect and duration
     */
    private class BlossomShieldTask extends BukkitRunnable {
        private final Player player;
        private final int durationSeconds;
        private int elapsedTicks = 0;
        private final BlockDisplay[] blossomDisplays = new BlockDisplay[16]; // 8 lower + 8 upper
        private final double radius = 1.5; // Radius around player

        public BlossomShieldTask(Player player, int durationSeconds) {
            this.player = player;
            this.durationSeconds = durationSeconds;
            
            // Create 16 block displays for spore blossoms (8 lower + 8 upper)
            BlockData sporeBlossomData = Material.SPORE_BLOSSOM.createBlockData();
            for (int i = 0; i < 16; i++) {
                BlockDisplay display = player.getWorld().spawn(player.getLocation(), BlockDisplay.class);
                display.setBlock(sporeBlossomData);
                blossomDisplays[i] = display;
            }
        }

        @Override
        public void run() {
            if (!player.isOnline() || player.isDead()) {
                cancel();
                cleanupShield(player.getUniqueId());
                return;
            }

            // Update positions to rotate the spore blossoms around the player
            World world = player.getWorld();
            Location center = player.getLocation().clone();
            // Use body position without head rotation - only yaw for horizontal rotation
            center.setYaw(0f); // Remove player's head rotation influence
            
            // Exact data from Minecraft commands - ordered as: 3, 7, 4, 8, 2, 5, 1, 6
            // Which is: North, NE, East, SE, South, SW, West, NW
            // Scaled down to bring closer to player
            center.setYaw(0f);
            center.setPitch(0f);
            // Positions (tx, ty, tz) - scaled by 0.65 and brought to player center
            float scale = 0.65f;
            float[][] positions = {
                {0.0625f * scale, 0.5f, -1.8125f * scale},    // 0: North (block 3)
                {1.8750f * scale, 0.5f, -1.5000f * scale},    // 1: NE (block 7)
                {2.8750f * scale, 0.5f, 0.0000f * scale},     // 2: East (block 4)
                {2.5000f * scale, 0.5f, 1.7500f * scale},     // 3: SE (block 8)
                {1.0000f * scale, 0.5f, 2.7500f * scale},     // 4: South (block 2)
                {-0.8750f * scale, 0.5f, 2.4375f * scale},    // 5: SW (block 5)
                {-1.9375f * scale, 0.5f, 1.0000f * scale},    // 6: West (block 1)
                {-1.5625f * scale, 0.5f, -0.8125f * scale}    // 7: NW (block 6)
            };
            
            // Rotation matrices (3x3, stored as 9 floats)
            float[][] rotations = {
                {1.0f, 0.0f, 0.0f, 0.0f, 0.0f, -1.0f, 0.0f, 1.0f, 0.0f},           // 0: North
                {0.7071f, -0.7071f, 0.0f, 0.0f, 0.0f, -1.0f, 0.7071f, 0.7071f, 0.0f}, // 1: NE
                {0.0f, -1.0f, 0.0f, 0.0f, 0.0f, -1.0f, 1.0f, 0.0f, 0.0f},         // 2: East
                {-0.7071f, -0.7071f, 0.0f, 0.0f, 0.0f, -1.0f, 0.7071f, -0.7071f, 0.0f}, // 3: SE
                {-1.0f, 0.0f, 0.0f, 0.0f, 0.0f, -1.0f, 0.0f, -1.0f, 0.0f},        // 4: South
                {-0.7071f, 0.7071f, 0.0f, 0.0f, 0.0f, -1.0f, -0.7071f, -0.7071f, 0.0f}, // 5: SW
                {0.0f, 1.0f, 0.0f, 0.0f, 0.0f, -1.0f, -1.0f, 0.0f, 0.0f},        // 6: West
                {0.7071f, 0.7071f, 0.0f, 0.0f, 0.0f, -1.0f, -0.7071f, 0.7071f, 0.0f}  // 7: NW
            };  
            
            // Update all 16 blossoms (8 lower + 8 upper)
            for (int i = 0; i < 16; i++) {
            if (blossomDisplays[i] != null && !blossomDisplays[i].isDead()) {
                // Current and next point indices for this blossom
                // Use modulo 8 for the position array since both circles share the same pattern
                int circleIndex = i % 8;
                double angle = (elapsedTicks * 0.02) + (2 * Math.PI * circleIndex / 8);
                double normalizedAngle = angle % (2 * Math.PI);
                if (normalizedAngle < 0) normalizedAngle += 2 * Math.PI;
                double pointIndex = (normalizedAngle / (2 * Math.PI)) * 8.0;
                int idx = (int)pointIndex % 8;
                int nextIdx = (idx + 1) % 8;
                double lerp = pointIndex - idx;
                
                // Interpolate position
                float posX = positions[idx][0] * (float)(1 - lerp) + positions[nextIdx][0] * (float)lerp;
                float posZ = positions[idx][2] * (float)(1 - lerp) + positions[nextIdx][2] * (float)lerp;
                
                // Determine Y position based on which circle this blossom belongs to
                float posY;
                if (i < 8) {
                    // Lower circle - move up by 0.5 blocks from original position
                    posY = (positions[idx][1] * (float)(1 - lerp) + positions[nextIdx][1] * (float)lerp) + 0.5f;
                } else {
                    // Upper circle - 1 block higher than lower circle
                    posY = (positions[idx][1] * (float)(1 - lerp) + positions[nextIdx][1] * (float)lerp) + 1.5f;
                }
                
                // No additional offset needed - positions should be centered on player
                
                // Interpolate rotation matrix between current and next
                float[] rotMatrix1 = rotations[idx];
                float[] rotMatrix2 = rotations[nextIdx];
                float[] rotMatrix = new float[9];
                for (int j = 0; j < 9; j++) {
                    rotMatrix[j] = rotMatrix1[j] * (float)(1 - lerp) + rotMatrix2[j] * (float)lerp;
                }
                float m00 = rotMatrix[0];
                float m01 = rotMatrix[1];
                float m02 = rotMatrix[2];
                float m10 = rotMatrix[3];
                float m11 = rotMatrix[4];
                float m12 = rotMatrix[5];
                float m20 = rotMatrix[6];
                float m21 = rotMatrix[7];
                float m22 = rotMatrix[8];
                
                // Convert rotation matrix to quaternion
                Quaternionf rotation = new Quaternionf();
                float trace = m00 + m11 + m22;
                if (trace > 0) {
                    float s = 0.5f / (float)Math.sqrt(trace + 1.0f);
                    rotation.w = 0.25f / s;
                    rotation.x = (m21 - m12) * s;
                    rotation.y = (m02 - m20) * s;
                    rotation.z = (m10 - m01) * s;
                } else if (m00 > m11 && m00 > m22) {
                    float s = 2.0f * (float)Math.sqrt(1.0f + m00 - m11 - m22);
                    rotation.w = (m21 - m12) / s;
                    rotation.x = 0.25f * s;
                    rotation.y = (m01 + m10) / s;
                    rotation.z = (m02 + m20) / s;
                } else if (m11 > m22) {
                    float s = 2.0f * (float)Math.sqrt(1.0f + m11 - m00 - m22);
                    rotation.w = (m02 - m20) / s;
                    rotation.x = (m01 + m10) / s;
                    rotation.y = (m01 + m10) / s;
                    rotation.z = (m12 + m21) / s;
                } else {
                    float s = 2.0f * (float)Math.sqrt(1.0f + m22 - m00 - m11);
                    rotation.w = (m10 - m01) / s;
                    rotation.x = (m02 + m20) / s;
                    rotation.y = (m12 + m21) / s;
                    rotation.z = 0.25f * s;
                }
                
                Vector3f translation = new Vector3f(posX, posY, posZ);
                
                Transformation transformation = new Transformation(
                    translation,
                    rotation,
                    new Vector3f(0.8f, 0.8f, 0.8f),  // Slightly smaller blocks
                    new Quaternionf()
                );
                blossomDisplays[i].setTransformation(transformation);
                
                // Teleport the display to player's current location to follow player movement
                Location teleportLoc = center.clone();
                // Calibrated offset: From your data, when player at (23.5, 80, -17.5), blossoms appeared at (22.5, 80, -18.5)
                // That's offset of (-1, 0, -1). Now when you're at (14.701, 86, -26.299), blossoms appear at (16.571, 86, -24.474)
                // That's offset of (-1.87, 0, -1.82). Let's use a more centered offset.
                teleportLoc.add(-0.3f, 0.0f, -0.2f);
                blossomDisplays[i].teleport(teleportLoc);
            }
            }

            elapsedTicks++;

            // Check if duration has ended
            int totalTicks = durationSeconds * 20; // Convert seconds to ticks
            if (elapsedTicks >= totalTicks) {
                cancel();
                // Finish the duration effect and start the cooldown
                finishShieldEffect(player.getUniqueId());
            }
        }

        public Player getPlayer() {
            return player;
        }
    }

    private void finishShieldEffect(UUID playerId) {
        Player player = Bukkit.getPlayer(playerId);
        if (player == null) return;

        // Remove the duration boss bar
        cooldownManager.removeCooldown(playerId, "lotus-shield-duration");

        // Start the cooldown now that the effect has finished
        long now = System.currentTimeMillis();
        shieldCooldowns.put(playerId, now);
        String cooldownTitle = "§d§lLotus Blossom: Shield"; // Changed title to "Shield" instead of "Cooldown"
        cooldownManager.startCooldown(player, "lotus-shield-cooldown", cooldownTitle, shieldCooldown * 1000L, BarColor.PURPLE);

        // Notify player that shield ended
        player.sendMessage("§d✦ §5Blossom Shield has ended!");
        player.playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_BREAK, 0.6f, 0.8f);
        
        // Clean up the shield
        cleanupShield(playerId);
    }

    private void cleanupShield(UUID playerId) {
        // Stop the shield effect for this player
        BlossomShieldTask task = activeShields.remove(playerId);
        if (task != null) {
            // Remove all the block display entities
            for (BlockDisplay display : task.blossomDisplays) {
                if (display != null && !display.isDead()) {
                    display.remove();
                }
            }
            task.cancel();
        }
    }

    public void resetCooldown(UUID playerId) {
        // Reset the cooldown time for the player
        shieldCooldowns.put(playerId, 0L);
        
        // Remove the cooldown boss bar
        cooldownManager.removeCooldown(playerId, "lotus-shield-cooldown");
        
        // Also remove the duration boss bar if it exists
        cooldownManager.removeCooldown(playerId, "lotus-shield-duration");
    }

    public void resetCooldown(Player player) {
        if (player != null) {
            resetCooldown(player.getUniqueId());
        }
    }

    public void resetAllCooldowns() {
        shieldCooldowns.clear();
        cooldownManager.clearAllCooldowns();
    }
}