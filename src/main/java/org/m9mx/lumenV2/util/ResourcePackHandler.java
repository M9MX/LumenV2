package org.m9mx.lumenV2.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerResourcePackStatusEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.util.Formatter;
import com.sun.net.httpserver.HttpServer;

public class ResourcePackHandler implements Listener {
    private final JavaPlugin plugin;
    private final File packFile;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();
    private static final String PACK_FILENAME = "resourcepack.zip";
    
    private String mode;
    private String customUrl;
    private String customSha1;
    private String autoHostSha1;
    private int port;
    
    public ResourcePackHandler(JavaPlugin plugin) {
        this.plugin = plugin;
        this.packFile = new File(plugin.getDataFolder(), PACK_FILENAME);
        
        loadConfiguration();
        
        if (mode.equalsIgnoreCase("auto_host")) {
            extractResourcePack();
            startHttpServer();
        }
    }
    
    private void loadConfiguration() {
        String configMode = plugin.getConfig().getString("resource_pack.mode", "none");
        this.mode = configMode.toLowerCase();
        
        if (mode.equals("auto_host")) {
            this.port = plugin.getConfig().getInt("resource_pack.auto_host.port", 8080);
            plugin.getLogger().info("Resource pack mode: AUTO_HOST on port " + port);
        } else if (mode.equals("self_host")) {
            this.customUrl = plugin.getConfig().getString("resource_pack.self_host.url", "");
            this.customSha1 = plugin.getConfig().getString("resource_pack.self_host.sha1", "");
            plugin.getLogger().info("Resource pack mode: SELF_HOST at " + customUrl);
        } else {
            plugin.getLogger().info("Resource pack mode: NONE (players will see default textures without custom packs)");
        }
    }
    
    private void extractResourcePack() {
        try (InputStream in = plugin.getResource(PACK_FILENAME)) {
            if (in != null) {
                plugin.getDataFolder().mkdirs();
                Files.copy(in, packFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                plugin.getLogger().info("Resource pack extracted to: " + packFile.getAbsolutePath());
                
                // Calculate SHA-1 hash for auto host
                this.autoHostSha1 = calculateSHA1(packFile);
                plugin.getLogger().info("Resource pack SHA-1: " + autoHostSha1);
            } else {
                plugin.getLogger().warning("Resource pack file not found in JAR. Make sure src/main/resources/pack exists and is zipped properly.");
            }
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to extract resource pack: " + e.getMessage());
        }
    }
    
    private String calculateSHA1(File file) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            byte[] fileBytes = Files.readAllBytes(file.toPath());
            byte[] hashBytes = digest.digest(fileBytes);
            
            Formatter formatter = new Formatter();
            for (byte b : hashBytes) {
                formatter.format("%02x", b);
            }
            return formatter.toString();
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to calculate SHA-1: " + e.getMessage());
            return "";
        }
    }
    
    private void startHttpServer() {
        new Thread(() -> {
            try {
                HttpServer server = HttpServer.create(new java.net.InetSocketAddress(port), 0);
                server.createContext("/resourcepack.zip", exchange -> {
                    try {
                        if (!packFile.exists()) {
                            exchange.sendResponseHeaders(404, 0);
                            exchange.close();
                            return;
                        }
                        
                        byte[] fileBytes = Files.readAllBytes(packFile.toPath());
                        exchange.getResponseHeaders().set("Content-Type", "application/zip");
                        exchange.getResponseHeaders().set("Content-Length", String.valueOf(fileBytes.length));
                        exchange.sendResponseHeaders(200, fileBytes.length);
                        exchange.getResponseBody().write(fileBytes);
                        exchange.close();
                    } catch (IOException e) {
                        plugin.getLogger().severe("Error serving resource pack: " + e.getMessage());
                    }
                });
                
                server.setExecutor(null);
                server.start();
                plugin.getLogger().info("Resource pack HTTP server started on port " + port);
            } catch (IOException e) {
                plugin.getLogger().severe("Failed to start HTTP server on port " + port + ": " + e.getMessage());
            }
        }).start();
    }
    
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        
        if (mode.equals("auto_host")) {
            if (packFile.exists() && autoHostSha1 != null && !autoHostSha1.isEmpty()) {
                try {
                    String serverIp = java.net.InetAddress.getLocalHost().getHostAddress();
                    String packUrl = "http://" + serverIp + ":" + port + "/resourcepack.zip";
                    
                    player.setResourcePack(packUrl, autoHostSha1, true);
                    plugin.getLogger().info("Resource pack sent to " + player.getName() + " at " + packUrl);
                } catch (java.net.UnknownHostException e) {
                    plugin.getLogger().severe("Failed to get server IP: " + e.getMessage());
                }
            }
        } else if (mode.equals("self_host")) {
            if (customUrl != null && !customUrl.isEmpty() && customSha1 != null && !customSha1.isEmpty()) {
                player.setResourcePack(customUrl, customSha1, true);
                plugin.getLogger().info("Resource pack sent to " + player.getName() + " from " + customUrl);
            }
        } else if (mode.equals("none")) {
            Component message = miniMessage.deserialize(
                "<yellow>[Lumen] <gray>Resource pack is not configured. You will see default player heads without custom textures."
            );
            player.sendMessage(message);
        }
    }
    
    @EventHandler
    public void onResourcePackStatus(PlayerResourcePackStatusEvent event) {
        Player player = event.getPlayer();
        
        // Only enforce if resource pack is enabled
        if (mode.equals("none")) {
            return;
        }
        
        if (event.getStatus() == PlayerResourcePackStatusEvent.Status.DECLINED) {
            Component message = miniMessage.deserialize(
                "<red>You must accept the resource pack to play on this server."
            );
            player.sendMessage(message);
            
            plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, () -> {
                player.kick(Component.text("You must accept the resource pack to play on this server."));
            }, 20L); // Kick after 1 second
        } else if (event.getStatus() == PlayerResourcePackStatusEvent.Status.FAILED_DOWNLOAD) {
            Component message = miniMessage.deserialize(
                "<red>Failed to download resource pack. Please check your connection and rejoin."
            );
            player.sendMessage(message);
            
            plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, () -> {
                player.kick(Component.text("Failed to download resource pack."));
            }, 20L);
        }
    }
    
    public File getResourcePackFile() {
        return packFile;
    }
    
    public boolean isResourcePackReady() {
        if (mode.equals("auto_host")) {
            return packFile.exists() && autoHostSha1 != null && !autoHostSha1.isEmpty();
        } else if (mode.equals("self_host")) {
            return customUrl != null && !customUrl.isEmpty() && customSha1 != null && !customSha1.isEmpty();
        }
        return false;
    }
    
    public String getMode() {
        return mode;
    }
}
