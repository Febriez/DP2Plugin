package com.febrie.dpp.manager;

import com.febrie.dpp.SimpleRPGMain;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ScoreboardManager {
    
    private final SimpleRPGMain plugin;
    private final Scoreboard mainScoreboard;
    private final Map<String, Objective> objectives = new HashMap<>();
    
    public ScoreboardManager(SimpleRPGMain plugin) {
        this.plugin = plugin;
        
        ScoreboardManager manager = Bukkit.getScoreboardManager();
        this.mainScoreboard = manager.getMainScoreboard();
        
        initializeObjectives();
    }
    
    private void initializeObjectives() {
        createObjective("burning_step", "버닝 스텝 단계");
        createObjective("pantsrun", "빤스런 사용 횟수");
        createObjective("virtual_level", "가상 레벨");
        createObjective("team_score", "팀 점수");
        createObjective("core_health", "코어 체력");
    }
    
    private void createObjective(String name, String displayName) {
        Objective existing = mainScoreboard.getObjective(name);
        if (existing != null) {
            existing.unregister();
        }
        
        Objective objective = mainScoreboard.registerNewObjective(name, "dummy", displayName);
        objectives.put(name, objective);
    }
    
    public void setScore(Player player, String objectiveName, int score) {
        Objective objective = objectives.get(objectiveName);
        if (objective != null) {
            objective.getScore(player.getName()).setScore(score);
        }
    }
    
    public void setScore(String entry, String objectiveName, int score) {
        Objective objective = objectives.get(objectiveName);
        if (objective != null) {
            objective.getScore(entry).setScore(score);
        }
    }
    
    public int getScore(Player player, String objectiveName) {
        Objective objective = objectives.get(objectiveName);
        if (objective != null) {
            return objective.getScore(player.getName()).getScore();
        }
        return 0;
    }
    
    public int getScore(String entry, String objectiveName) {
        Objective objective = objectives.get(objectiveName);
        if (objective != null) {
            return objective.getScore(entry).getScore();
        }
        return 0;
    }
    
    public void addScore(Player player, String objectiveName, int amount) {
        int current = getScore(player, objectiveName);
        setScore(player, objectiveName, current + amount);
    }
    
    public void resetPlayerScores(Player player) {
        for (Objective objective : objectives.values()) {
            objective.getScore(player.getName()).resetScore();
        }
    }
    
    public void resetAllScores() {
        for (Objective objective : objectives.values()) {
            for (String entry : objective.getScoreboard().getEntries()) {
                objective.getScore(entry).resetScore();
            }
        }
    }
    
    public void resetObjective(String objectiveName) {
        Objective objective = objectives.get(objectiveName);
        if (objective != null) {
            for (String entry : objective.getScoreboard().getEntries()) {
                objective.getScore(entry).resetScore();
            }
        }
    }
}