package com.febrie.dpp.manager;

import com.febrie.dpp.SimpleRPGMain;
import com.febrie.dpp.dto.PlayerSkillState;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;

public class SkillManager {

    private final SimpleRPGMain plugin;
    private final PlayerDataService playerDataService;
    private final CooldownManager cooldownManager;
    private final ScoreboardManager scoreboardManager;

    private final NamespacedKey skillIdKey;

    public SkillManager(SimpleRPGMain plugin, PlayerDataService playerDataService,
                        CooldownManager cooldownManager, ScoreboardManager scoreboardManager) {
        this.plugin = plugin;
        this.playerDataService = playerDataService;
        this.cooldownManager = cooldownManager;
        this.scoreboardManager = scoreboardManager;
        this.skillIdKey = new NamespacedKey(plugin, "skill_id");
    }

    @SuppressWarnings("deprecation")
    public ItemStack createSkillBook(String skillId) {
        ItemStack book = new ItemStack(Material.BOOK);
        ItemMeta meta = book.getItemMeta();

        String displayName = getSkillDisplayName(skillId);
        List<String> lore = getSkillLore(skillId);

        meta.displayName(Component.text(displayName));
        meta.lore(lore.stream().map(Component::text).toList());
        meta.getPersistentDataContainer().set(skillIdKey, PersistentDataType.STRING, skillId);
        meta.setCustomModelData(getSkillModelData(skillId));

        book.setItemMeta(meta);
        return book;
    }

    @SuppressWarnings("deprecation")
    public ItemStack createSkillRemovalPotion(String skillId) {
        ItemStack potion = new ItemStack(Material.POTION);
        ItemMeta meta = potion.getItemMeta();

        String displayName = getSkillDisplayName(skillId) + " §c삭제 포션";
        List<String> lore = Arrays.asList(
                "§7이 포션을 마시면",
                "§7" + getSkillDisplayName(skillId) + " §7스킬이 삭제됩니다.",
                "§c주의: 되돌릴 수 없습니다!"
        );

        meta.displayName(Component.text(displayName));
        meta.lore(lore.stream().map(Component::text).toList());
        meta.getPersistentDataContainer().set(skillIdKey, PersistentDataType.STRING, skillId);
        meta.setCustomModelData(getSkillModelData(skillId) + 100);

        potion.setItemMeta(meta);
        return potion;
    }

    public boolean learnSkill(@NotNull Player player, String skillId) {
        return playerDataService.getPlayerSkills(player.getUniqueId(), player.getName())
                .thenCompose(skills -> {
                    if (hasSkill(skills, skillId)) {
                        player.sendMessage(Component.text("§c이미 보유하고 있는 스킬입니다!"));
                        return java.util.concurrent.CompletableFuture.completedFuture(false);
                    }

                    PlayerSkillState newSkills = updateSkillState(skills, skillId, true);
                    return playerDataService.updatePlayerSkills(newSkills)
                            .thenApply(v -> {
                                player.sendMessage(Component.text("§a" + getSkillDisplayName(skillId) + " §a스킬을 습득했습니다!"));
                                updatePlayerEffects(player, newSkills);
                                return true;
                            });
                }).join();
    }

    public boolean removeSkill(@NotNull Player player, String skillId) {
        return playerDataService.getPlayerSkills(player.getUniqueId(), player.getName())
                .thenCompose(skills -> {
                    if (!hasSkill(skills, skillId)) {
                        player.sendMessage(Component.text("§c보유하지 않은 스킬입니다!"));
                        return java.util.concurrent.CompletableFuture.completedFuture(false);
                    }

                    PlayerSkillState newSkills = updateSkillState(skills, skillId, false);
                    return playerDataService.updatePlayerSkills(newSkills)
                            .thenApply(v -> {
                                player.sendMessage(Component.text("§e" + getSkillDisplayName(skillId) + " §e스킬을 삭제했습니다!"));
                                updatePlayerEffects(player, newSkills);
                                return true;
                            });
                }).join();
    }

    public void updateVirtualLevel(@NotNull Player player, int newLevel) {
        playerDataService.getPlayerSkills(player.getUniqueId(), player.getName())
                .thenCompose(skills -> {
                    PlayerSkillState newSkills = new PlayerSkillState(
                            skills.playerId(),
                            skills.playerName(),
                            skills.burningStep(),
                            skills.pantsRun(),
                            skills.crystalProtection(),
                            skills.flyingCarpet(),
                            skills.rageStrike(),
                            skills.bombDog(),
                            newLevel,
                            System.currentTimeMillis()
                    );

                    scoreboardManager.setScore(player, "virtual_level", newLevel);
                    updatePlayerEffects(player, newSkills);

                    return playerDataService.updatePlayerSkills(newSkills);
                });
    }

    public void updatePlayerEffects(Player player, @NotNull PlayerSkillState skills) {
        if (skills.rageStrike()) {
            int strengthLevel = skills.virtualLevel() / 10;
            if (strengthLevel > 0) {
                player.addPotionEffect(new PotionEffect(
                        PotionEffectType.STRENGTH,
                        Integer.MAX_VALUE,
                        strengthLevel - 1,
                        false,
                        false
                ));
            }
        }
    }
    
    public void applyPlayerEffectsOnJoin(@NotNull Player player) {
        playerDataService.getPlayerSkills(player.getUniqueId(), player.getName())
                .thenAccept(skills -> updatePlayerEffects(player, skills));
    }

