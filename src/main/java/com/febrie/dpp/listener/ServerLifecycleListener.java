package com.febrie.dpp.listener;

import com.febrie.dpp.SimpleRPGMain;
import com.febrie.dpp.manager.PlayerDataService;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

public class ServerLifecycleListener implements Listener {

    private final SimpleRPGMain plugin;
    private final PlayerDataService playerDataService;

    public ServerLifecycleListener(SimpleRPGMain plugin, PlayerDataService playerDataService) {
        this.plugin = plugin;
        this.playerDataService = playerDataService;
        
        // 주기적으로 패시브 효과 업데이트 (30초마다)
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : plugin.getServer().getOnlinePlayers()) {
                    playerDataService.getPlayerSkills(player.getUniqueId(), player.getName())
                            .thenAccept(skills -> {
                                if (skills != null) {
                                    plugin.getSkillManager().updatePlayerEffects(player, skills);
                                }
                            });
                }
            }
        }.runTaskTimer(plugin, 600L, 600L); // 30초마다 실행
    }

    @EventHandler
    public void onPlayerJoin(@NotNull PlayerJoinEvent event) {
        Player player = event.getPlayer();

        playerDataService.getPlayerSkills(player.getUniqueId(), player.getName())
                .thenAccept(skills -> {
                    if (skills != null) {
                        plugin.getSkillManager().updatePlayerEffects(player, skills);
                        plugin.getScoreboardManager().setScore(player, "virtual_level", skills.virtualLevel());
                    }
                });

        playerDataService.getPlayerCooldowns(player.getUniqueId());
    }

    @EventHandler
    public void onPlayerQuit(@NotNull PlayerQuitEvent event) {
        Player player = event.getPlayer();
        playerDataService.removePlayer(player.getUniqueId());
    }
    
    @EventHandler
    public void onPlayerRespawn(@NotNull PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        
        // 리스폰 후 약간의 딜레이를 주고 효과 재적용
        new BukkitRunnable() {
            @Override
            public void run() {
                playerDataService.getPlayerSkills(player.getUniqueId(), player.getName())
                        .thenAccept(skills -> {
                            if (skills != null) {
                                plugin.getSkillManager().updatePlayerEffects(player, skills);
                            }
                        });
            }
        }.runTaskLater(plugin, 20L); // 1초 후 실행
    }
}