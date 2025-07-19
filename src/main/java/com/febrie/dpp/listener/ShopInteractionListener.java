package com.febrie.dpp.listener;

import com.febrie.dpp.manager.ShopManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.player.PlayerInteractEvent;

public class ShopInteractionListener implements Listener {
    
    private final ShopManager shopManager;
    
    public ShopInteractionListener(ShopManager shopManager) {
        this.shopManager = shopManager;
    }
    
    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        
        if (shopManager.isInProtectedArea(event.getBlock().getLocation())) {
            if (!shopManager.canBypass(player)) {
                event.setCancelled(true);
                player.sendMessage("§c상점 보호구역에서는 블록을 파괴할 수 없습니다!");
            }
        }
    }
    
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        
        if (event.getClickedBlock() != null && 
            shopManager.isInProtectedArea(event.getClickedBlock().getLocation())) {
            
            if (!shopManager.canBypass(player) && 
                (event.getAction().name().contains("BLOCK"))) {
                event.setCancelled(true);
                player.sendMessage("§c상점 보호구역에서는 블록과 상호작용할 수 없습니다!");
            }
        }
    }
    
    @EventHandler
    public void onProjectileLaunch(ProjectileLaunchEvent event) {
        if (!(event.getEntity().getShooter() instanceof Player player)) return;
        
        if (shopManager.isInProtectedArea(player.getLocation())) {
            if (!shopManager.canBypass(player)) {
                event.setCancelled(true);
                player.sendMessage("§c상점 보호구역에서는 투사체를 사용할 수 없습니다!");
            }
        }
    }
}