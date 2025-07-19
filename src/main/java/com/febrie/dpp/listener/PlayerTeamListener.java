package com.febrie.dpp.listener;

import com.febrie.dpp.manager.TeamManager;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

public class PlayerTeamListener implements Listener {
    
    private final TeamManager teamManager;
    
    public PlayerTeamListener(TeamManager teamManager) {
        this.teamManager = teamManager;
    }
    
    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim)) return;
        
        Player attacker = null;
        
        if (event.getDamager() instanceof Player) {
            attacker = (Player) event.getDamager();
        } else if (event.getDamager() instanceof Projectile projectile) {
            if (projectile.getShooter() instanceof Player) {
                attacker = (Player) projectile.getShooter();
            }
        }
        
        if (attacker == null) return;
        
        if (teamManager.areTeammates(attacker.getUniqueId(), victim.getUniqueId())) {
            event.setCancelled(true);
            attacker.sendMessage("§c같은 팀 멤버를 공격할 수 없습니다!");
        }
    }
}