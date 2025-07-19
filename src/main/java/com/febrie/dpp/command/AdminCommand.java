package com.febrie.dpp.command;

import com.febrie.dpp.SimpleRPGMain;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

public class AdminCommand implements CommandExecutor {

    private final SimpleRPGMain plugin;

    public AdminCommand(SimpleRPGMain plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, @NotNull Command command, @NotNull String label, String @NotNull [] args) {
        if (!sender.hasPermission("simplerp.admin")) {
            sender.sendMessage("§c이 명령어를 사용할 권한이 없습니다.");
            return true;
        }

        if (args.length != 1) {
            sender.sendMessage("§c사용법: /admin <reload>");
            return true;
        }

        if (args[0].equalsIgnoreCase("reload")) {
            plugin.reloadConfig();
            plugin.getShopManager().refreshNPCs();
            sender.sendMessage("§a플러그인이 리로드되었습니다!");
        } else {
            sender.sendMessage("§c알 수 없는 명령어: " + args[0]);
            sender.sendMessage("§7사용 가능한 명령어: reload");
        }

        return true;
    }
}