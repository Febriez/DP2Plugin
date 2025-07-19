package com.febrie.dpp.listener;

import com.febrie.dpp.SimpleRPGMain;
import com.febrie.dpp.manager.PlayerDataService;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jetbrains.annotations.NotNull;

public class ServerLifecycleListener implements Listener {

    private final SimpleRPGMain plugin;
    private final PlayerDataService playerDataService;

    public ServerLifecycleListener(SimpleRPGMain plugin, PlayerDataService playerDataService) {
        this.plugin = plugin;
        this.playerDataService = playerDataService;
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
}