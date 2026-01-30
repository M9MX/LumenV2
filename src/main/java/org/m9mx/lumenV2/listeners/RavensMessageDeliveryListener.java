package org.m9mx.lumenV2.listeners;

import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.m9mx.lumenV2.data.RavensMessageDataManager;

import java.util.List;

public class RavensMessageDeliveryListener implements Listener {

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        
        // Get pending mails for this player
        List<RavensMessageDataManager.PendingMail> pendingMails = 
            RavensMessageDataManager.getPendingMails(player.getName());
        
        if (pendingMails.isEmpty()) {
            return;
        }

        // Deliver all pending mails
        for (RavensMessageDataManager.PendingMail mail : pendingMails) {
            player.getInventory().addItem(mail.item);
            player.sendMessage("§b" + mail.senderName + " sent you an item via Raven's Message!");
            
            // Play delivery sound
            player.playSound(player.getLocation(), Sound.BLOCK_BELL_USE, SoundCategory.PLAYERS, 1.0f, 1.0f);
        }
    }
}
