package com.febrie.dpp.command;

import com.febrie.dpp.SimpleRPGMain;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class AdminCommand implements CommandExecutor, TabCompleter {

    private final SimpleRPGMain plugin;

    public AdminCommand(SimpleRPGMain plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String @NotNull [] args) {
        if (!sender.hasPermission("simplerp.admin")) {
            sender.sendMessage("§c이 명령어를 사용할 권한이 없습니다.");
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage("§c사용법: /" + label + " <리로드|상점등록>");
            return true;
        }

        String subCommand = convertToEnglish(args[0].toLowerCase());
        
        switch (subCommand) {
            case "reload":
                plugin.reloadConfig();
                plugin.getShopManager().refreshNPCs();
                sender.sendMessage("§a플러그인이 리로드되었습니다!");
                break;
            
            case "registershop":
                if (!(sender instanceof Player)) {
                    sender.sendMessage("§c이 명령어는 플레이어만 사용할 수 있습니다.");
                    return true;
                }
                
                if (args.length != 2) {
                    sender.sendMessage("§c사용법: /" + label + " 상점등록 <타입>");
                    sender.sendMessage("§7사용 가능한 타입: 화폐, 스킬, 포션");
                    return true;
                }
                
                Player player = (Player) sender;
                String shopType = convertShopType(args[1].toLowerCase());
                
                switch (shopType) {
                    case "currency":
                    case "skill":
                    case "potion":
                        boolean result = plugin.getShopManager().registerShop(player.getLocation(), shopType);
                        if (result) {
                            sender.sendMessage("§a" + getShopName(shopType) + " 상점이 등록되었습니다!");
                        } else {
                            sender.sendMessage("§c상점 등록에 실패했습니다.");
                        }
                        break;
                    default:
                        sender.sendMessage("§c알 수 없는 상조 타입: " + args[1]);
                        sender.sendMessage("§7사용 가능한 타입: 화폐, 스킬, 포션");
                        break;
                }
                break;
                
            default:
                sender.sendMessage("§c알 수 없는 명령어: " + args[0]);
                sender.sendMessage("§7사용 가능한 명령어: 리로드, 상점등록");
                break;
        }

        return true;
    }
    
    private String convertToEnglish(String korean) {
        return switch (korean) {
            case "리로드" -> "reload";
            case "상점등록" -> "registershop";
            default -> korean;
        };
    }
    
    private String convertShopType(String korean) {
        return switch (korean) {
            case "화폐" -> "currency";
            case "스킬" -> "skill";
            case "포션" -> "potion";
            default -> korean;
        };
    }
    
    private String getShopName(String type) {
        switch (type) {
            case "currency":
                return "화폐 교환소";
            case "skill":
                return "스킬북 상점";
            case "potion":
                return "스킬 삭제 상점";
            default:
                return "알 수 없는";
        }
    }
    
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, String @NotNull [] args) {
        List<String> completions = new ArrayList<>();
        
        if (!sender.hasPermission("simplerp.admin")) {
            return completions;
        }
        
        if (args.length == 1) {
            completions.addAll(Arrays.asList("리로드", "상점등록"));
            return filterCompletions(completions, args[0]);
        } else if (args.length == 2 && convertToEnglish(args[0].toLowerCase()).equals("registershop")) {
            completions.addAll(Arrays.asList("화폐", "스킬", "포션"));
            return filterCompletions(completions, args[1]);
        }
        
        return completions;
    }
    
    private List<String> filterCompletions(List<String> completions, String input) {
        List<String> filtered = new ArrayList<>();
        String lowercaseInput = input.toLowerCase();
        
        for (String completion : completions) {
            if (completion.toLowerCase().startsWith(lowercaseInput)) {
                filtered.add(completion);
            }
        }
        
        return filtered;
    }
}