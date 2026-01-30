package org.m9mx.lumenV2.item;

/**
 * Default item attributes (damage, speed, etc.)
 */
public class ItemAttributes {
    // Current attributes
    private double attackDamage = 0;
    private double attackSpeed = 0;
    private double armor = 0;
    private double health = 0;
    
    // New attributes based on Minecraft's attribute system
    private double armorToughness = 0;                    // Default: 0
    private double attackKnockback = 0;                   // Default: 0
    private double attackReach = 2.5;                     // Default: 2.5
    private double blockBreakSpeed = 1;                   // Default: 1
    private double blockInteractionRange = 4.5;          // Default: 4.5
    private double burningTime = 1;                       // Default: 1
    private double cameraDistance = 4;                    // Default: 4
    private double entityInteractionRange = 3;            // Default: 3
    private double explosionKnockbackResistance = 0;      // Default: 0
    private double fallDamageMultiplier = 1;              // Default: 1
    private double flyingSpeed = 0.4;                     // Default: 0.4
    private double followRange = 32;                      // Default: 32
    private double gravity = 0.08;                        // Default: 0.08
    private double jumpStrength = 0.42;                   // Default: 0.42
    private double knockbackResistance = 0;               // Default: 0
    private double luck = 0;                              // Default: 0
    private double maxAbsorption = 0;                     // Default: 0
    private double maxHealth = 20;                        // Default: 20
    private double miningEfficiency = 0;                  // Default: 0
    private double movementEfficiency = 0;                // Default: 0
    private double movementSpeed = 0.7;                   // Default: 0.7
    private double oxygenBonus = 0;                       // Default: 0
    private double safeFallDistance = 3;                  // Default: 3
    private double scale = 1;                             // Default: 1
    private double spawnReinforcements = 0;               // Default: 0
    private double sneakingSpeed = 0.3;                   // Default: 0.3
    private double stepHeight = 0.6;                      // Default: 0.6
    private double submergedMiningSpeed = 0.2;            // Default: 0.2
    private double sweepingDamageRatio = 0;               // Default: 0
    private double temptRange = 10;                       // Default: 10
    private double waterMovementEfficiency = 0;           // Default: 0
    private double waypointReceiveRange = 0;              // Default: 0
    private double waypointTransmitRange = 0;             // Default: 0
    
    public ItemAttributes() {}
    
    // Getters and setters for existing attributes
    public double getAttackDamage() { return attackDamage; }
    public double getAttackSpeed() { return attackSpeed; }
    public double getArmor() { return armor; }
    public double getHealth() { return health; }
    
    public void setAttackDamage(double damage) { this.attackDamage = damage; }
    public void setAttackSpeed(double speed) { this.attackSpeed = speed; }
    public void setArmor(double armor) { this.armor = armor; }
    public void setHealth(double health) { this.health = health; }
    
    // Getters and setters for new attributes
    public double getArmorToughness() { return armorToughness; }
    public void setArmorToughness(double armorToughness) { this.armorToughness = armorToughness; }
    
    public double getAttackKnockback() { return attackKnockback; }
    public void setAttackKnockback(double attackKnockback) { this.attackKnockback = attackKnockback; }
    
    public double getAttackReach() { return attackReach; }
    public void setAttackReach(double attackReach) { this.attackReach = attackReach; }
    
    public double getBlockBreakSpeed() { return blockBreakSpeed; }
    public void setBlockBreakSpeed(double blockBreakSpeed) { this.blockBreakSpeed = blockBreakSpeed; }
    
    public double getBlockInteractionRange() { return blockInteractionRange; }
    public void setBlockInteractionRange(double blockInteractionRange) { this.blockInteractionRange = blockInteractionRange; }
    
    public double getBurningTime() { return burningTime; }
    public void setBurningTime(double burningTime) { this.burningTime = burningTime; }
    
    public double getCameraDistance() { return cameraDistance; }
    public void setCameraDistance(double cameraDistance) { this.cameraDistance = cameraDistance; }
    
    public double getEntityInteractionRange() { return entityInteractionRange; }
    public void setEntityInteractionRange(double entityInteractionRange) { this.entityInteractionRange = entityInteractionRange; }
    
