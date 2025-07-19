package com.febrie.dpp.manager;

import com.febrie.dpp.SimpleRPGMain;
import com.febrie.dpp.dto.CoreEntityData;
import com.febrie.dpp.dto.CoreGameState;
import com.febrie.dpp.dto.TeamData;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;
import java.util.stream.Collectors;

public class CoreGameManager {
    
    private final SimpleRPGMain plugin;
    private final TeamManager teamManager;
    private final ScoreboardManager scoreboardManager;
    private final NamespacedKey teamKey;
    
    private CoreGameState gameState;
    private BukkitRunnable gameTask;
    private final Map<UUID, BukkitRunnable> coreTasks = new HashMap<>();
    
    public CoreGameManager(SimpleRPGMain plugin, TeamManager teamManager, ScoreboardManager scoreboardManager) {
        this.plugin = plugin;
        this.teamManager = teamManager;
        this.scoreboardManager = scoreboardManager;
        this.teamKey = new NamespacedKey(plugin, "team");
        this.gameState = CoreGameState.createDefault();
    }
    
    public boolean startCoreGame() {
        if (gameState.active()) {
            return false;
        }
        
        Map<String, TeamData> teams = teamManager.getAllTeams();
        if (teams.size() < 2) {
            return false;
        }
        
        long startTime = System.currentTimeMillis();
        long endTime = startTime + (420 * 1000);
        
        gameState = new CoreGameState(
            true,
            startTime,
            endTime,
            new HashMap<>(),
            CoreGameState.GamePhase.PREPARATION
        );
        
        distributeCoreEggs();
        startGameLoop();
        
        Bukkit.broadcastMessage("§c§l코어전이 시작됩니다!");
        Bukkit.broadcastMessage("§e각 팀에게 코어 소환 알이 지급되었습니다!");
        
        return true;
    }
    
    private void distributeCoreEggs() {
        for (TeamData team : teamManager.getAllTeams().values()) {
            ItemStack coreEgg = createCoreEgg(team.teamName());
            
            for (UUID playerId : team.members()) {
                Player player = Bukkit.getPlayer(playerId);
                if (player != null && player.isOnline()) {
                    player.getInventory().addItem(coreEgg.clone());
                    player.sendMessage("§a코어 소환 알을 받았습니다! 우클릭하여 코어를 소환하세요!");
                }
            }
        }
    }
    
    private ItemStack createCoreEgg(String teamName) {
        ItemStack egg = new ItemStack(Material.IRON_GOLEM_SPAWN_EGG);
        ItemMeta meta = egg.getItemMeta();
        
        String teamColor = getTeamColor(teamName);
        meta.setDisplayName(teamColor + teamName.toUpperCase() + " 팀 코어 소환 알");
        meta.setLore(Arrays.asList(
            "§7우클릭하여 코어를 소환합니다.",
            "§7팀당 하나의 코어만 소환 가능합니다.",
            "§c물에 닿으면 코어가 파괴됩니다!"
        ));
        meta.getPersistentDataContainer().set(teamKey, PersistentDataType.STRING, teamName);
        
        egg.setItemMeta(meta);
        return egg;
    }
    
    public boolean spawnCore(Player player, String teamName) {
        if (!gameState.active() || gameState.teamCores().containsKey(teamName)) {
            return false;
        }
        
        Location spawnLoc = player.getLocation();
        IronGolem core = (IronGolem) spawnLoc.getWorld().spawnEntity(spawnLoc, EntityType.IRON_GOLEM);
        
        String teamColor = getTeamColor(teamName);
        core.setCustomName(teamColor + teamName.toUpperCase() + " 팀 코어");
        core.setCustomNameVisible(true);
        core.setPlayerCreated(false);
        
        CoreEntityData coreData = CoreEntityData.create(teamName, core.getUniqueId(), spawnLoc);
        gameState.teamCores().put(teamName, coreData);
        
        startCoreAI(core, teamName);
        
        Bukkit.broadcastMessage(teamColor + teamName.toUpperCase() + " 팀이 코어를 소환했습니다!");
        return true;
    }
    
