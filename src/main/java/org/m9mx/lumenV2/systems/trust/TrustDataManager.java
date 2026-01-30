package org.m9mx.lumenV2.systems.trust;

import org.bukkit.plugin.Plugin;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Manages persistent data storage for teams and allies
 */
public class TrustDataManager {

    private Plugin plugin;
    private File dataFolder;
    private File teamsFile;
    private File teamMembersFile;
    private File alliesFile;

    private Map<String, TeamData> teams;
    private Map<String, List<String>> teamMembers;
    private Map<String, Map<String, String>> allies;
    private Map<String, Set<String>> sessionInvites; // Session-only: team_id -> pending players

    public TrustDataManager(Plugin plugin) {
        this.plugin = plugin;
        this.dataFolder = new File(plugin.getDataFolder(), "trust_data");
        this.teamsFile = new File(dataFolder, "teams.yml");
        this.teamMembersFile = new File(dataFolder, "team_members.yml");
        this.alliesFile = new File(dataFolder, "allies.yml");

        this.teams = new HashMap<>();
        this.teamMembers = new HashMap<>();
        this.allies = new HashMap<>();
        this.sessionInvites = new HashMap<>();

        initializeDataFolder();
        loadData();
    }

    private void initializeDataFolder() {
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
    }

    private void loadData() {
        loadTeams();
        loadTeamMembers();
        loadAllies();
        plugin.getLogger().info("Trust system data loaded");
    }

