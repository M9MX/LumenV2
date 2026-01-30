package org.m9mx.lumenV2.command;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.m9mx.lumenV2.systems.trust.TrustDataManager;
import org.m9mx.lumenV2.systems.trust.TrustSystem;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;

/**
 * /trust command handler
 */
public class TrustCommand implements CommandExecutor, TabCompleter {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cOnly players can use this command");
            return true;
        }

        Player player = (Player) sender;

        if (args.length == 0) {
            return sendHelp(player);
        }

        if (args[0].equalsIgnoreCase("team")) {
            return handleTeamCommand(player, args);
        } else if (args[0].equalsIgnoreCase("allies")) {
            return handleAlliesCommand(player, args);
        }

        return sendHelp(player);
    }

    private boolean handleTeamCommand(Player player, String[] args) {
        if (args.length < 2) {
            return sendHelp(player);
        }

        TrustSystem trustSystem = TrustSystem.getInstance();
        TrustDataManager dataManager = trustSystem.getDataManager();
        String playerUuid = player.getUniqueId().toString();

        String subcommand = args[1].toLowerCase();

        switch (subcommand) {
            case "create":
                return handleTeamCreate(player, args, dataManager);
            case "accept":
                return handleTeamAccept(player, args, dataManager);
            case "leave":
                return handleTeamLeave(player, args, dataManager, false);
            case "leave-silent":
                return handleTeamLeave(player, args, dataManager, true);
            case "info":
                return handleTeamInfo(player, args, dataManager);
            case "invite":
                return handleTeamInvite(player, args, dataManager);
            case "kick":
                return handleTeamKick(player, args, dataManager);
            case "transfer":
                return handleTeamTransfer(player, args, dataManager);
            case "delete":
                return handleTeamDelete(player, args, dataManager);
            case "modify":
                return handleTeamModify(player, args, dataManager);
            default:
                return sendHelp(player);
        }
    }

    private boolean handleAlliesCommand(Player player, String[] args) {
        if (args.length < 2) {
            return sendHelp(player);
        }

        TrustSystem trustSystem = TrustSystem.getInstance();
        TrustDataManager dataManager = trustSystem.getDataManager();
        String playerUuid = player.getUniqueId().toString();

        String subcommand = args[1].toLowerCase();

        // Check if player is a team leader
        List<String> playerTeams = dataManager.getPlayerTeams(playerUuid);
        boolean isLeader = playerTeams.stream().anyMatch(teamId -> dataManager.isTeamLeader(playerUuid, teamId));

        if (!isLeader) {
            player.sendMessage(Component.text("§cYou must be a team leader to manage allies", NamedTextColor.RED));
            return true;
        }

        switch (subcommand) {
            case "add":
                return handleAlliesAdd(player, args, dataManager);
            case "accept":
                return handleAlliesAccept(player, args, dataManager);
            case "remove":
                return handleAlliesRemove(player, args, dataManager);
            case "list":
                return handleAlliesList(player, args, dataManager);
            default:
                return sendHelp(player);
        }
    }

    // ===== Team Command Handlers =====

    private boolean handleTeamCreate(Player player, String[] args, TrustDataManager dataManager) {
        String playerUuid = player.getUniqueId().toString();
        List<String> playerTeams = dataManager.getPlayerTeams(playerUuid);

        if (!playerTeams.isEmpty()) {
            player.sendMessage(
                    Component.text("§cYou are already in a team. Leave your current team first.", NamedTextColor.RED));
            return true;
        }

        if (args.length < 3) {
            player.sendMessage(Component.text("§cUsage: /trust team create <id>", NamedTextColor.RED));
            return true;
        }

        String teamId = args[2];

        // Check if team ID already exists
        if (dataManager.getTeam(teamId) != null) {
            player.sendMessage(Component.text("§cA team with this ID already exists", NamedTextColor.RED));
            return true;
        }

        dataManager.createTeam(teamId, teamId, playerUuid);
        player.sendMessage(Component.text("§aTeam created: " + teamId, NamedTextColor.GREEN));
        return true;
    }

    private boolean handleTeamAccept(Player player, String[] args, TrustDataManager dataManager) {
        if (args.length < 3) {
            player.sendMessage(Component.text("§cUsage: /trust team accept <team_id>", NamedTextColor.RED));
            return true;
        }

        String teamId = args[2];
        String playerUuid = player.getUniqueId().toString();

        if (!dataManager.hasSessionInvite(teamId, playerUuid)) {
            player.sendMessage(Component.text("§cYou don't have an invite to this team", NamedTextColor.RED));
            return true;
        }

        dataManager.addMember(teamId, playerUuid);
        dataManager.removeSessionInvite(teamId, playerUuid);
        player.sendMessage(Component.text("§aYou have joined the team", NamedTextColor.GREEN));
        
        // Notify team members
        for (String memberUuid : dataManager.getTeamMembers(teamId)) {
            Player member = Bukkit.getPlayer(UUID.fromString(memberUuid));
            if (member != null && !member.equals(player)) {
                member.sendMessage(
                        Component.text("§a" + player.getName() + " has joined the team", NamedTextColor.GREEN));
            }
        }

        return true;
    }

    private boolean handleTeamLeave(Player player, String[] args, TrustDataManager dataManager, boolean silent) {
        String playerUuid = player.getUniqueId().toString();
        List<String> playerTeams = dataManager.getPlayerTeams(playerUuid);

        if (playerTeams.isEmpty()) {
            player.sendMessage(Component.text("§cYou are not in a team", NamedTextColor.RED));
            return true;
        }

        // For now, leave the first team (or specify which one)
        String teamId = playerTeams.get(0);

        // Check if player is team leader
        if (dataManager.isTeamLeader(playerUuid, teamId)) {
            // Disband the team
            dataManager.deleteTeam(teamId);
            player.sendMessage(Component.text("§aYou disbanded the team", NamedTextColor.GREEN));

            // Notify other members
            for (String memberUuid : dataManager.getTeamMembers(teamId)) {
                Player member = Bukkit.getPlayer(UUID.fromString(memberUuid));
                if (member != null && !member.equals(player)) {
                    member.sendMessage(Component.text("§cThe team has been disbanded", NamedTextColor.RED));
                }
            }
        } else {
            dataManager.removeMember(teamId, playerUuid);
            player.sendMessage(Component.text("§aYou left the team", NamedTextColor.GREEN));

            if (!silent) {
                // Notify team members
                for (String memberUuid : dataManager.getTeamMembers(teamId)) {
                    Player member = Bukkit.getPlayer(UUID.fromString(memberUuid));
                    if (member != null && !member.equals(player)) {
                        member.sendMessage(
                                Component.text("§c" + player.getName() + " has left the team", NamedTextColor.RED));
                    }
                }
            }
        }

        return true;
    }

    private boolean handleTeamInfo(Player player, String[] args, TrustDataManager dataManager) {
        String playerUuid = player.getUniqueId().toString();
        List<String> playerTeams = dataManager.getPlayerTeams(playerUuid);

        if (playerTeams.isEmpty()) {
            player.sendMessage(Component.text("§cYou are not in a team", NamedTextColor.RED));
            return true;
        }

        for (String teamId : playerTeams) {
            player.sendMessage(Component.text("§e" + teamId + " - Members:", NamedTextColor.YELLOW));
            for (String memberUuid : dataManager.getTeamMembers(teamId)) {
                Player member = Bukkit.getPlayer(UUID.fromString(memberUuid));
                String name = member != null ? member.getName() : memberUuid;
                boolean isLeader = dataManager.isTeamLeader(memberUuid, teamId);
                String prefix = isLeader ? "§6[LEADER] " : "§7";
                player.sendMessage(Component.text(prefix + name, NamedTextColor.GRAY));
            }
        }

        return true;
    }

    private boolean handleTeamInvite(Player player, String[] args, TrustDataManager dataManager) {
        String playerUuid = player.getUniqueId().toString();

        if (args.length < 3) {
            player.sendMessage(Component.text("§cUsage: /trust team invite <player>", NamedTextColor.RED));
            return true;
        }

        String targetName = args[2];
        Player target = Bukkit.getPlayer(targetName);

        if (target == null) {
            player.sendMessage(Component.text("§cPlayer not found", NamedTextColor.RED));
            return true;
        }

        // Find a team where player is leader
        List<String> playerTeams = dataManager.getPlayerTeams(playerUuid);
        String leaderTeam = playerTeams.stream()
                .filter(teamId -> dataManager.isTeamLeader(playerUuid, teamId))
                .findFirst()
                .orElse(null);

        if (leaderTeam == null) {
            player.sendMessage(Component.text("§cYou are not a team leader", NamedTextColor.RED));
            return true;
        }

        String targetUuid = target.getUniqueId().toString();
        dataManager.addSessionInvite(leaderTeam, targetUuid);

        player.sendMessage(Component.text("§aInvite sent to " + target.getName(), NamedTextColor.GREEN));
        target.sendMessage(
                Component.text("§a" + player.getName() + " invited you to join their team!", NamedTextColor.GREEN));

        // Send clickable accept message
        Component acceptMessage = Component.text("§e[ACCEPT]", NamedTextColor.YELLOW)
                .clickEvent(ClickEvent.runCommand("/trust team accept " + leaderTeam));
        Component fullMessage = Component.text("§eClick to join: ").append(acceptMessage);
        target.sendMessage(fullMessage);

        return true;
    }

    private boolean handleTeamKick(Player player, String[] args, TrustDataManager dataManager) {
        String playerUuid = player.getUniqueId().toString();

        if (args.length < 3) {
            player.sendMessage(Component.text("§cUsage: /trust team kick <player>", NamedTextColor.RED));
            return true;
        }

        String targetName = args[2];

        // Find a team where player is leader
        List<String> playerTeams = dataManager.getPlayerTeams(playerUuid);
        String leaderTeam = playerTeams.stream()
                .filter(teamId -> dataManager.isTeamLeader(playerUuid, teamId))
                .findFirst()
                .orElse(null);

        if (leaderTeam == null) {
            player.sendMessage(Component.text("§cYou are not a team leader", NamedTextColor.RED));
            return true;
        }

        // Find target player UUID
        String targetUuid = null;
        for (String memberUuid : dataManager.getTeamMembers(leaderTeam)) {
            Player member = Bukkit.getPlayer(UUID.fromString(memberUuid));
            if (member != null && member.getName().equals(targetName)) {
                targetUuid = memberUuid;
                break;
            }
        }

        if (targetUuid == null) {
            player.sendMessage(Component.text("§cPlayer not found in your team", NamedTextColor.RED));
            return true;
        }

        dataManager.removeMember(leaderTeam, targetUuid);
        player.sendMessage(Component.text("§a" + targetName + " has been kicked", NamedTextColor.GREEN));

        Player target = Bukkit.getPlayer(UUID.fromString(targetUuid));
        if (target != null) {
            target.sendMessage(Component.text("§cYou have been kicked from the team", NamedTextColor.RED));
        }

        return true;
    }

    private boolean handleTeamTransfer(Player player, String[] args, TrustDataManager dataManager) {
        player.sendMessage(Component.text("§cNot yet implemented", NamedTextColor.RED));
        return true;
    }

    private boolean handleTeamDelete(Player player, String[] args, TrustDataManager dataManager) {
        String playerUuid = player.getUniqueId().toString();

        List<String> playerTeams = dataManager.getPlayerTeams(playerUuid);
        String leaderTeam = playerTeams.stream()
                .filter(teamId -> dataManager.isTeamLeader(playerUuid, teamId))
                .findFirst()
                .orElse(null);

        if (leaderTeam == null) {
            player.sendMessage(Component.text("§cYou are not a team leader", NamedTextColor.RED));
            return true;
        }

        dataManager.deleteTeam(leaderTeam);
        player.sendMessage(Component.text("§aTeam deleted", NamedTextColor.GREEN));

        return true;
    }

    private boolean handleTeamModify(Player player, String[] args, TrustDataManager dataManager) {
        String playerUuid = player.getUniqueId().toString();

        if (args.length < 4) {
            player.sendMessage(Component.text("§cUsage: /trust team modify displayname <name>", NamedTextColor.RED));
            return true;
        }

        List<String> playerTeams = dataManager.getPlayerTeams(playerUuid);
        String leaderTeam = playerTeams.stream()
                .filter(teamId -> dataManager.isTeamLeader(playerUuid, teamId))
                .findFirst()
                .orElse(null);

        if (leaderTeam == null) {
            player.sendMessage(Component.text("§cYou are not a team leader", NamedTextColor.RED));
            return true;
        }

        if (args[2].equalsIgnoreCase("displayname")) {
            String newName = args[3];
            dataManager.updateTeamDisplayName(leaderTeam, newName);
            player.sendMessage(Component.text("§aTeam display name updated", NamedTextColor.GREEN));
            return true;
        }

        return sendHelp(player);
    }

    // ===== Allies Command Handlers =====

    private boolean handleAlliesAdd(Player player, String[] args, TrustDataManager dataManager) {
        player.sendMessage(Component.text("§cNot yet implemented", NamedTextColor.RED));
        return true;
    }

    private boolean handleAlliesAccept(Player player, String[] args, TrustDataManager dataManager) {
        player.sendMessage(Component.text("§cNot yet implemented", NamedTextColor.RED));
        return true;
    }

    private boolean handleAlliesRemove(Player player, String[] args, TrustDataManager dataManager) {
        player.sendMessage(Component.text("§cNot yet implemented", NamedTextColor.RED));
        return true;
    }

    private boolean handleAlliesList(Player player, String[] args, TrustDataManager dataManager) {
        player.sendMessage(Component.text("§cNot yet implemented", NamedTextColor.RED));
        return true;
    }

    // ===== Help =====

    private boolean sendHelp(Player player) {
        player.sendMessage("§e/trust team create <id>");
        player.sendMessage("§e/trust team accept <team_id>");
        player.sendMessage("§e/trust team leave");
        player.sendMessage("§e/trust team leave-silent");
        player.sendMessage("§e/trust team info");
        player.sendMessage("§e/trust team invite <player>");
        player.sendMessage("§e/trust team kick <player>");
        player.sendMessage("§e/trust team modify displayname <name>");
        player.sendMessage("§e/trust team delete");
        player.sendMessage("§e/trust allies add <team_owner>");
        player.sendMessage("§e/trust allies accept <team_owner>");
        player.sendMessage("§e/trust allies remove <team_owner>");
        player.sendMessage("§e/trust allies list");
        return true;
    }

    // ===== Tab Completion =====

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            return new ArrayList<>();
        }

        Player player = (Player) sender;
        TrustSystem trustSystem = TrustSystem.getInstance();
        TrustDataManager dataManager = trustSystem.getDataManager();
        String playerUuid = player.getUniqueId().toString();
        List<String> playerTeams = dataManager.getPlayerTeams(playerUuid);

        if (args.length <= 1) {
            List<String> completions = new ArrayList<>();
            completions.add("team");
            completions.add("allies");
            return completions;
        }

        if (args.length == 2) {
            String subcommand = args[0].toLowerCase();
            List<String> completions = new ArrayList<>();

            if (subcommand.equals("team")) {
                if (playerTeams.isEmpty()) {
                    completions.add("create");
                    // Add pending invites
                    completions.addAll(dataManager.getPendingInvitesForPlayer(playerUuid));
                }
                if (!playerTeams.isEmpty()) {
                    completions.add("leave");
                    completions.add("leave-silent");
                    completions.add("info");
                }

                // Leader commands
                for (String teamId : playerTeams) {
                    if (dataManager.isTeamLeader(playerUuid, teamId)) {
                        completions.add("invite");
                        completions.add("kick");
                        completions.add("modify");
                        completions.add("transfer");
                        completions.add("delete");
                        break;
                    }
                }
            } else if (subcommand.equals("allies")) {
                boolean isLeader = playerTeams.stream()
                        .anyMatch(teamId -> dataManager.isTeamLeader(playerUuid, teamId));

                if (isLeader) {
                    completions.add("add");
                    completions.add("accept");
                    completions.add("remove");
                    completions.add("list");
                }
            }

            return completions;
        }

        // Tab complete for team accept
        if (args.length == 3 && args[0].equalsIgnoreCase("team") && args[1].equalsIgnoreCase("accept")) {
            return new ArrayList<>(dataManager.getPendingInvitesForPlayer(playerUuid));
        }

        return new ArrayList<>();
    }
}
