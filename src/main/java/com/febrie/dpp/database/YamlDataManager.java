package com.febrie.dpp.database;

import com.febrie.dpp.SimpleRPGMain;
import com.febrie.dpp.dto.PlayerSkillState;
import com.febrie.dpp.dto.TeamData;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public class YamlDataManager {
    
    private final SimpleRPGMain plugin;
    private final File dataFolder;
    private final File playersFolder;
    private final File teamsFile;
    private final File cooldownsFile;
    
    public YamlDataManager(SimpleRPGMain plugin) {
        this.plugin = plugin;
        this.dataFolder = new File(plugin.getDataFolder(), "data");
        this.playersFolder = new File(dataFolder, "players");
        this.teamsFile = new File(dataFolder, "teams.yml");
        this.cooldownsFile = new File(dataFolder, "cooldowns.yml");
    }
    
    public CompletableFuture<Boolean> initialize() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (!dataFolder.exists()) {
                    dataFolder.mkdirs();
                }
                if (!playersFolder.exists()) {
                    playersFolder.mkdirs();
                }
                if (!teamsFile.exists()) {
                    teamsFile.createNewFile();
                }
                if (!cooldownsFile.exists()) {
                    cooldownsFile.createNewFile();
                }
                
                plugin.getLogger().info("YAML data storage initialized successfully!");
                return true;
                
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to initialize YAML storage", e);
                return false;
            }
        });
    }
    
    public CompletableFuture<PlayerSkillState> getPlayerSkills(UUID playerId) {
        return CompletableFuture.supplyAsync(() -> {
            File playerFile = new File(playersFolder, playerId.toString() + ".yml");
            if (!playerFile.exists()) {
                return null;
            }
            
            YamlConfiguration config = YamlConfiguration.loadConfiguration(playerFile);
            
            String playerIdStr = config.getString("player-id");
            if (playerIdStr == null) {
                plugin.getLogger().warning("Player ID is null in file for: " + playerId);
                return null;
            }
            
            return new PlayerSkillState(
                UUID.fromString(playerIdStr),
                config.getString("player-name", "Unknown"),
                config.getBoolean("skills.burning-step", false),
                config.getBoolean("skills.pants-run", false),
                config.getBoolean("skills.crystal-protection", false),
                config.getBoolean("skills.flying-carpet", false),
                config.getBoolean("skills.rage-strike", false),
                config.getBoolean("skills.bomb-dog", false),
                config.getInt("virtual-level", 0),
                config.getLong("last-updated", System.currentTimeMillis())
            );
        });
    }
    
    public CompletableFuture<Void> savePlayerSkills(PlayerSkillState skillState) {
        return CompletableFuture.runAsync(() -> {
            File playerFile = new File(playersFolder, skillState.playerId().toString() + ".yml");
            YamlConfiguration config = new YamlConfiguration();
            
            config.set("player-id", skillState.playerId().toString());
            config.set("player-name", skillState.playerName());
            config.set("skills.burning-step", skillState.burningStep());
            config.set("skills.pants-run", skillState.pantsRun());
            config.set("skills.crystal-protection", skillState.crystalProtection());
            config.set("skills.flying-carpet", skillState.flyingCarpet());
            config.set("skills.rage-strike", skillState.rageStrike());
            config.set("skills.bomb-dog", skillState.bombDog());
            config.set("virtual-level", skillState.virtualLevel());
            config.set("last-updated", skillState.lastUpdated());
            
            try {
                config.save(playerFile);
                plugin.getLogger().fine("Player skills saved to YAML");
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to save player skills", e);
            }
        });
    }
    
    public CompletableFuture<Map<String, Long>> getPlayerCooldowns(UUID playerId) {
        return CompletableFuture.supplyAsync(() -> {
            Map<String, Long> cooldowns = new HashMap<>();
            YamlConfiguration config = YamlConfiguration.loadConfiguration(cooldownsFile);
            
            String playerPath = "cooldowns." + playerId.toString();
            if (config.contains(playerPath)) {
                for (String skillId : config.getConfigurationSection(playerPath).getKeys(false)) {
                    long expireTime = config.getLong(playerPath + "." + skillId);
                    cooldowns.put(skillId, expireTime);
                }
            }
            
            return cooldowns;
        });
    }
    
    public CompletableFuture<Void> savePlayerCooldown(UUID playerId, String skillId, long expireTime) {
        return CompletableFuture.runAsync(() -> {
            YamlConfiguration config = YamlConfiguration.loadConfiguration(cooldownsFile);
            
            String path = "cooldowns." + playerId.toString() + "." + skillId;
            config.set(path, expireTime);
            
            try {
                config.save(cooldownsFile);
                plugin.getLogger().fine("Player cooldown saved to YAML");
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to save player cooldown", e);
            }
        });
    }
    
    public CompletableFuture<Void> clearExpiredCooldowns() {
        return CompletableFuture.runAsync(() -> {
            YamlConfiguration config = YamlConfiguration.loadConfiguration(cooldownsFile);
            long currentTime = System.currentTimeMillis();
            int clearedCount = 0;
            
            if (config.contains("cooldowns")) {
                for (String playerId : config.getConfigurationSection("cooldowns").getKeys(false)) {
                    String playerPath = "cooldowns." + playerId;
                    Set<String> skillsToRemove = new HashSet<>();
                    
                    for (String skillId : config.getConfigurationSection(playerPath).getKeys(false)) {
                        long expireTime = config.getLong(playerPath + "." + skillId);
                        if (expireTime < currentTime) {
                            skillsToRemove.add(skillId);
                            clearedCount++;
                        }
                    }
                    
                    for (String skillId : skillsToRemove) {
                        config.set(playerPath + "." + skillId, null);
                    }
                    
                    if (config.getConfigurationSection(playerPath).getKeys(false).isEmpty()) {
                        config.set(playerPath, null);
                    }
                }
            }
            
            try {
                config.save(cooldownsFile);
                plugin.getLogger().fine("Cleared " + clearedCount + " expired cooldowns");
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to clear expired cooldowns", e);
            }
        });
    }
    
    public CompletableFuture<Map<String, TeamData>> getAllTeams() {
        return CompletableFuture.supplyAsync(() -> {
            Map<String, TeamData> teams = new HashMap<>();
            YamlConfiguration config = YamlConfiguration.loadConfiguration(teamsFile);
            
            if (config.contains("teams")) {
                for (String teamName : config.getConfigurationSection("teams").getKeys(false)) {
                    String teamPath = "teams." + teamName;
                    
                    Set<UUID> members = ConcurrentHashMap.newKeySet();
                    List<String> memberList = config.getStringList(teamPath + ".members");
                    for (String memberIdStr : memberList) {
                        try {
                            members.add(UUID.fromString(memberIdStr));
                        } catch (IllegalArgumentException e) {
                            plugin.getLogger().warning("Invalid UUID in team " + teamName + ": " + memberIdStr);
                        }
                    }
                    
                    TeamData team = new TeamData(
                        teamName,
                        members,
                        config.getBoolean(teamPath + ".eliminated", false),
                        config.getBoolean(teamPath + ".won", false),
                        config.getLong(teamPath + ".created-time", System.currentTimeMillis())
                    );
                    teams.put(teamName, team);
                }
            }
            
            return teams;
        });
    }
    
    public CompletableFuture<Void> saveTeam(TeamData team) {
        return CompletableFuture.runAsync(() -> {
            YamlConfiguration config = YamlConfiguration.loadConfiguration(teamsFile);
            
            String teamPath = "teams." + team.teamName();
            List<String> memberIds = new ArrayList<>();
            for (UUID memberId : team.members()) {
                memberIds.add(memberId.toString());
            }
            
            config.set(teamPath + ".members", memberIds);
            config.set(teamPath + ".eliminated", team.eliminated());
            config.set(teamPath + ".won", team.won());
            config.set(teamPath + ".created-time", team.createdTime());
            
            try {
                config.save(teamsFile);
                plugin.getLogger().fine("Team saved to YAML");
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to save team", e);
            }
        });
    }
    
    public CompletableFuture<Void> clearAllTeams() {
        return CompletableFuture.runAsync(() -> {
            YamlConfiguration config = new YamlConfiguration();
            
            try {
                config.save(teamsFile);
                plugin.getLogger().info("Cleared all teams from YAML");
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to clear teams", e);
            }
        });
    }
    
    public void close() {
        plugin.getLogger().info("YAML data manager closed successfully");
    }
}