    private void loadTeams() {
        if (!teamsFile.exists()) {
            return;
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(teamsFile);
        for (String teamId : config.getKeys(false)) {
            String displayName = config.getString(teamId + ".displayName");
            String creatorUuid = config.getString(teamId + ".creatorUuid");
            long createdAt = config.getLong(teamId + ".createdAt", System.currentTimeMillis());

            teams.put(teamId, new TeamData(teamId, displayName, creatorUuid, createdAt));
        }
    }

    private void loadTeamMembers() {
        if (!teamMembersFile.exists()) {
            return;
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(teamMembersFile);
        for (String teamId : config.getKeys(false)) {
            List<String> members = config.getStringList(teamId);
            teamMembers.put(teamId, new ArrayList<>(members));
        }
    }

    private void loadAllies() {
        if (!alliesFile.exists()) {
            return;
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(alliesFile);
        for (String teamId : config.getKeys(false)) {
            Map<String, Object> allyMap = config.getConfigurationSection(teamId).getValues(false);
            Map<String, String> allyStatuses = new HashMap<>();

            for (String allyTeamId : allyMap.keySet()) {
                allyStatuses.put(allyTeamId, (String) allyMap.get(allyTeamId));
            }

            allies.put(teamId, allyStatuses);
        }
    }

    // ===== Team Operations =====

    public void createTeam(String teamId, String displayName, String creatorUuid) {
        teams.put(teamId, new TeamData(teamId, displayName, creatorUuid, System.currentTimeMillis()));
        teamMembers.put(teamId, new ArrayList<>());
        teamMembers.get(teamId).add(creatorUuid);
        allies.put(teamId, new HashMap<>());
        saveTeams();
    }

    public void deleteTeam(String teamId) {
        teams.remove(teamId);
        teamMembers.remove(teamId);
        allies.remove(teamId);

        // Remove this team from all allies
        for (Map<String, String> allyMap : allies.values()) {
            allyMap.remove(teamId);
        }

        saveTeams();
        saveTeamMembers();
        saveAllies();
    }

    public TeamData getTeam(String teamId) {
        return teams.get(teamId);
    }

    public String getTeamIdByDisplayName(String displayName) {
        for (TeamData team : teams.values()) {
            if (team.getDisplayName().equals(displayName)) {
                return team.getId();
            }
        }
        return null;
    }

    public void updateTeamDisplayName(String teamId, String newDisplayName) {
        TeamData team = teams.get(teamId);
        if (team != null) {
            team.setDisplayName(newDisplayName);
            saveTeams();
        }
    }

    // ===== Team Members =====

    public void addMember(String teamId, String playerUuid) {
        List<String> members = teamMembers.computeIfAbsent(teamId, k -> new ArrayList<>());
        if (!members.contains(playerUuid)) {
            members.add(playerUuid);
            saveTeamMembers();
        }
    }

    public void removeMember(String teamId, String playerUuid) {
        List<String> members = teamMembers.get(teamId);
        if (members != null) {
            members.remove(playerUuid);
            saveTeamMembers();
        }
    }

    public List<String> getTeamMembers(String teamId) {
        return new ArrayList<>(teamMembers.getOrDefault(teamId, new ArrayList<>()));
    }

    public List<String> getPlayerTeams(String playerUuid) {
        List<String> playerTeams = new ArrayList<>();
        for (String teamId : teamMembers.keySet()) {
            if (teamMembers.get(teamId).contains(playerUuid)) {
                playerTeams.add(teamId);
            }
        }
        return playerTeams;
    }

    public boolean isPlayerInTeam(String playerUuid, String teamId) {
        List<String> members = teamMembers.get(teamId);
        return members != null && members.contains(playerUuid);
    }

    public boolean isTeamLeader(String playerUuid, String teamId) {
        TeamData team = teams.get(teamId);
        return team != null && team.getCreatorUuid().equals(playerUuid);
    }

    // ===== Allies =====

    public void requestAlly(String fromTeamId, String toTeamId) {
        Map<String, String> allyMap = allies.computeIfAbsent(fromTeamId, k -> new HashMap<>());
        allyMap.put(toTeamId, "pending");
        saveAllies();
    }

    public void acceptAlly(String teamId, String allyTeamId) {
        Map<String, String> allyMap = allies.get(teamId);
        if (allyMap != null && "pending".equals(allyMap.get(allyTeamId))) {
            allyMap.put(allyTeamId, "accepted");

            // Also mark from the other side if exists
            Map<String, String> reverseAllyMap = allies.computeIfAbsent(allyTeamId, k -> new HashMap<>());
            reverseAllyMap.put(teamId, "accepted");

            saveAllies();
        }
    }

    public void removeAlly(String teamId, String allyTeamId) {
        Map<String, String> allyMap = allies.get(teamId);
        if (allyMap != null) {
            allyMap.remove(allyTeamId);
        }

        Map<String, String> reverseAllyMap = allies.get(allyTeamId);
        if (reverseAllyMap != null) {
            reverseAllyMap.remove(teamId);
        }

        saveAllies();
    }

    public Map<String, String> getTeamAllies(String teamId) {
        return new HashMap<>(allies.getOrDefault(teamId, new HashMap<>()));
    }

    public boolean isAllyTeam(String teamId, String otherTeamId) {
        Map<String, String> allyMap = allies.get(teamId);
        return allyMap != null && "accepted".equals(allyMap.get(otherTeamId));
    }

    // ===== Session Invites (ephemeral) =====

    public void addSessionInvite(String teamId, String playerUuid) {
        sessionInvites.computeIfAbsent(teamId, k -> new HashSet<>()).add(playerUuid);
    }

    public boolean hasSessionInvite(String teamId, String playerUuid) {
        Set<String> invites = sessionInvites.get(teamId);
        return invites != null && invites.contains(playerUuid);
    }

    public void removeSessionInvite(String teamId, String playerUuid) {
        Set<String> invites = sessionInvites.get(teamId);
        if (invites != null) {
            invites.remove(playerUuid);
        }
    }

    public void clearSessionInvites() {
        sessionInvites.clear();
    }

    public List<String> getPendingInvitesForPlayer(String playerUuid) {
        List<String> invitedTeams = new ArrayList<>();
        for (Map.Entry<String, Set<String>> entry : sessionInvites.entrySet()) {
            if (entry.getValue().contains(playerUuid)) {
                invitedTeams.add(entry.getKey());
            }
        }
        return invitedTeams;
    }

    // ===== Persistence =====

    private void saveTeams() {
        YamlConfiguration config = new YamlConfiguration();

        for (TeamData team : teams.values()) {
            String path = team.getId();
            config.set(path + ".id", team.getId());
            config.set(path + ".displayName", team.getDisplayName());
            config.set(path + ".creatorUuid", team.getCreatorUuid());
            config.set(path + ".createdAt", team.getCreatedAt());
        }

        try {
            config.save(teamsFile);
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to save teams.yml: " + e.getMessage());
        }
    }

    private void saveTeamMembers() {
        YamlConfiguration config = new YamlConfiguration();

        for (String teamId : teamMembers.keySet()) {
            config.set(teamId, teamMembers.get(teamId));
        }

        try {
            config.save(teamMembersFile);
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to save team_members.yml: " + e.getMessage());
        }
    }

    private void saveAllies() {
        YamlConfiguration config = new YamlConfiguration();

        for (String teamId : allies.keySet()) {
            for (String allyTeamId : allies.get(teamId).keySet()) {
                config.set(teamId + "." + allyTeamId, allies.get(teamId).get(allyTeamId));
            }
        }

        try {
            config.save(alliesFile);
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to save allies.yml: " + e.getMessage());
        }
    }
}
