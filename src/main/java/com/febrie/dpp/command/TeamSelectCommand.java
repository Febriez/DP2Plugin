package com.febrie.dpp.command;

import com.febrie.dpp.manager.TeamManager;
import net.kyori.adventure.text.Component;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TeamSelectCommand implements CommandExecutor, TabCompleter {

    private final TeamManager teamManager;

    public TeamSelectCommand(TeamManager teamManager) {
        this.teamManager = teamManager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String @NotNull [] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("§c플레이어만 사용할 수 있는 명령어입니다."));
            return true;
        }

        if (args.length != 1) {
            player.sendMessage(Component.text("§c사용법: /" + label + " <빨강|파랑|초록|노랑>"));
            return true;
        }

        String teamName = convertToEnglish(args[0].toLowerCase());
        if (!isValidTeam(teamName)) {
            player.sendMessage(Component.text("§c올바른 팀을 선택하세요: 빨강, 파랑, 초록, 노랑"));
            return true;
        }

        if (teamManager.joinTeam(player, teamName)) {
            return true;
        } else {
            player.sendMessage(Component.text("§c팀 참가에 실패했습니다."));
            return true;
        }
    }

    private boolean isValidTeam(String teamName) {
        return teamName.equals("red") || teamName.equals("blue") ||
                teamName.equals("green") || teamName.equals("yellow");
    }
    
    private String convertToEnglish(String koreanTeam) {
        return switch (koreanTeam) {
            case "빨강" -> "red";
            case "파랑" -> "blue";
            case "초록" -> "green";
            case "노랑" -> "yellow";
            default -> koreanTeam;
        };
    }
    
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, String @NotNull [] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            completions.addAll(Arrays.asList("빨강", "파랑", "초록", "노랑"));
            
            String lowercaseInput = args[0].toLowerCase();
            completions.removeIf(completion -> !completion.toLowerCase().startsWith(lowercaseInput));
        }
        
        return completions;
    }
}