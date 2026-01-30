package org.m9mx.lumenV2.systems.protection;

/**
 * Item protection modes
 */
public enum ItemProtectionMode {
    NONE,      // No protection
    SIMPLE,    // Blocks placement in enderchest, furnace, bundle
    STRICT     // SIMPLE + logs item locations in file
}
