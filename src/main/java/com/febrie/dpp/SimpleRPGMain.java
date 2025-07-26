package com.febrie.dpp;

import com.febrie.dpp.command.*;
import com.febrie.dpp.database.YamlDataManager;
import com.febrie.dpp.listener.*;
import com.febrie.dpp.manager.*;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;

public final class SimpleRPGMain extends JavaPlugin {

    private YamlDataManager dataManager;
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

        dataManager = new YamlDataManager(this);
        dataManager.initialize().thenAccept(success -> {
            if (!success) {
                getLogger().severe("Failed to initialize data storage! Disabling plugin...");
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
        playerDataService = new PlayerDataService(this, dataManager);
        cooldownManager = new CooldownManager(this, playerDataService);
        scoreboardManager = new ScoreboardManager();
        skillManager = new SkillManager(this, playerDataService, cooldownManager, scoreboardManager);
        teamManager = new TeamManager(this, dataManager);
        shopManager = new ShopManager(this, skillManager);
        coreGameManager = new CoreGameManager(this, teamManager, scoreboardManager);

        getLogger().info("All managers initialized successfully!");
    }

    private void registerCommands() {
        TeamSelectCommand teamSelectCommand = new TeamSelectCommand(teamManager);
        Objects.requireNonNull(getCommand("팀선택")).setExecutor(teamSelectCommand);
        Objects.requireNonNull(getCommand("팀선택")).setTabCompleter(teamSelectCommand);
        Objects.requireNonNull(getCommand("코어전시작")).setExecutor(new StartCoreCommand(coreGameManager));
        Objects.requireNonNull(getCommand("스킬목록")).setExecutor(new SkillsCommand(playerDataService));
        SkillInfoCommand skillInfoCommand = new SkillInfoCommand();
        Objects.requireNonNull(getCommand("스킬정보")).setExecutor(skillInfoCommand);
        Objects.requireNonNull(getCommand("스킬정보")).setTabCompleter(skillInfoCommand);
        AdminCommand adminCommand = new AdminCommand(this);
        Objects.requireNonNull(getCommand("관리자")).setExecutor(adminCommand);
        Objects.requireNonNull(getCommand("관리자")).setTabCompleter(adminCommand);
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
        if (dataManager != null) {
            dataManager.close();
        }

        getLogger().info("SimpleRPG Plugin has been disabled!");
    }

    public YamlDataManager getDataManager() {
        return dataManager;
    }

    public PlayerDataService getPlayerDataService() {
        return playerDataService;
    }

    public CooldownManager getCooldownManager() {
        return cooldownManager;
    }

    public ScoreboardManager getScoreboardManager() {
        return scoreboardManager;
    }

    public SkillManager getSkillManager() {
        return skillManager;
    }

    public TeamManager getTeamManager() {
        return teamManager;
    }

    public ShopManager getShopManager() {
        return shopManager;
    }

    public CoreGameManager getCoreGameManager() {
        return coreGameManager;
    }
}
