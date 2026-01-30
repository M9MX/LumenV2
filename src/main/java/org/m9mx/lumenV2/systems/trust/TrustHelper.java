package org.m9mx.lumenV2.systems.trust;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

/**
 * Utility helper for trust system checks
 */
public class TrustHelper {
    
    private static TrustSystem trustSystem;
    
    /**
     * Check if ability damage should be blocked between two entities
     * Returns true if entities are teammates/allies
     */
    public static boolean shouldBlockAbilityDamage(LivingEntity attacker, LivingEntity defender) {
        // Only check for player-to-player damage
        if (!(attacker instanceof Player) || !(defender instanceof Player)) {
            return false;
        }
        
        Player attackerPlayer = (Player) attacker;
        Player defenderPlayer = (Player) defender;
        
        // Get trust system
        trustSystem = TrustSystem.getInstance();
        if (!trustSystem.isEnabled()) {
            return false;
        }
        
        TrustDataManager dataManager = trustSystem.getDataManager();
        String attackerUuid = attackerPlayer.getUniqueId().toString();
        String defenderUuid = defenderPlayer.getUniqueId().toString();
        
        // Get attacker's teams
        for (String teamId : dataManager.getPlayerTeams(attackerUuid)) {
            // Check if defender is in same team
            if (dataManager.isPlayerInTeam(defenderUuid, teamId)) {
                return true;
            }
            
            // Check if defender is in an allied team
            for (String allyTeamId : dataManager.getTeamAllies(teamId).keySet()) {
                if ("accepted".equals(dataManager.getTeamAllies(teamId).get(allyTeamId))) {
                    if (dataManager.isPlayerInTeam(defenderUuid, allyTeamId)) {
                        return true;
                    }
                }
            }
        }
        
        return false;
    }
}
