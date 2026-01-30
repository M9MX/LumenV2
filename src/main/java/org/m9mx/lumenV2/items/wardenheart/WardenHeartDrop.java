package org.m9mx.lumenV2.items.wardenheart;

import org.bukkit.entity.EntityType;
import org.m9mx.lumenV2.util.MobDropHandler;

/**
 * Handles the Warden Heart drop from Wardens
 * According to the spec: 25% chance to drop on Warden death by player
 */
public class WardenHeartDrop extends MobDropHandler {
    
    private static WardenHeartDrop instance;
    
    public WardenHeartDrop() {
        // Configure the drop for Warden: 25% chance to drop Warden Heart when killed by player
        addDropConfig(new DropConfig(EntityType.WARDEN, "warden_heart", 0.25));
    }
    
    public static WardenHeartDrop getInstance() {
        if (instance == null) {
            instance = new WardenHeartDrop();
        }
        return instance;
    }
}