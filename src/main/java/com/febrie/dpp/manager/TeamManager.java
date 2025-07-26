package com.febrie.dpp.manager;

import com.febrie.dpp.SimpleRPGMain;
import com.febrie.dpp.database.YamlDataManager;
import com.febrie.dpp.dto.TeamData;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class TeamManager {

    private final SimpleRPGMain plugin;
    private final YamlDataManager dataManager;
    private final Map<String, TeamData> teams = new ConcurrentHashMap<>();
    private final Map<UUID, String> playerTeams = new ConcurrentHashMap<>();

    public TeamManager(SimpleRPGMain plugin, YamlDataManager dataManager) {
        this.plugin = plugin;
        this.dataManager = dataManager;

        loadTeamsFromDatabase();
    }

    private void loadTeamsFromDatabase() {
        dataManager.getAllTeams().thenAccept(loadedTeams -> {
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
            player.sendMessage(Component.text("§c해당 팀은 이미 가득 찼습니다! (최대 2명)"));
            return false;
        }

        team.members().add(playerId);
        playerTeams.put(playerId, teamName);

        dataManager.saveTeam(team).whenComplete((result, throwable) -> {
            if (throwable != null) {
                plugin.getLogger().warning("Failed to save team " + teamName + ": " + throwable.getMessage());
            }
        });

        player.sendMessage(Component.text(String.format("§a%s 팀에 참가했습니다!", getTeamDisplayName(teamName))));
        return true;
    }

    public void leaveTeam(@NotNull Player player) {
        UUID playerId = player.getUniqueId();
        String teamName = playerTeams.remove(playerId);

        if (teamName != null) {
            TeamData team = teams.get(teamName);
            if (team != null) {
                team.members().remove(playerId);

                if (team.isEmpty()) {
                    teams.remove(teamName);
                    dataManager.clearAllTeams().whenComplete((result, throwable) -> {
                        if (throwable != null) {
                            plugin.getLogger().warning("Failed to clear all teams from database: " + throwable.getMessage());
                        }
                    });
                } else {
                    dataManager.saveTeam(team).whenComplete((result, throwable) -> {
                        if (throwable != null) {
                            plugin.getLogger().warning("Failed to save team " + teamName + ": " + throwable.getMessage());
                        }
                    });
                }
            }

            player.sendMessage(Component.text("§e팀을 떠났습니다."));
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
            dataManager.saveTeam(eliminatedTeam).whenComplete((result, throwable) -> {
                if (throwable != null) {
                    plugin.getLogger().warning("Failed to save eliminated team " + teamName + ": " + throwable.getMessage());
                }
            });
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
            dataManager.saveTeam(winningTeam).whenComplete((result, throwable) -> {
                if (throwable != null) {
                    plugin.getLogger().warning("Failed to save winning team " + teamName + ": " + throwable.getMessage());
                }
            });
        }
    }

    public void resetAllTeams() {
        teams.clear();
        playerTeams.clear();
        dataManager.clearAllTeams().whenComplete((result, throwable) -> {
            if (throwable != null) {
                plugin.getLogger().warning("Failed to clear all teams: " + throwable.getMessage());
            }
        });
    }

    public Map<String, TeamData> getAllTeams() {
        return new ConcurrentHashMap<>(teams);
    }

    private boolean isValidTeam(@NotNull String teamName) {
        return teamName.equals("red") || teamName.equals("blue") ||
                teamName.equals("green") || teamName.equals("yellow") ||
                teamName.equals("purple") || teamName.equals("orange");
    }

    @Contract(pure = true)
    private String getTeamDisplayName(@NotNull String teamName) {
        return switch (teamName) {
            case "red" -> "§c빨간";
            case "blue" -> "§9파란";
            case "green" -> "§a초록";
            case "yellow" -> "§e노란";
            case "purple" -> "§5보라";
            case "orange" -> "§6주황";
            default -> teamName;
        };
    }
}