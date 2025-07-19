package com.febrie.dpp.listener;

import com.febrie.dpp.manager.CoreGameManager;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.IronGolem;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CoreGameListener implements Listener {

    private final CoreGameManager coreGameManager;
    private final NamespacedKey teamKey;

    public CoreGameListener(@NotNull CoreGameManager coreGameManager) {
        this.coreGameManager = coreGameManager;
        this.teamKey = new NamespacedKey(coreGameManager.getPlugin(), "team");
    }

    @EventHandler
    public void onPlayerInteract(@NotNull PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (item == null || item.getType() != Material.IRON_GOLEM_SPAWN_EGG) {
            return;
        }

        if (item.getItemMeta() == null) return;

        String teamName = item.getItemMeta().getPersistentDataContainer()
                .get(teamKey, PersistentDataType.STRING);

        if (teamName != null && coreGameManager.isGameActive()) {
            if (coreGameManager.spawnCore(player, teamName)) {
                item.setAmount(item.getAmount() - 1);
                event.setCancelled(true);
            } else {
                player.sendMessage(Component.text("§c코어를 소환할 수 없습니다! (이미 소환되었거나 게임이 진행중이지 않습니다)"));
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onEntityDeath(@NotNull EntityDeathEvent event) {
        if (!(event.getEntity() instanceof IronGolem ironGolem)) return;

        if (ironGolem.customName() != null) {
            String customNameStr = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(ironGolem.customName());
            if (customNameStr.contains("코어")) {
                String teamName = extractTeamFromCoreName(customNameStr);
                if (teamName != null) {
                    coreGameManager.destroyCore(teamName, "파괴되었습니다!");
                }
            }
        }
    }

    private @Nullable String extractTeamFromCoreName(@NotNull String coreName) {
        if (coreName.contains("RED") || coreName.contains("빨간")) return "red";
        if (coreName.contains("BLUE") || coreName.contains("파란")) return "blue";
        if (coreName.contains("GREEN") || coreName.contains("초록")) return "green";
        if (coreName.contains("YELLOW") || coreName.contains("노란")) return "yellow";
        return null;
    }
}