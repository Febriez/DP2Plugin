package com.febrie.dpp.dto;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public record PlayerCooldown(
    UUID playerId,
    Map<String, Long> skillCooldowns,
    long lastUpdated  
) {
    public static PlayerCooldown createDefault(UUID playerId) {
        return new PlayerCooldown(
            playerId,
            new ConcurrentHashMap<>(),
            System.currentTimeMillis()
        );
    }
    
    public boolean isOnCooldown(String skillId) {
        Long expireTime = skillCooldowns.get(skillId);
        return expireTime != null && expireTime > System.currentTimeMillis();
    }
    
    public long getRemainingCooldown(String skillId) {
        Long expireTime = skillCooldowns.get(skillId);
        if (expireTime == null) return 0;
        long remaining = expireTime - System.currentTimeMillis();
        return Math.max(0, remaining);
    }
}