package com.febrie.dpp.manager;

import com.febrie.dpp.SimpleRPGMain;
import com.febrie.dpp.database.DatabaseManager;
import com.febrie.dpp.dto.PlayerCooldown;
import com.febrie.dpp.dto.PlayerSkillState;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerDataService {
    
    private final SimpleRPGMain plugin;
    private final DatabaseManager databaseManager;
    private final Map<UUID, PlayerSkillState> skillCache = new ConcurrentHashMap<>();
    private final Map<UUID, PlayerCooldown> cooldownCache = new ConcurrentHashMap<>();
    
    public PlayerDataService(SimpleRPGMain plugin, DatabaseManager databaseManager) {
        this.plugin = plugin;
        this.databaseManager = databaseManager;
    }
    
    public CompletableFuture<PlayerSkillState> getPlayerSkills(UUID playerId, String playerName) {
        PlayerSkillState cached = skillCache.get(playerId);
        if (cached != null) {
            return CompletableFuture.completedFuture(cached);
        }
        
        return databaseManager.getPlayerSkills(playerId).thenApply(skills -> {
            if (skills == null) {
                skills = PlayerSkillState.createDefault(playerId, playerName);
            }
            skillCache.put(playerId, skills);
            return skills;
        });
    }
    
    public CompletableFuture<Void> updatePlayerSkills(PlayerSkillState skills) {
        skillCache.put(skills.playerId(), skills);
        return databaseManager.savePlayerSkills(skills);
    }
    
    public CompletableFuture<PlayerCooldown> getPlayerCooldowns(UUID playerId) {
        PlayerCooldown cached = cooldownCache.get(playerId);
        if (cached != null) {
            return CompletableFuture.completedFuture(cached);
        }
        
        return databaseManager.getPlayerCooldowns(playerId).thenApply(cooldowns -> {
            PlayerCooldown cooldownData = new PlayerCooldown(
                playerId, 
                cooldowns, 
                System.currentTimeMillis()
            );
            cooldownCache.put(playerId, cooldownData);
            return cooldownData;
        });
    }
    
    public void setCooldown(UUID playerId, String skillId, long durationMs) {
        long expireTime = System.currentTimeMillis() + durationMs;
        
        PlayerCooldown current = cooldownCache.get(playerId);
        if (current == null) {
            current = PlayerCooldown.createDefault(playerId);
        }
        
        current.skillCooldowns().put(skillId, expireTime);
        cooldownCache.put(playerId, current);
        
        databaseManager.savePlayerCooldown(playerId, skillId, expireTime);
    }
    
    public boolean isOnCooldown(UUID playerId, String skillId) {
        PlayerCooldown cooldown = cooldownCache.get(playerId);
        return cooldown != null && cooldown.isOnCooldown(skillId);
    }
    
    public long getRemainingCooldown(UUID playerId, String skillId) {
        PlayerCooldown cooldown = cooldownCache.get(playerId);
        return cooldown != null ? cooldown.getRemainingCooldown(skillId) : 0;
    }
    
    public void removePlayer(UUID playerId) {
        skillCache.remove(playerId);
        cooldownCache.remove(playerId);
    }
    
    public void clearExpiredCooldowns() {
        databaseManager.clearExpiredCooldowns();
        
        long currentTime = System.currentTimeMillis();
        cooldownCache.values().forEach(cooldown -> 
            cooldown.skillCooldowns().entrySet().removeIf(entry -> 
                entry.getValue() <= currentTime));
    }
}