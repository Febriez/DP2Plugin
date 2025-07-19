package com.febrie.dpp.manager;

import com.febrie.dpp.SimpleRPGMain;
import com.febrie.dpp.database.DatabaseManager;
import com.febrie.dpp.dto.TeamData;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class TeamManager {
    
    private final SimpleRPGMain plugin;
    private final DatabaseManager databaseManager;
    private final Map<String, TeamData> teams = new ConcurrentHashMap<>();
    private final Map<UUID, String> playerTeams = new ConcurrentHashMap<>();
    
    public TeamManager(SimpleRPGMain plugin, DatabaseManager databaseManager) {
        this.plugin = plugin;
        this.databaseManager = databaseManager;
        
        loadTeamsFromDatabase();
    }
    
    private void loadTeamsFromDatabase() {
        databaseManager.getAllTeams().thenAccept(loadedTeams -> {
            teams.putAll(loadedTeams);
            
            for (TeamData team : loadedTeams.values()) {
                for (UUID playerId : team.members()) {
                    playerTeams.put(playerId, team.teamName());
                }
            }
        });
    }
    
    public boolean joinTeam(Player player, String teamName) {
        if (!isValidTeam(teamName)) {
            return false;
        }
        
        UUID playerId = player.getUniqueId();
        String currentTeam = playerTeams.get(playerId);
        
        if (currentTeam != null) {
            leaveTeam(player);
        }
        
        TeamData team = teams.computeIfAbsent(teamName, TeamData::createDefault);
        
        if (team.isFull()) {
            player.sendMessage("§c해당 팀은 이미 가득 찼습니다! (최대 2명)");
            return false;
        }
        
        team.members().add(playerId);
        playerTeams.put(playerId, teamName);
        
        databaseManager.saveTeam(team);
        
        player.sendMessage(String.format("§a%s 팀에 참가했습니다!", getTeamDisplayName(teamName)));
        return true;
    }
    
    public void leaveTeam(Player player) {
        UUID playerId = player.getUniqueId();
        String teamName = playerTeams.remove(playerId);
        
        if (teamName != null) {
            TeamData team = teams.get(teamName);
            if (team != null) {
                team.members().remove(playerId);
                
                if (team.isEmpty()) {
                    teams.remove(teamName);
                    databaseManager.clearAllTeams();
                } else {
                    databaseManager.saveTeam(team);
                }
            }
            
            player.sendMessage("§e팀을 떠났습니다.");
        }
    }
    
    public String getPlayerTeam(UUID playerId) {
        return playerTeams.get(playerId);
    }
    
    public TeamData getTeam(String teamName) {
        return teams.get(teamName);
    }
    
    public boolean areTeammates(UUID player1, UUID player2) {
        String team1 = playerTeams.get(player1);
        String team2 = playerTeams.get(player2);
        
        return team1 != null && team1.equals(team2);
    }
    
    public void eliminateTeam(String teamName) {
        TeamData team = teams.get(teamName);
        if (team != null) {
            TeamData eliminatedTeam = new TeamData(
                team.teamName(),
                team.members(),
                true,
                false,
                team.createdTime()
            );
            teams.put(teamName, eliminatedTeam);
            databaseManager.saveTeam(eliminatedTeam);
        }
    }
    
    public void setTeamWin(String teamName) {
        TeamData team = teams.get(teamName);
        if (team != null) {
            TeamData winningTeam = new TeamData(
                team.teamName(),
                team.members(),
                false,
                true,
                team.createdTime()
            );
            teams.put(teamName, winningTeam);
            databaseManager.saveTeam(winningTeam);
        }
    }
    
    public void resetAllTeams() {
        teams.clear();
        playerTeams.clear();
        databaseManager.clearAllTeams();
    }
    
    public Map<String, TeamData> getAllTeams() {
        return new ConcurrentHashMap<>(teams);
    }
    
    private boolean isValidTeam(String teamName) {
        return teamName.equals("red") || teamName.equals("blue") || 
               teamName.equals("green") || teamName.equals("yellow");
    }
    
    private String getTeamDisplayName(String teamName) {
        return switch (teamName) {
            case "red" -> "§c빨간";
            case "blue" -> "§9파란";
            case "green" -> "§a초록";
            case "yellow" -> "§e노란";
            default -> teamName;
        };
    }
}