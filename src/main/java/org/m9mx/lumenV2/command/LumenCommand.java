package org.m9mx.lumenV2.command;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.event.HoverEvent;

import org.m9mx.lumenV2.Lumen;
import org.m9mx.lumenV2.config.ConfigManager;
import org.m9mx.lumenV2.config.ItemsConfig;
import org.m9mx.lumenV2.item.CustomItem;
import org.m9mx.lumenV2.item.ItemAttributes;
import org.m9mx.lumenV2.item.ItemRegistry;
import org.m9mx.lumenV2.item.Recipe;
import org.m9mx.lumenV2.systems.CooldownBossBarManager;
import org.m9mx.lumenV2.systems.EnhancementSystem;
import org.m9mx.lumenV2.systems.ForgeSystem;
import org.m9mx.lumenV2.systems.ItemProtection;
import org.m9mx.lumenV2.systems.protection.ItemProtectionMode;
import org.m9mx.lumenV2.util.BundleUtil;
import org.m9mx.lumenV2.util.ItemDataHelper;

import java.util.ArrayList;
import java.util.List;

/**
 * Main /lumen command handler
 */
public class LumenCommand implements CommandExecutor, TabCompleter {
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage("§e/lumen item give <item> [amount] [player]");
            sender.sendMessage("§e/lumen item info <item>");
            sender.sendMessage("§e/lumen item cooldown [player]");
            sender.sendMessage("§e/lumen item recipe <item> [true/false]");
            sender.sendMessage("§e/lumen item enable <item> [true/false]");
            return true;
        }
        
        if (args[0].equalsIgnoreCase("item")) {
            if (args.length < 2) {
                sender.sendMessage("§e/lumen item give <item> [amount] [player]");
                sender.sendMessage("§e/lumen item info <item>");
                sender.sendMessage("§e/lumen item cooldown [player]");
                sender.sendMessage("§e/lumen item recipe <item> [true/false]");
                sender.sendMessage("§e/lumen item enable <item> [true/false]");
                return true;
            }
            
            if (args[1].equalsIgnoreCase("give")) {
                return handleItemGive(sender, args);
            } else if (args[1].equalsIgnoreCase("info")) {
                return handleItemInfo(sender, args);
            } else if (args[1].equalsIgnoreCase("cooldown")) {
                return handleItemCooldown(sender, args);
            } else if (args[1].equalsIgnoreCase("recipe")) {
                return handleRecipeToggle(sender, args);
            } else if (args[1].equalsIgnoreCase("enable")) {
                return handleItemEnableToggle(sender, args);
            } else {
                sender.sendMessage("§cUnknown subcommand. Use: /lumen item give, /lumen item info, /lumen item cooldown, /lumen item recipe, or /lumen item enable");
                return true;
            }
        } else if (args[0].equalsIgnoreCase("reload")) {
            return handleReload(sender, args);
        }
        
        sender.sendMessage("§cUnknown command. Use: /lumen item give or /lumen reload");
        return true;
    }
    
    private boolean handleItemGive(CommandSender sender, String[] args) {
        // /lumen item give <item> [amount] [player]
        if (args.length < 3) {
            sender.sendMessage("§cUsage: /lumen item give <item> [amount] [player]");
            return true;
        }
        
        String itemId = args[2];
        int amount = 1;
        Player targetPlayer = null;
        
        // Parse amount if provided
        if (args.length >= 4) {
            try {
                amount = Integer.parseInt(args[3]);
                if (amount < 1) {
                    sender.sendMessage("§cAmount must be at least 1");
                    return true;
                }
            } catch (NumberFormatException e) {
                sender.sendMessage("§cInvalid amount: " + args[3]);
                return true;
            }
        }
        
        // Parse player if provided
        if (args.length >= 5) {
            targetPlayer = sender.getServer().getPlayer(args[4]);
            if (targetPlayer == null) {
                sender.sendMessage("§cPlayer not found: " + args[4]);
                return true;
            }
        } else {
            if (!(sender instanceof Player)) {
                sender.sendMessage("§cYou must specify a player when running from console");
                return true;
            }
            targetPlayer = (Player) sender;
        }
        
        // Check if item exists
        if (!ItemDataHelper.itemExists(itemId)) {
            sender.sendMessage("§cItem not found: " + itemId);
            return true;
        }
        
        // Give the item
        for (int i = 0; i < amount; i++) {
            org.bukkit.inventory.ItemStack item = ItemDataHelper.createItem(itemId);
            if (item == null) {
                sender.sendMessage("§cCould not create item: " + itemId);
                return true;
            }
            targetPlayer.getInventory().addItem(item);
        }
        
        sender.sendMessage("§aGave " + amount + "x " + itemId + " to " + targetPlayer.getName());
        targetPlayer.sendMessage("§aYou received " + amount + "x " + itemId);
        return true;
    }
    
    private boolean handleReload(CommandSender sender, String[] args) {
        // /lumen reload [type]
        String type = args.length >= 2 ? args[1].toLowerCase() : "all";
        
        sender.sendMessage("§6Reloading...");
        
        if (type.equals("config") || type.equals("all")) {
            ConfigManager.getInstance().getMainConfig().reload();
            sender.sendMessage("§aReloaded config.yml");
        }
        
        if (type.equals("systems") || type.equals("all")) {
            ConfigManager.getInstance().getSystemsConfig().reload();
            ItemProtection.getInstance().loadConfig();
            EnhancementSystem.getInstance().loadConfig();
            ForgeSystem.getInstance().loadConfig();
            sender.sendMessage("§aReloaded systems.yml");
        }
        
        if (type.equals("items") || type.equals("all")) {
            ConfigManager.getInstance().getItemsConfig().reload();
            
            // Reload enabled status for all items
            for (org.m9mx.lumenV2.item.CustomItem item : org.m9mx.lumenV2.item.ItemRegistry.getAllItems()) {
                item.reloadEnabledStatus();
            }
            
            sender.sendMessage("§aReloaded items.yml and updated item enabled statuses");
        }
        
        if (!type.equals("config") && !type.equals("systems") && !type.equals("items") && !type.equals("all")) {
            sender.sendMessage("§cUnknown reload type: " + type);
            sender.sendMessage("§eUsage: /lumen reload [config|systems|items|all]");
            return true;
        }
        
        sender.sendMessage("§aReload complete!");
        return true;
    }
    
    private boolean handleItemInfo(CommandSender sender, String[] args) {
        // /lumen item info <item>
        if (args.length < 3) {
            sender.sendMessage("§cUsage: /lumen item info <item>");
            return true;
        }
        
        String itemId = args[2];
        
        // Check if item exists
        if (!ItemDataHelper.itemExists(itemId)) {
            sender.sendMessage("§cItem not found: " + itemId);
            return true;
        }
        
        // Send header
        sender.sendMessage("§e§m" + "─".repeat(50));
        sender.sendMessage("§6Item Info: §e" + itemId);
        sender.sendMessage("§e§m" + "─".repeat(50));
        
        // Basic info
        sender.sendMessage("§7Material: §f" + ItemDataHelper.getMaterial(itemId));
        sender.sendMessage("§7Item Model: §f" + (ItemDataHelper.getItemModel(itemId) != null ? ItemDataHelper.getItemModel(itemId) : "None"));
        
        // Display name
        Component displayName = ItemDataHelper.getDisplayName(itemId);
        if (displayName != null) {
            sender.sendMessage("§7Display Name: §r" + net.kyori.adventure.text.serializer.plain.PlainComponentSerializer.plain().serialize(displayName)); // Convert to plain text
        }
        
        // Attributes
        ItemAttributes attrs = ItemDataHelper.getAttributes(itemId);
        if (attrs != null && (attrs.getAttackDamage() != 0 || attrs.getAttackSpeed() != 0 || attrs.getArmor() != 0 || attrs.getHealth() != 0 ||
                attrs.getArmorToughness() != 0 || attrs.getAttackKnockback() != 0 || attrs.getAttackReach() != 2.5 || 
                attrs.getBlockBreakSpeed() != 1 || attrs.getBlockInteractionRange() != 4.5 || attrs.getBurningTime() != 1 ||
                attrs.getCameraDistance() != 4 || attrs.getEntityInteractionRange() != 3 || attrs.getExplosionKnockbackResistance() != 0 ||
                attrs.getFallDamageMultiplier() != 1 || attrs.getFlyingSpeed() != 0.4 || attrs.getFollowRange() != 32 ||
                attrs.getGravity() != 0.08 || attrs.getJumpStrength() != 0.42 || attrs.getKnockbackResistance() != 0 ||
                attrs.getLuck() != 0 || attrs.getMaxAbsorption() != 0 || attrs.getMaxHealth() != 20 ||
                attrs.getMiningEfficiency() != 0 || attrs.getMovementEfficiency() != 0 || attrs.getMovementSpeed() != 0.7 ||
                attrs.getOxygenBonus() != 0 || attrs.getSafeFallDistance() != 3 || attrs.getScale() != 1 ||
                attrs.getSpawnReinforcements() != 0 || attrs.getSneakingSpeed() != 0.3 || attrs.getStepHeight() != 0.6 ||
                attrs.getSubmergedMiningSpeed() != 0.2 || attrs.getSweepingDamageRatio() != 0 || attrs.getTemptRange() != 10 ||
                attrs.getWaterMovementEfficiency() != 0 || attrs.getWaypointReceiveRange() != 0 || attrs.getWaypointTransmitRange() != 0)) {
            sender.sendMessage("§7Attributes:");
            if (attrs.getAttackDamage() != 0) {
                sender.sendMessage("  §f➤ Attack Damage: §6" + attrs.getAttackDamage());
            }
            if (attrs.getAttackSpeed() != 0) {
                sender.sendMessage("  §f➤ Attack Speed: §6" + attrs.getAttackSpeed());
            }
            if (attrs.getArmor() != 0) {
                sender.sendMessage("  §f➤ Armor: §6" + attrs.getArmor());
            }
            if (attrs.getHealth() != 0) {
                sender.sendMessage("  §f➤ Health: §6" + attrs.getHealth());
            }
            if (attrs.getArmorToughness() != 0) {
                sender.sendMessage("  §f➤ Armor Toughness: §6" + attrs.getArmorToughness());
            }
            if (attrs.getAttackKnockback() != 0) {
                sender.sendMessage("  §f➤ Attack Knockback: §6" + attrs.getAttackKnockback());
            }
            if (attrs.getAttackReach() != 2.5) {
                sender.sendMessage("  §f➤ Attack Reach: §6" + attrs.getAttackReach());
            }
            if (attrs.getBlockBreakSpeed() != 1) {
                sender.sendMessage("  §f➤ Block Break Speed: §6" + attrs.getBlockBreakSpeed());
            }
            if (attrs.getBlockInteractionRange() != 4.5) {
                sender.sendMessage("  §f➤ Block Interaction Range: §6" + attrs.getBlockInteractionRange());
            }
            if (attrs.getBurningTime() != 1) {
                sender.sendMessage("  §f➤ Burning Time: §6" + attrs.getBurningTime());
            }
            if (attrs.getCameraDistance() != 4) {
                sender.sendMessage("  §f➤ Camera Distance: §6" + attrs.getCameraDistance());
            }
            if (attrs.getEntityInteractionRange() != 3) {
                sender.sendMessage("  §f➤ Entity Interaction Range: §6" + attrs.getEntityInteractionRange());
            }
            if (attrs.getExplosionKnockbackResistance() != 0) {
                sender.sendMessage("  §f➤ Explosion Knockback Resistance: §6" + attrs.getExplosionKnockbackResistance());
            }
            if (attrs.getFallDamageMultiplier() != 1) {
                sender.sendMessage("  §f➤ Fall Damage Multiplier: §6" + attrs.getFallDamageMultiplier());
            }
            if (attrs.getFlyingSpeed() != 0.4) {
                sender.sendMessage("  §f➤ Flying Speed: §6" + attrs.getFlyingSpeed());
            }
            if (attrs.getFollowRange() != 32) {
                sender.sendMessage("  §f➤ Follow Range: §6" + attrs.getFollowRange());
            }
            if (Math.abs(attrs.getGravity() - 0.08) > 0.0001) {
                sender.sendMessage("  §f➤ Gravity: §6" + attrs.getGravity());
            }
            if (Math.abs(attrs.getJumpStrength() - 0.42) > 0.0001) {
                sender.sendMessage("  §f➤ Jump Strength: §6" + attrs.getJumpStrength());
            }
            if (attrs.getKnockbackResistance() != 0) {
                sender.sendMessage("  §f➤ Knockback Resistance: §6" + attrs.getKnockbackResistance());
            }
            if (attrs.getLuck() != 0) {
                sender.sendMessage("  §f➤ Luck: §6" + attrs.getLuck());
            }
            if (attrs.getMaxAbsorption() != 0) {
                sender.sendMessage("  §f➤ Max Absorption: §6" + attrs.getMaxAbsorption());
            }
            if (attrs.getMaxHealth() != 20) {
                sender.sendMessage("  §f➤ Max Health: §6" + attrs.getMaxHealth());
            }
            if (attrs.getMiningEfficiency() != 0) {
                sender.sendMessage("  §f➤ Mining Efficiency: §6" + attrs.getMiningEfficiency());
            }
            if (attrs.getMovementEfficiency() != 0) {
                sender.sendMessage("  §f➤ Movement Efficiency: §6" + attrs.getMovementEfficiency());
            }
            if (Math.abs(attrs.getMovementSpeed() - 0.7) > 0.0001) {
                sender.sendMessage("  §f➤ Movement Speed: §6" + attrs.getMovementSpeed());
            }
            if (attrs.getOxygenBonus() != 0) {
                sender.sendMessage("  §f➤ Oxygen Bonus: §6" + attrs.getOxygenBonus());
            }
            if (attrs.getSafeFallDistance() != 3) {
                sender.sendMessage("  §f➤ Safe Fall Distance: §6" + attrs.getSafeFallDistance());
            }
            if (attrs.getScale() != 1) {
                sender.sendMessage("  §f➤ Scale: §6" + attrs.getScale());
            }
            if (attrs.getSpawnReinforcements() != 0) {
                sender.sendMessage("  §f➤ Spawn Reinforcements: §6" + attrs.getSpawnReinforcements());
            }
            if (Math.abs(attrs.getSneakingSpeed() - 0.3) > 0.0001) {
                sender.sendMessage("  §f➤ Sneaking Speed: §6" + attrs.getSneakingSpeed());
            }
            if (attrs.getStepHeight() != 0.6) {
                sender.sendMessage("  §f➤ Step Height: §6" + attrs.getStepHeight());
            }
            if (Math.abs(attrs.getSubmergedMiningSpeed() - 0.2) > 0.0001) {
                sender.sendMessage("  §f➤ Submerged Mining Speed: §6" + attrs.getSubmergedMiningSpeed());
            }
            if (attrs.getSweepingDamageRatio() != 0) {
                sender.sendMessage("  §f➤ Sweeping Damage Ratio: §6" + attrs.getSweepingDamageRatio());
            }
            if (attrs.getTemptRange() != 10) {
                sender.sendMessage("  §f➤ Tempt Range: §6" + attrs.getTemptRange());
            }
            if (attrs.getWaterMovementEfficiency() != 0) {
                sender.sendMessage("  §f➤ Water Movement Efficiency: §6" + attrs.getWaterMovementEfficiency());
            }
            if (attrs.getWaypointReceiveRange() != 0) {
                sender.sendMessage("  §f➤ Waypoint Receive Range: §6" + attrs.getWaypointReceiveRange());
            }
            if (attrs.getWaypointTransmitRange() != 0) {
                sender.sendMessage("  §f➤ Waypoint Transmit Range: §6" + attrs.getWaypointTransmitRange());
            }
        }
        
        // Unbreakable
        if (ItemDataHelper.isUnbreakable(itemId)) {
            sender.sendMessage("§7Unbreakable: §aYes");
        } else {
            sender.sendMessage("§7Unbreakable: §cNo");
        }
        
        // Recipe
        if (ItemDataHelper.hasRecipe(itemId)) {
            Recipe recipe = ItemDataHelper.getRecipe(itemId);
            sender.sendMessage("§7Recipe Type: §f" + recipe.getType());
            sender.sendMessage("§7Recipe Grid:");
            Recipe.RecipeSlot[][] grid = recipe.getGrid();
            for (int row = 0; row < 3; row++) {
                StringBuilder line = new StringBuilder("  ");
                for (int col = 0; col < 3; col++) {
                    Recipe.RecipeSlot slot = grid[row][col];
                    if (slot != null) {
                        line.append("§e[").append(slot.getItemId()).append("x").append(slot.getAmount()).append("]§7 ");
                    } else {
                        line.append("§7[—]§7 ");
                    }
                }
                sender.sendMessage(line.toString());
            }
            sender.sendMessage("§7Result: §e" + recipe.getResult());
        } else {
            sender.sendMessage("§7Recipe: §cNone");
        }
        
        // Enhancement & Protection
        sender.sendMessage("§7Enhancable: §f" + (ItemDataHelper.isEnhancable(itemId) ? "§aYes" : "§cNo"));
        sender.sendMessage("§7Protection Mode: §f" + ItemDataHelper.getProtectionMode(itemId));
        
        // Separator
        sender.sendMessage("§e§m" + "─".repeat(50));
        
        // Item name with lore hover at bottom
        Component itemNameComponent = ItemDataHelper.getDisplayName(itemId);
        if (itemNameComponent == null) {
            itemNameComponent = Component.text(itemId);
        }
        
        List<Component> lore = ItemDataHelper.getLore(itemId);
        if (lore != null && !lore.isEmpty()) {
            // Create hover text with lore
            Component loreComponent = Component.empty();
            for (int i = 0; i < lore.size(); i++) {
                if (i > 0) {
                    loreComponent = loreComponent.append(Component.newline());
                }
                loreComponent = loreComponent.append(lore.get(i));
            }
            
            // Create clickable item name with hover
            Component clickableItem = itemNameComponent
                .hoverEvent(HoverEvent.showText(Component.text("Lore:", NamedTextColor.GOLD).append(Component.newline()).append(loreComponent)));
            
            sender.sendMessage(Component.text("Hover over name to see lore: ", NamedTextColor.GRAY).append(clickableItem));
        } else {
            sender.sendMessage(Component.text("Item: ", NamedTextColor.GRAY).append(itemNameComponent));
        }
        
        return true;
    }
    
    private boolean handleItemCooldown(CommandSender sender, String[] args) {
        // /lumen item cooldown
        Player targetPlayer = null;
        
        // Get target player
        if (args.length >= 3) {
            targetPlayer = sender.getServer().getPlayer(args[2]);
            if (targetPlayer == null) {
                sender.sendMessage("§cPlayer not found: " + args[2]);
                return true;
            }
        } else {
            if (!(sender instanceof Player)) {
                sender.sendMessage("§cYou must specify a player when running from console");
                return true;
            }
            targetPlayer = (Player) sender;
        }
        
        // Reset cooldowns for the specific player
        CooldownBossBarManager.getInstance().removeAllCooldowns(targetPlayer);
        Lumen plugin = Lumen.getPlugin(Lumen.class);
        if (plugin != null) {
            if (plugin.getEtherealKatanaAbility() != null) {
                plugin.getEtherealKatanaAbility().resetCooldown(targetPlayer);
            }
            if (plugin.getSolsticeAbility() != null) {
                plugin.getSolsticeAbility().resetCooldown(targetPlayer);
            }
            if (plugin.getSoulrenderAbility() != null) {
                plugin.getSoulrenderAbility().resetCooldowns(targetPlayer); // Note: uses resetCooldowns instead of resetCooldown
            }
            if (plugin.getLotusBlossomAbility() != null) {
                plugin.getLotusBlossomAbility().resetCooldown(targetPlayer);
            }
            // Note: ReinforcedMeshAbility doesn't have a resetCooldown method
            if (plugin.getAwakenedLichbladeAbility() != null) {
                plugin.getAwakenedLichbladeAbility().resetCooldown(targetPlayer);
            }
        }
        
        sender.sendMessage("§aReset cooldowns for " + targetPlayer.getName());
        return true;
    }
    
    private boolean handleRecipeToggle(CommandSender sender, String[] args) {
        // /lumen item recipe <item> [true/false]
        if (args.length < 3) {
            sender.sendMessage("§cUsage: /lumen item recipe <item> [true/false]");
            return true;
        }

        String itemId = args[2].toLowerCase(); // Convert to lowercase to match registry keys
        boolean newState;

        // Check if a state was provided
        if (args.length == 4) {
            if (args[3].equalsIgnoreCase("true")) {
                newState = true;
            } else if (args[3].equalsIgnoreCase("false")) {
                newState = false;
            } else {
                sender.sendMessage("§cInvalid state: " + args[3] + ". Use 'true' or 'false'.");
                return true;
            }
        } else {
            // Toggle the current state
            boolean currentState = ForgeSystem.getInstance().isRecipeEnabled(itemId);
            newState = !currentState;
        }

        // Set the new recipe state
        ForgeSystem.getInstance().setRecipeEnabled(itemId, newState);
        
        // Update the systems.yml config
        ForgeSystem.getInstance().updateRecipeConfig(itemId, newState);
        
        // Reload the forge system config to refresh the cache
        ForgeSystem.getInstance().loadConfig();

        sender.sendMessage("§aRecipe for '" + itemId + "' is now " + (newState ? "enabled" : "disabled") + ".");
        return true;
    }
    
    private boolean handleItemEnableToggle(CommandSender sender, String[] args) {
        // /lumen item enable <item> [true/false]
        if (args.length < 3) {
            sender.sendMessage("§cUsage: /lumen item enable <item> [true/false]");
            return true;
        }

        String itemId = args[2].toLowerCase(); // Convert to lowercase to match registry keys
        boolean newState;

        // Check if a state was provided
        if (args.length == 4) {
            if (args[3].equalsIgnoreCase("true")) {
                newState = true;
            } else if (args[3].equalsIgnoreCase("false")) {
                newState = false;
            } else {
                sender.sendMessage("§cInvalid state: " + args[3] + ". Use 'true' or 'false'.");
                return true;
            }
        } else {
            // Toggle the current state
            CustomItem item = ItemRegistry.getInstance().getItem(itemId);
            if (item != null) {
                newState = !item.isEnabled();
            } else {
                sender.sendMessage("§cItem not found: " + itemId);
                return true;
            }
        }

        // Update the items.yml config
        updateItemEnabledStatus(itemId, newState);
        
        // Reload the enabled status for all items to reflect the change
        for (CustomItem item : ItemRegistry.getAllItems()) {
            item.reloadEnabledStatus();
        }

        sender.sendMessage("§aItem '" + itemId + "' is now " + (newState ? "enabled" : "disabled") + ".");
        return true;
    }
    
    /**
     * Update the enabled status of an item in the items.yml config file
     */
    private void updateItemEnabledStatus(String itemId, boolean enabled) {
        ItemsConfig itemsConfig = ConfigManager.getInstance().getItemsConfig();
        itemsConfig.getConfig().set(itemId + ".enabled", enabled);
        itemsConfig.save();
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length <= 1) {
            // First arg: "item" or "reload"
            completions.add("item");
            completions.add("reload");
        } else if (args.length == 2 && args[0].equalsIgnoreCase("item")) {
            // Second arg: "give", "info", "cooldown", "recipe", or "enable"
            completions.add("give");
            completions.add("info");
            completions.add("cooldown");
            completions.add("recipe");
            completions.add("enable");
        } else if (args.length == 2 && args[0].equalsIgnoreCase("reload")) {
            // Second arg for reload: type
            completions.add("config");
            completions.add("systems");
            completions.add("items");
            completions.add("all");
        } else if (args.length == 3 && args[0].equalsIgnoreCase("item") && (args[1].equalsIgnoreCase("give") || args[1].equalsIgnoreCase("info") || args[1].equalsIgnoreCase("recipe") || args[1].equalsIgnoreCase("enable"))) {
            // Third arg: item IDs (for give, info, recipe, and enable)
            completions.addAll(ItemDataHelper.getAllItemIds());
        } else if (args.length == 4 && args[0].equalsIgnoreCase("item") && args[1].equalsIgnoreCase("recipe")) {
            // Fourth arg for recipe: true or false
            completions.add("true");
            completions.add("false");
        } else if (args.length == 4 && args[0].equalsIgnoreCase("item") && args[1].equalsIgnoreCase("enable")) {
            // Fourth arg for enable: true or false
            completions.add("true");
            completions.add("false");
        } else if (args.length == 4 && args[0].equalsIgnoreCase("item") && args[1].equalsIgnoreCase("give")) {
            // Fourth arg: amount (just suggest some numbers)
            completions.add("1");
            completions.add("16");
            completions.add("64");
        } else if (args.length == 5 && args[0].equalsIgnoreCase("item") && args[1].equalsIgnoreCase("give")) {
            // Fifth arg: player names
            for (Player player : sender.getServer().getOnlinePlayers()) {
                completions.add(player.getName());
            }
        } else if (args.length == 3 && args[0].equalsIgnoreCase("item") && args[1].equalsIgnoreCase("cooldown")) {
            // Third arg for cooldown: player names
            for (Player player : sender.getServer().getOnlinePlayers()) {
                completions.add(player.getName());
            }
        }
        
        return completions;
    }
}