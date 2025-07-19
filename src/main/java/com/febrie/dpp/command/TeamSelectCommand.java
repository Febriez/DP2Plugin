package com.febrie.dpp.command;

import com.febrie.dpp.manager.TeamManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class TeamSelectCommand implements CommandExecutor {

    private final TeamManager teamManager;

    public TeamSelectCommand(TeamManager teamManager) {
        this.teamManager = teamManager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String @NotNull [] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§c플레이어만 사용할 수 있는 명령어입니다.");
            return true;
        }

        if (args.length != 1) {
            player.sendMessage("§c사용법: /teamselect <red|blue|green|yellow>");
            return true;
        }

        String teamName = args[0].toLowerCase();
        if (!isValidTeam(teamName)) {
            player.sendMessage("§c올바른 팀을 선택하세요: red, blue, green, yellow");
            return true;
        }

        if (teamManager.joinTeam(player, teamName)) {
            return true;
        } else {
            player.sendMessage("§c팀 참가에 실패했습니다.");
            return true;
        }
    }

    private boolean isValidTeam(String teamName) {
        return teamName.equals("red") || teamName.equals("blue") ||
                teamName.equals("green") || teamName.equals("yellow");
    }
}