    private void startCoreAI(IronGolem core, String teamName) {
        BukkitRunnable coreTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (core.isDead() || !gameState.active()) {
                    this.cancel();
                    coreTasks.remove(core.getUniqueId());
                    return;
                }
                
                if (core.getLocation().getBlock().getType() == Material.WATER) {
                    destroyCore(teamName, "물에 닿아서 파괴되었습니다!");
                    this.cancel();
                    return;
                }
                
                moveCore(core);
                applyEffectsToNearbyPlayers(core, teamName);
                clearNearbyMobs(core);
            }
        };
        
        coreTask.runTaskTimer(plugin, 0, 20 * 15);
        coreTasks.put(core.getUniqueId(), coreTask);
    }
    
    private void moveCore(IronGolem core) {
        Location currentLoc = core.getLocation();
        double angle = Math.random() * 2 * Math.PI;
        double distance = 5 + Math.random() * 15;
        
        double x = currentLoc.getX() + Math.cos(angle) * distance;
        double z = currentLoc.getZ() + Math.sin(angle) * distance;
        int y = currentLoc.getWorld().getHighestBlockYAt((int) x, (int) z) + 1;
        
        Location newLoc = new Location(currentLoc.getWorld(), x, y, z);
        core.teleport(newLoc);
        
        core.getWorld().spawnParticle(Particle.PORTAL, newLoc, 50, 1, 1, 1, 0.1);
    }
    
    private void applyEffectsToNearbyPlayers(IronGolem core, String coreTeam) {
        for (Entity entity : core.getNearbyEntities(30, 30, 30)) {
            if (entity instanceof Player player) {
                String playerTeam = teamManager.getPlayerTeam(player.getUniqueId());
                
                if (playerTeam != null && !playerTeam.equals(coreTeam)) {
                    player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 40, 2));
                    player.addPotionEffect(new PotionEffect(PotionEffectType.MINING_FATIGUE, 40, 2));
                }
            }
        }
    }
    
    private void clearNearbyMobs(IronGolem core) {
        for (Entity entity : core.getNearbyEntities(30, 30, 30)) {
            if (entity instanceof Monster || (entity instanceof Animals && !(entity instanceof Player))) {
                entity.remove();
            }
        }
    }
    
    public void destroyCore(String teamName, String reason) {
        CoreEntityData coreData = gameState.teamCores().get(teamName);
        if (coreData != null) {
            Entity core = Bukkit.getEntity(coreData.entityId());
            if (core != null) {
                core.remove();
            }
            
            BukkitRunnable task = coreTasks.remove(coreData.entityId());
            if (task != null) {
                task.cancel();
            }
            
            gameState.teamCores().remove(teamName);
            teamManager.eliminateTeam(teamName);
            
            String teamColor = getTeamColor(teamName);
            Bukkit.broadcastMessage(teamColor + teamName.toUpperCase() + " 팀의 코어가 " + reason);
            
            TeamData team = teamManager.getTeam(teamName);
            if (team != null) {
                for (UUID playerId : team.members()) {
                    Player player = Bukkit.getPlayer(playerId);
                    if (player != null) {
                        player.sendTitle("§c패배", "§7코어가 파괴되었습니다", 10, 70, 20);
                    }
                }
            }
            
            checkGameEnd();
        }
    }
    
    private void startGameLoop() {
        gameTask = new BukkitRunnable() {
            @Override
            public void run() {
                long timeLeft = gameState.endTime() - System.currentTimeMillis();
                
                if (timeLeft <= 0) {
                    endGame();
                    return;
                }
                
                int secondsLeft = (int) (timeLeft / 1000);
                if (secondsLeft % 60 == 0 || secondsLeft <= 10) {
                    Bukkit.broadcastMessage("§e코어전 종료까지 " + secondsLeft + "초!");
                }
            }
        };
        
        gameTask.runTaskTimer(plugin, 0, 20);
    }
    
    private void checkGameEnd() {
        Map<String, TeamData> aliveTeams = teamManager.getAllTeams().entrySet().stream()
            .filter(entry -> !entry.getValue().eliminated())
            .filter(entry -> gameState.teamCores().containsKey(entry.getKey()))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        
        if (aliveTeams.size() <= 1) {
            if (aliveTeams.size() == 1) {
                String winnerTeam = aliveTeams.keySet().iterator().next();
                declareWinner(winnerTeam);
            } else {
                endGame();
            }
        }
    }
    
    private void declareWinner(String teamName) {
        teamManager.setTeamWin(teamName);
        String teamColor = getTeamColor(teamName);
        
        Bukkit.broadcastMessage("§a§l" + teamName.toUpperCase() + " 팀이 승리했습니다!");
        
        TeamData winnerTeam = teamManager.getTeam(teamName);
        if (winnerTeam != null) {
            for (UUID playerId : winnerTeam.members()) {
                Player player = Bukkit.getPlayer(playerId);
                if (player != null) {
                    player.sendTitle("§a승리!", "§7코어전에서 승리했습니다!", 10, 70, 20);
                    giveRewards(player);
                }
            }
        }
        
        endGame();
    }
    
    private void giveRewards(Player player) {
        player.getInventory().addItem(new ItemStack(Material.DIAMOND, 5));
        player.getInventory().addItem(new ItemStack(Material.EMERALD, 10));
        player.sendMessage("§a승리 보상을 받았습니다!");
    }
    
    private void endGame() {
        gameState = CoreGameState.createDefault();
        
        if (gameTask != null) {
            gameTask.cancel();
        }
        
        for (BukkitRunnable task : coreTasks.values()) {
            task.cancel();
        }
        coreTasks.clear();
        
        scoreboardManager.resetAllScores();
        teamManager.resetAllTeams();
        
        Bukkit.broadcastMessage("§c코어전이 종료되었습니다!");
    }
    
    private String getTeamColor(String teamName) {
        return switch (teamName) {
            case "red" -> "§c";
            case "blue" -> "§9";
            case "green" -> "§a";
            case "yellow" -> "§e";
            default -> "§f";
        };
    }
    
    public boolean isGameActive() {
        return gameState.active();
    }
    
    public CoreGameState getGameState() {
        return gameState;
    }
    
    public SimpleRPGMain getPlugin() {
        return plugin;
    }
}