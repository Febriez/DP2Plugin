package com.febrie.dpp.command;

import com.febrie.dpp.manager.CoreGameManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class StartCoreCommand implements CommandExecutor {
    
    private final CoreGameManager coreGameManager;
    
    public StartCoreCommand(CoreGameManager coreGameManager) {
        this.coreGameManager = coreGameManager;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("simplerp.admin")) {
            sender.sendMessage("§c이 명령어를 사용할 권한이 없습니다.");
            return true;
        }
        
        if (coreGameManager.isGameActive()) {
            sender.sendMessage("§c이미 코어전이 진행 중입니다!");
            return true;
        }
        
        if (coreGameManager.startCoreGame()) {
            sender.sendMessage("§a코어전을 시작했습니다!");
        } else {
            sender.sendMessage("§c코어전을 시작할 수 없습니다. (최소 2팀 필요)");
        }
        
        return true;
    }
}