    public double getExplosionKnockbackResistance() { return explosionKnockbackResistance; }
    public void setExplosionKnockbackResistance(double explosionKnockbackResistance) { this.explosionKnockbackResistance = explosionKnockbackResistance; }
    
    public double getFallDamageMultiplier() { return fallDamageMultiplier; }
    public void setFallDamageMultiplier(double fallDamageMultiplier) { this.fallDamageMultiplier = fallDamageMultiplier; }
    
    public double getFlyingSpeed() { return flyingSpeed; }
    public void setFlyingSpeed(double flyingSpeed) { this.flyingSpeed = flyingSpeed; }
    
    public double getFollowRange() { return followRange; }
    public void setFollowRange(double followRange) { this.followRange = followRange; }
    
    public double getGravity() { return gravity; }
    public void setGravity(double gravity) { this.gravity = gravity; }
    
    public double getJumpStrength() { return jumpStrength; }
    public void setJumpStrength(double jumpStrength) { this.jumpStrength = jumpStrength; }
    
    public double getKnockbackResistance() { return knockbackResistance; }
    public void setKnockbackResistance(double knockbackResistance) { this.knockbackResistance = knockbackResistance; }
    
    public double getLuck() { return luck; }
    public void setLuck(double luck) { this.luck = luck; }
    
    public double getMaxAbsorption() { return maxAbsorption; }
    public void setMaxAbsorption(double maxAbsorption) { this.maxAbsorption = maxAbsorption; }
    
    public double getMaxHealth() { return maxHealth; }
    public void setMaxHealth(double maxHealth) { this.maxHealth = maxHealth; }
    
    public double getMiningEfficiency() { return miningEfficiency; }
    public void setMiningEfficiency(double miningEfficiency) { this.miningEfficiency = miningEfficiency; }
    
    public double getMovementEfficiency() { return movementEfficiency; }
    public void setMovementEfficiency(double movementEfficiency) { this.movementEfficiency = movementEfficiency; }
    
    public double getMovementSpeed() { return movementSpeed; }
    public void setMovementSpeed(double movementSpeed) { this.movementSpeed = movementSpeed; }
    
    public double getOxygenBonus() { return oxygenBonus; }
    public void setOxygenBonus(double oxygenBonus) { this.oxygenBonus = oxygenBonus; }
    
    public double getSafeFallDistance() { return safeFallDistance; }
    public void setSafeFallDistance(double safeFallDistance) { this.safeFallDistance = safeFallDistance; }
    
    public double getScale() { return scale; }
    public void setScale(double scale) { this.scale = scale; }
    
    public double getSpawnReinforcements() { return spawnReinforcements; }
    public void setSpawnReinforcements(double spawnReinforcements) { this.spawnReinforcements = spawnReinforcements; }
    
    public double getSneakingSpeed() { return sneakingSpeed; }
    public void setSneakingSpeed(double sneakingSpeed) { this.sneakingSpeed = sneakingSpeed; }
    
    public double getStepHeight() { return stepHeight; }
    public void setStepHeight(double stepHeight) { this.stepHeight = stepHeight; }
    
    public double getSubmergedMiningSpeed() { return submergedMiningSpeed; }
    public void setSubmergedMiningSpeed(double submergedMiningSpeed) { this.submergedMiningSpeed = submergedMiningSpeed; }
    
    public double getSweepingDamageRatio() { return sweepingDamageRatio; }
    public void setSweepingDamageRatio(double sweepingDamageRatio) { this.sweepingDamageRatio = sweepingDamageRatio; }
    
    public double getTemptRange() { return temptRange; }
    public void setTemptRange(double temptRange) { this.temptRange = temptRange; }
    
    public double getWaterMovementEfficiency() { return waterMovementEfficiency; }
    public void setWaterMovementEfficiency(double waterMovementEfficiency) { this.waterMovementEfficiency = waterMovementEfficiency; }
    
    public double getWaypointReceiveRange() { return waypointReceiveRange; }
    public void setWaypointReceiveRange(double waypointReceiveRange) { this.waypointReceiveRange = waypointReceiveRange; }
    
    public double getWaypointTransmitRange() { return waypointTransmitRange; }
    public void setWaypointTransmitRange(double waypointTransmitRange) { this.waypointTransmitRange = waypointTransmitRange; }
}