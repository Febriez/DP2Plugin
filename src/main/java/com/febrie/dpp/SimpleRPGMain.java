package com.febrie.dpp;

import com.febrie.dpp.command.*;
import com.febrie.dpp.database.DatabaseManager;
import com.febrie.dpp.listener.*;
import com.febrie.dpp.manager.*;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Level;

public final class SimpleRPGMain extends JavaPlugin {

    private DatabaseManager databaseManager;
    private PlayerDataService playerDataService;
    private CooldownManager cooldownManager;
    private ScoreboardManager scoreboardManager;
    private SkillManager skillManager;
    private TeamManager teamManager;
    private ShopManager shopManager;
    private CoreGameManager coreGameManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        
        databaseManager = new DatabaseManager(this);
        databaseManager.initialize().thenAccept(success -> {
            if (!success) {
                getLogger().severe("Failed to initialize database! Disabling plugin...");
                getServer().getPluginManager().disablePlugin(this);
                return;
            }
            
            initializeManagers();
            registerCommands();
            registerListeners();
            
            getLogger().info("SimpleRPG Plugin has been enabled successfully!");
        });
    }
    
    private void initializeManagers() {
        playerDataService = new PlayerDataService(this, databaseManager);
        cooldownManager = new CooldownManager(this, playerDataService);
        scoreboardManager = new ScoreboardManager(this);
        skillManager = new SkillManager(this, playerDataService, cooldownManager, scoreboardManager);
        teamManager = new TeamManager(this, databaseManager);
        shopManager = new ShopManager(this, skillManager);
        coreGameManager = new CoreGameManager(this, teamManager, scoreboardManager);
        
        getLogger().info("All managers initialized successfully!");
    }
    
    private void registerCommands() {
        getCommand("teamselect").setExecutor(new TeamSelectCommand(teamManager));
        getCommand("startcore").setExecutor(new StartCoreCommand(coreGameManager));
        getCommand("skills").setExecutor(new SkillsCommand(playerDataService));
        getCommand("skillinfo").setExecutor(new SkillInfoCommand());
        getCommand("admin").setExecutor(new AdminCommand(this));
    }
    
    private void registerListeners() {
        getServer().getPluginManager().registerEvents(new PlayerSkillListener(skillManager, cooldownManager), this);
        getServer().getPluginManager().registerEvents(new PlayerTeamListener(teamManager), this);
        getServer().getPluginManager().registerEvents(new ShopInteractionListener(shopManager), this);
        getServer().getPluginManager().registerEvents(new CoreGameListener(coreGameManager), this);
        getServer().getPluginManager().registerEvents(new ServerLifecycleListener(this, playerDataService), this);
    }

    @Override
    public void onDisable() {
        if (databaseManager != null) {
            databaseManager.close();
        }
        
        getLogger().info("SimpleRPG Plugin has been disabled!");
    }
    
    public DatabaseManager getDatabaseManager() { return databaseManager; }
    public PlayerDataService getPlayerDataService() { return playerDataService; }
    public CooldownManager getCooldownManager() { return cooldownManager; }
    public ScoreboardManager getScoreboardManager() { return scoreboardManager; }
    public SkillManager getSkillManager() { return skillManager; }
    public TeamManager getTeamManager() { return teamManager; }
    public ShopManager getShopManager() { return shopManager; }
    public CoreGameManager getCoreGameManager() { return coreGameManager; }
}
