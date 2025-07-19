package com.febrie.dpp.database;

import com.febrie.dpp.SimpleRPGMain;
import com.febrie.dpp.dto.*;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.bukkit.configuration.ConfigurationSection;

import java.sql.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

public class DatabaseManager {
    
    private final SimpleRPGMain plugin;
    private final Gson gson = new Gson();
    private Connection connection;
    
    public DatabaseManager(SimpleRPGMain plugin) {
        this.plugin = plugin;
    }
    
    public CompletableFuture<Boolean> initialize() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                ConfigurationSection dbConfig = plugin.getConfig().getConfigurationSection("database");
                String host = dbConfig.getString("host");
                int port = dbConfig.getInt("port");
                String database = dbConfig.getString("database");
                String username = dbConfig.getString("username");
                String password = dbConfig.getString("password");
                
                String url = String.format("jdbc:mysql://%s:%d/%s?useSSL=false&allowPublicKeyRetrieval=true", 
                    host, port, database);
                
                connection = DriverManager.getConnection(url, username, password);
                createTables();
                
                plugin.getLogger().info("Database connection established successfully!");
                return true;
                
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to connect to database", e);
                return false;
            }
        });
    }
    
    private void createTables() throws SQLException {
        String[] tables = {
            """
            CREATE TABLE IF NOT EXISTS player_skills (
                player_id VARCHAR(36) PRIMARY KEY,
                player_name VARCHAR(16),
                burning_step BOOLEAN DEFAULT FALSE,
                pants_run BOOLEAN DEFAULT FALSE, 
                crystal_protection BOOLEAN DEFAULT FALSE,
                flying_carpet BOOLEAN DEFAULT FALSE,
                rage_strike BOOLEAN DEFAULT FALSE,
                bomb_dog BOOLEAN DEFAULT FALSE,
                virtual_level INT DEFAULT 0,
                last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
            """,
            """
            CREATE TABLE IF NOT EXISTS player_cooldowns (
                player_id VARCHAR(36),
                skill_id VARCHAR(50),
                expire_time BIGINT,
                PRIMARY KEY (player_id, skill_id)
            )
            """,
            """
            CREATE TABLE IF NOT EXISTS teams (
                team_name VARCHAR(20) PRIMARY KEY,
                members JSON,
                eliminated BOOLEAN DEFAULT FALSE,
                won BOOLEAN DEFAULT FALSE,
                created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
            """,
            """
            CREATE TABLE IF NOT EXISTS core_game (
                id INT PRIMARY KEY AUTO_INCREMENT,
                active BOOLEAN DEFAULT FALSE,
                start_time BIGINT,
                end_time BIGINT,
                game_data JSON,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
            """
        };
        
        for (String sql : tables) {
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.executeUpdate();
            }
        }
    }
    
    public CompletableFuture<PlayerSkillState> getPlayerSkills(UUID playerId) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT * FROM player_skills WHERE player_id = ?";
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setString(1, playerId.toString());
                
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        return new PlayerSkillState(
                            UUID.fromString(rs.getString("player_id")),
                            rs.getString("player_name"),
                            rs.getBoolean("burning_step"),
                            rs.getBoolean("pants_run"),
                            rs.getBoolean("crystal_protection"),
                            rs.getBoolean("flying_carpet"),
                            rs.getBoolean("rage_strike"),
                            rs.getBoolean("bomb_dog"),
                            rs.getInt("virtual_level"),
                            rs.getLong("last_updated")
                        );
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to get player skills", e);
            }
            return null;
        });
    }
    
    public CompletableFuture<Void> savePlayerSkills(PlayerSkillState skillState) {
        return CompletableFuture.runAsync(() -> {
            String sql = """
                INSERT INTO player_skills (player_id, player_name, burning_step, pants_run, 
                crystal_protection, flying_carpet, rage_strike, bomb_dog, virtual_level, last_updated) 
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?) 
                ON DUPLICATE KEY UPDATE 
                player_name = VALUES(player_name),
                burning_step = VALUES(burning_step),
                pants_run = VALUES(pants_run),
                crystal_protection = VALUES(crystal_protection),
                flying_carpet = VALUES(flying_carpet),
                rage_strike = VALUES(rage_strike),
                bomb_dog = VALUES(bomb_dog),
                virtual_level = VALUES(virtual_level),
                last_updated = VALUES(last_updated)
                """;
                
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setString(1, skillState.playerId().toString());
                stmt.setString(2, skillState.playerName());
                stmt.setBoolean(3, skillState.burningStep());
                stmt.setBoolean(4, skillState.pantsRun());
                stmt.setBoolean(5, skillState.crystalProtection());
                stmt.setBoolean(6, skillState.flyingCarpet());
                stmt.setBoolean(7, skillState.rageStrike());
                stmt.setBoolean(8, skillState.bombDog());
                stmt.setInt(9, skillState.virtualLevel());
                stmt.setLong(10, skillState.lastUpdated());
                
                stmt.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to save player skills", e);
            }
        });
    }
    
    public CompletableFuture<Map<String, Long>> getPlayerCooldowns(UUID playerId) {
        return CompletableFuture.supplyAsync(() -> {
            Map<String, Long> cooldowns = new HashMap<>();
            String sql = "SELECT skill_id, expire_time FROM player_cooldowns WHERE player_id = ?";
            
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setString(1, playerId.toString());
                
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        cooldowns.put(rs.getString("skill_id"), rs.getLong("expire_time"));
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to get player cooldowns", e);
            }
            return cooldowns;
        });
    }
    
    public CompletableFuture<Void> savePlayerCooldown(UUID playerId, String skillId, long expireTime) {
        return CompletableFuture.runAsync(() -> {
            String sql = """
                INSERT INTO player_cooldowns (player_id, skill_id, expire_time) 
                VALUES (?, ?, ?) 
                ON DUPLICATE KEY UPDATE expire_time = VALUES(expire_time)
                """;
                
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setString(1, playerId.toString());
                stmt.setString(2, skillId);
                stmt.setLong(3, expireTime);
                stmt.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to save player cooldown", e);
            }
        });
    }
    
    public CompletableFuture<Void> clearExpiredCooldowns() {
        return CompletableFuture.runAsync(() -> {
            String sql = "DELETE FROM player_cooldowns WHERE expire_time < ?";
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setLong(1, System.currentTimeMillis());
                stmt.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to clear expired cooldowns", e);
            }
        });
    }
    
    public CompletableFuture<Map<String, TeamData>> getAllTeams() {
        return CompletableFuture.supplyAsync(() -> {
            Map<String, TeamData> teams = new HashMap<>();
            String sql = "SELECT * FROM teams";
            
            try (PreparedStatement stmt = connection.prepareStatement(sql);
                 ResultSet rs = stmt.executeQuery()) {
                
                while (rs.next()) {
                    String membersJson = rs.getString("members");
                    Set<UUID> members = gson.fromJson(membersJson, new TypeToken<Set<UUID>>(){}.getType());
                    
                    TeamData team = new TeamData(
                        rs.getString("team_name"),
                        members,
                        rs.getBoolean("eliminated"),
                        rs.getBoolean("won"),
                        rs.getLong("created_time")
                    );
                    teams.put(team.teamName(), team);
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to get teams", e);
            }
            return teams;
        });
    }
    
    public CompletableFuture<Void> saveTeam(TeamData team) {
        return CompletableFuture.runAsync(() -> {
            String sql = """
                INSERT INTO teams (team_name, members, eliminated, won, created_time) 
                VALUES (?, ?, ?, ?, ?) 
                ON DUPLICATE KEY UPDATE 
                members = VALUES(members),
                eliminated = VALUES(eliminated),
                won = VALUES(won)
                """;
                
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setString(1, team.teamName());
                stmt.setString(2, gson.toJson(team.members()));
                stmt.setBoolean(3, team.eliminated());
                stmt.setBoolean(4, team.won());
                stmt.setLong(5, team.createdTime());
                stmt.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to save team", e);
            }
        });
    }
    
    public CompletableFuture<Void> clearAllTeams() {
        return CompletableFuture.runAsync(() -> {
            String sql = "DELETE FROM teams";
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to clear teams", e);
            }
        });
    }
    
    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to close database connection", e);
        }
    }
}