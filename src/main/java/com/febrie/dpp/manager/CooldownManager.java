package com.febrie.dpp.manager;

import com.febrie.dpp.SimpleRPGMain;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class CooldownManager {
    
    private final SimpleRPGMain plugin;
    private final PlayerDataService playerDataService;
    private final Map<String, Long> skillCooldowns = new ConcurrentHashMap<>();
    
    public CooldownManager(SimpleRPGMain plugin, PlayerDataService playerDataService) {
        this.plugin = plugin;
        this.playerDataService = playerDataService;
        
        initializeCooldownDurations();
        startCleanupTask();
    }
    
    private void initializeCooldownDurations() {
        skillCooldowns.put("burning_step", 8000L);
        skillCooldowns.put("pants_run", 30000L);
        skillCooldowns.put("crystal_protection", 60000L);
        skillCooldowns.put("flying_carpet", 0L);
        skillCooldowns.put("bomb_dog", 60000L);
        skillCooldowns.put("rage_strike", 0L);
    }
    
    public boolean checkCooldown(Player player, String skillId) {
        return !playerDataService.isOnCooldown(player.getUniqueId(), skillId);
    }
    
    public void setCooldown(Player player, String skillId) {
        Long duration = skillCooldowns.get(skillId);
        if (duration != null && duration > 0) {
            playerDataService.setCooldown(player.getUniqueId(), skillId, duration);
        }
    }
    
    public long getRemainingCooldown(Player player, String skillId) {
        return playerDataService.getRemainingCooldown(player.getUniqueId(), skillId);
    }
    
    public String formatCooldownMessage(Player player, String skillId) {
        long remaining = getRemainingCooldown(player, skillId);
        if (remaining <= 0) return null;
        
        double seconds = remaining / 1000.0;
        return String.format("§c스킬이 쿨타임 중입니다! (%.1f초 남음)", seconds);
    }
    
    private void startCleanupTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                playerDataService.clearExpiredCooldowns();
            }
        }.runTaskTimerAsynchronously(plugin, 20 * 60, 20 * 60);
    }
}