    private boolean hasSkill(PlayerSkillState skills, @NotNull String skillId) {
        return switch (skillId) {
            case "burning_step" -> skills.burningStep();
            case "pants_run" -> skills.pantsRun();
            case "crystal_protection" -> skills.crystalProtection();
            case "flying_carpet" -> skills.flyingCarpet();
            case "rage_strike" -> skills.rageStrike();
            case "bomb_dog" -> skills.bombDog();
            default -> false;
        };
    }
    
    public boolean hasSkill(@NotNull Player player, @NotNull String skillId) {
        return playerDataService.getPlayerSkills(player.getUniqueId(), player.getName())
                .thenApply(skills -> hasSkill(skills, skillId))
                .join();
    }

    private PlayerSkillState updateSkillState(PlayerSkillState skills, @NotNull String skillId, boolean value) {
        return switch (skillId) {
            case "burning_step" -> new PlayerSkillState(skills.playerId(), skills.playerName(),
                    value, skills.pantsRun(), skills.crystalProtection(), skills.flyingCarpet(),
                    skills.rageStrike(), skills.bombDog(), skills.virtualLevel(), System.currentTimeMillis());
            case "pants_run" -> new PlayerSkillState(skills.playerId(), skills.playerName(),
                    skills.burningStep(), value, skills.crystalProtection(), skills.flyingCarpet(),
                    skills.rageStrike(), skills.bombDog(), skills.virtualLevel(), System.currentTimeMillis());
            case "crystal_protection" -> new PlayerSkillState(skills.playerId(), skills.playerName(),
                    skills.burningStep(), skills.pantsRun(), value, skills.flyingCarpet(),
                    skills.rageStrike(), skills.bombDog(), skills.virtualLevel(), System.currentTimeMillis());
            case "flying_carpet" -> new PlayerSkillState(skills.playerId(), skills.playerName(),
                    skills.burningStep(), skills.pantsRun(), skills.crystalProtection(), value,
                    skills.rageStrike(), skills.bombDog(), skills.virtualLevel(), System.currentTimeMillis());
            case "rage_strike" -> new PlayerSkillState(skills.playerId(), skills.playerName(),
                    skills.burningStep(), skills.pantsRun(), skills.crystalProtection(), skills.flyingCarpet(),
                    value, skills.bombDog(), skills.virtualLevel(), System.currentTimeMillis());
            case "bomb_dog" -> new PlayerSkillState(skills.playerId(), skills.playerName(),
                    skills.burningStep(), skills.pantsRun(), skills.crystalProtection(), skills.flyingCarpet(),
                    skills.rageStrike(), value, skills.virtualLevel(), System.currentTimeMillis());
            default -> skills;
        };
    }

    @Contract(pure = true)
    private String getSkillDisplayName(@NotNull String skillId) {
        return switch (skillId) {
            case "burning_step" -> "§c버닝 스텝";
            case "pants_run" -> "§b빤스런";
            case "crystal_protection" -> "§f크리스탈 프로텍션";
            case "flying_carpet" -> "§d마법의 양탄자";
            case "rage_strike" -> "§4분노의 일격";
            case "bomb_dog" -> "§e폭견";
            default -> skillId;
        };
    }

    private @NotNull List<String> getSkillLore(@NotNull String skillId) {
        return switch (skillId) {
            case "burning_step" -> Arrays.asList(
                    "§7검을 우클릭하여 발동",
                    "§7바라보는 방향으로 5블록 대시",
                    "§7화염 저항 5초 부여",
                    "§7주변 몹에게 5 데미지",
                    "§7쿨타임: 8초"
            );
            case "pants_run" -> Arrays.asList(
                    "§7책에 좌표를 적고 던져서 발동",
                    "§7해당 좌표로 텔레포트",
                    "§7쿨타임: 30초"
            );
            case "crystal_protection" -> Arrays.asList(
                    "§7방패를 우클릭하여 발동",
                    "§7플레이어 중심 3×3×3 얼음 구체 생성",
                    "§7쿨타임: 60초"
            );
            case "flying_carpet" -> Arrays.asList(
                    "§7마인카트에 탑승하여 발동",
                    "§7바라보는 방향으로 부드러운 이동",
                    "§7마인카트에서 내릴 때까지 지속"
            );
            case "rage_strike" -> Arrays.asList(
                    "§7패시브 스킬",
                    "§7가상 레벨 10당 힘 1 효과",
                    "§7영구 지속"
            );
            case "bomb_dog" -> Arrays.asList(
                    "§7뼈를 던져서 발동",
                    "§7늑대 2마리 소환",
                    "§7가장 가까운 적 추적 후 폭발",
                    "§730초 후 자동 소멸",
                    "§7쿨타임: 60초"
            );
            default -> Arrays.asList("§7알 수 없는 스킬");
        };
    }

    @Contract(pure = true)
    private int getSkillModelData(@NotNull String skillId) {
        return switch (skillId) {
            case "burning_step" -> 1001;
            case "pants_run" -> 1002;
            case "crystal_protection" -> 1003;
            case "flying_carpet" -> 1004;
            case "rage_strike" -> 1005;
            case "bomb_dog" -> 1006;
            default -> 1000;
        };
    }

    public String getSkillIdFromItem(ItemStack item) {
        if (item == null || item.getItemMeta() == null) return null;
        return item.getItemMeta().getPersistentDataContainer().get(skillIdKey, PersistentDataType.STRING);
    }

    public SimpleRPGMain getPlugin() {
        return plugin;
    }
}