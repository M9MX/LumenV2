package org.m9mx.lumenV2.data;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.m9mx.lumenV2.util.ItemDataHelper;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class RavensMessageDataManager {

    private static File dataFile;
    private static FileConfiguration mailData;

    public static void initialize(File pluginFolder) {
        dataFile = new File(pluginFolder, "data/ravens_messages.yml");
        
        if (!dataFile.getParentFile().exists()) {
            dataFile.getParentFile().mkdirs();
        }
        
        if (!dataFile.exists()) {
            try {
                dataFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        
        loadData();
    }

    public static void loadData() {
        if (dataFile == null) return;
        mailData = YamlConfiguration.loadConfiguration(dataFile);
    }

    public static void saveData() {
        if (mailData == null || dataFile == null) return;
        try {
            mailData.save(dataFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Save a pending mail for a player who is currently offline
     */
    public static void savePendingMail(String playerName, ItemStack item, String senderName) {
        if (mailData == null) return;
        
        String serialized = ItemDataHelper.serializeItem(item);
        if (serialized == null || serialized.isEmpty()) {
            return;
        }

        // Create path: pending_mails.PlayerName.mails[0].item, sender, timestamp
        String mailsPath = "pending_mails." + playerName + ".mails";
        
        List<String> existingMails = mailData.getStringList(mailsPath);
        if (existingMails == null) {
            existingMails = new ArrayList<>();
        }

        // Store as compound: item_data|sender_name|timestamp
        String mailEntry = serialized + "|" + senderName + "|" + System.currentTimeMillis();
        existingMails.add(mailEntry);

        mailData.set(mailsPath, existingMails);
        saveData();
    }

    /**
     * Get all pending mails for a player and clear them
     */
    public static List<PendingMail> getPendingMails(String playerName) {
        if (mailData == null) return new ArrayList<>();
        
        List<String> mails = mailData.getStringList("pending_mails." + playerName + ".mails");
        List<PendingMail> result = new ArrayList<>();

        for (String mailEntry : mails) {
            String[] parts = mailEntry.split("\\|");
            if (parts.length >= 3) {
                String itemData = parts[0];
                String senderName = parts[1];
                
                ItemStack item = ItemDataHelper.deserializeItem(itemData);
                if (item != null) {
                    result.add(new PendingMail(item, senderName));
                }
            }
        }

        // Clear the mails for this player
        mailData.set("pending_mails." + playerName, null);
        saveData();

        return result;
    }

    /**
     * Data class for pending mails
     */
    public static class PendingMail {
        public final ItemStack item;
        public final String senderName;

        public PendingMail(ItemStack item, String senderName) {
            this.item = item;
            this.senderName = senderName;
        }
    }
}
