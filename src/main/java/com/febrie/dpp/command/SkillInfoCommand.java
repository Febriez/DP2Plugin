package com.febrie.dpp.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class SkillInfoCommand implements CommandExecutor, TabCompleter {

    private final Map<String, String> koreanNames = Map.of(
            "burning_step", "버닝스텝",
            "pants_run", "빤스런",
            "crystal_protection", "크리스탈프로텍션",
            "flying_carpet", "마법의양탄자",
            "rage_strike", "분노의일격",
            "bomb_dog", "폭견"
    );
    
    private final Map<String, List<String>> skillInfos = Map.of(
            "burning_step", Arrays.asList(
                    "§c§l버닝 스텝",
                    "§7발동 방법: §f검 우클릭",
                    "§7효과: §f바라보는 방향으로 5블록 대시",
                    "§7      §f화염 저항 5초 부여",
                    "§7      §f주변 몹에게 5 데미지",
                    "§7쿨타임: §f8초"
            ),
            "pants_run", Arrays.asList(
                    "§b§l빤스런",
                    "§7발동 방법: §f책에 'x y z' 좌표를 적고 던지기",
                    "§7효과: §f해당 좌표로 텔레포트",
                    "§7쿨타임: §f30초"
            ),
            "crystal_protection", Arrays.asList(
                    "§f§l크리스탈 프로텍션",
                    "§7발동 방법: §f방패 우클릭",
                    "§7효과: §f플레이어 중심 3×3×3 얼음 구체 생성",
                    "§7      §f중심은 물 블록",
                    "§7쿨타임: §f60초"
            ),
            "flying_carpet", Arrays.asList(
                    "§d§l마법의 양탄자",
                    "§7발동 방법: §f마인카트에 탑승",
                    "§7효과: §f바라보는 방향으로 부드러운 이동",
                    "§7지속: §f마인카트에서 내릴 때까지",
                    "§7쿨타임: §f없음"
            ),
            "rage_strike", Arrays.asList(
                    "§4§l분노의 일격",
                    "§7발동 방법: §f패시브 (항상 활성)",
                    "§7효과: §f가상 레벨 10당 힘 1 효과",
                    "§7      §f(힘 1 = 추가 데미지 5)",
                    "§7지속: §f영구"
            ),
            "bomb_dog", Arrays.asList(
                    "§e§l폭견",
                    "§7발동 방법: §f뼈 던지기",
                    "§7효과: §f늑대 2마리 소환",
                    "§7      §f가장 가까운 적 추적 후 폭발",
                    "§7지속: §f30초 후 자동 소멸",
                    "§7쿨타임: §f60초"
            )
    );

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (args.length != 1) {
            sender.sendMessage("§c사용법: /" + label + " <스킬명>");
            sender.sendMessage("§7사용 가능한 스킬: 버닝스텝, 빤스런, 크리스탈프로텍션, 마법의양탄자, 분노의일격, 폭견");
            return true;
        }

        String skillName = convertToEnglish(args[0].toLowerCase());
        List<String> info = skillInfos.get(skillName);

        if (info == null) {
            sender.sendMessage("§c존재하지 않는 스킬입니다.");
            sender.sendMessage("§7사용 가능한 스킬: 버닝스텝, 빤스런, 크리스탈프로텍션, 마법의양탄자, 분노의일격, 폭견");
            return true;
        }

        sender.sendMessage("§e=========== §a스킬 정보 §e===========");
        for (String line : info) {
            sender.sendMessage(line);
        }
        sender.sendMessage("§e================================");

        return true;
    }
    
    private String convertToEnglish(String koreanSkill) {
        for (Map.Entry<String, String> entry : koreanNames.entrySet()) {
            if (entry.getValue().equals(koreanSkill)) {
                return entry.getKey();
            }
        }
        return koreanSkill;
    }
    
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, String @NotNull [] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            completions.addAll(koreanNames.values());
            
            String lowercaseInput = args[0].toLowerCase();
            completions.removeIf(completion -> !completion.toLowerCase().startsWith(lowercaseInput));
        }
        
        return completions;
    }
}