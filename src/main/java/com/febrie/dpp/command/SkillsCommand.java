package com.febrie.dpp.command;

import com.febrie.dpp.manager.PlayerDataService;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class SkillsCommand implements CommandExecutor {

    private final PlayerDataService playerDataService;

    public SkillsCommand(PlayerDataService playerDataService) {
        this.playerDataService = playerDataService;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String @NotNull [] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§c플레이어만 사용할 수 있는 명령어입니다.");
            return true;
        }

        playerDataService.getPlayerSkills(player.getUniqueId(), player.getName())
                .thenAccept(skills -> {
                    if (skills == null) {
                        player.sendMessage("§c스킬 정보를 불러올 수 없습니다.");
                        return;
                    }

                    player.sendMessage("§e====== §a보유 스킬 목록 §e======");
                    player.sendMessage("§7가상 레벨: §f" + skills.virtualLevel());
                    player.sendMessage("");

                    List<String> ownedSkills = new ArrayList<>();
                    if (skills.burningStep()) ownedSkills.add("§c버닝 스텝");
                    if (skills.pantsRun()) ownedSkills.add("§b빤스런");
                    if (skills.crystalProtection()) ownedSkills.add("§f크리스탈 프로텍션");
                    if (skills.flyingCarpet()) ownedSkills.add("§d마법의 양탄자");
                    if (skills.rageStrike()) ownedSkills.add("§4분노의 일격");
                    if (skills.bombDog()) ownedSkills.add("§e폭견");

                    if (ownedSkills.isEmpty()) {
                        player.sendMessage("§7보유한 스킬이 없습니다.");
                    } else {
                        for (String skill : ownedSkills) {
                            player.sendMessage("§a✓ " + skill);
                        }
                    }

                    player.sendMessage("");
                    player.sendMessage("§7자세한 스킬 정보: §f/skillinfo <스킬명>");
                });

        return true;
    }
}