package com.magmaguy.elitemobs;

/*
 * Created by MagmaGuy on 07/10/2016.
 */

import com.magmaguy.elitemobs.commands.CommandHandler;
import com.magmaguy.elitemobs.config.*;
import com.magmaguy.elitemobs.config.custombosses.CustomBossesConfig;
import com.magmaguy.elitemobs.config.customloot.CustomLootConfig;
import com.magmaguy.elitemobs.config.enchantments.EnchantmentsConfig;
import com.magmaguy.elitemobs.config.menus.MenusConfig;
import com.magmaguy.elitemobs.config.mobproperties.MobPropertiesConfig;
import com.magmaguy.elitemobs.config.npcs.NPCsConfig;
import com.magmaguy.elitemobs.config.powers.PowersConfig;
import com.magmaguy.elitemobs.economy.VaultCompatibility;
import com.magmaguy.elitemobs.events.EventLauncher;
import com.magmaguy.elitemobs.items.customenchantments.CustomEnchantmentCache;
import com.magmaguy.elitemobs.items.customitems.CustomItem;
import com.magmaguy.elitemobs.mobconstructor.CombatSystem;
import com.magmaguy.elitemobs.mobconstructor.mobdata.PluginMobProperties;
import com.magmaguy.elitemobs.mobscanner.SuperMobScanner;
import com.magmaguy.elitemobs.npcs.NPCInitializer;
import com.magmaguy.elitemobs.playerdata.PlayerData;
import com.magmaguy.elitemobs.powerstances.MajorPowerStanceMath;
import com.magmaguy.elitemobs.powerstances.MinorPowerStanceMath;
import com.magmaguy.elitemobs.quests.QuestRefresher;
import com.magmaguy.elitemobs.runnables.EggRunnable;
import com.magmaguy.elitemobs.runnables.EntityScanner;
import com.magmaguy.elitemobs.runnables.PotionEffectApplier;
import com.magmaguy.elitemobs.runnables.ScoreboardUpdater;
import com.magmaguy.elitemobs.utils.NonSolidBlockTypes;
import com.magmaguy.elitemobs.versionnotifier.VersionChecker;
import com.magmaguy.elitemobs.versionnotifier.VersionWarner;
import com.magmaguy.elitemobs.worlds.CustomWorldLoading;
import org.bstats.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.EntityType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

public class EliteMobs extends JavaPlugin {

    public static List<World> validWorldList = new ArrayList();
    public static boolean WORLDGUARD_IS_ENABLED = false;

    @Override
    public void onEnable() {

        //Enable stats
        Metrics metrics = new Metrics(this);

        //Initialize custom enchantments
        CustomEnchantmentCache.initialize();

        //Load loot from config
        ConfigValues.initializeConfigurations();
        ConfigValues.initializeCachedConfigurations();


        /*
        New config loading
         */
        initializeConfigs();

        if (WORLDGUARD_IS_ENABLED)
            Bukkit.getLogger().warning("[EliteMobs] WorldGuard 兼容已启用!");
        else
            Bukkit.getLogger().warning("[EliteMobs] WorldGuard 兼容未启用!");

        //Enable Vault
        try {
            VaultCompatibility.vaultSetup();
        } catch (Exception e) {
            Bukkit.getLogger().warning("[EliteMobs] 你的 Vault 配置可能出了问题 - Vault " +
                    "版本大概不兼容这个版本的 EliteMobs. 请与开发者反馈这个问题.");
            VaultCompatibility.VAULT_ENABLED = false;
        }

        //Hook up all listeners, some depend on config
        EventsRegistrer.registerEvents();

        //Launch the local data cache
        PlayerData.initializePlayerData();
        PlayerData.synchronizeDatabases();

        //Get world list
        worldScanner();

        //Start the repeating tasks such as scanners
        launchRunnables();

        //Commands
        this.getCommand("elitemobs").setExecutor(new CommandHandler());

        //launch events
        EventLauncher eventLauncher = new EventLauncher();
        eventLauncher.eventRepeatingTask();

        //launch internal clock for attack cooldown
        CombatSystem.launchInternalClock();

        /*
        Initialize mob values
         */
        PluginMobProperties.initializePluginMobValues();

        /*
        Cache animation vectors
         */
        MinorPowerStanceMath.initializeVectorCache();
        MajorPowerStanceMath.initializeVectorCache();

        /*
        Scan for loaded SuperMobs
         */
        SuperMobScanner.scanSuperMobs();

        /*
        Initialize NPCs
         */
        new NPCInitializer();

        /*
        Make sure entities are getting culled - necessary due to some plugins on some servers
         */
        EntityTracker.entityValidator();

        /*
        Check for new plugin version
         */
        VersionChecker.updateComparer();
        if (!VersionChecker.pluginIsUpToDate)
            this.getServer().getPluginManager().registerEvents(new VersionWarner(), this);

        /*
        Initialize anticheat block values
         */
        NonSolidBlockTypes.initializeNonSolidBlocks();

        /*
        Launch quests
         */
        QuestRefresher.generateNewQuestMenus();

        /*
        Load plugin worlds
         */
        CustomWorldLoading.startupWorldInitialization();

    }

    @Override
    public void onLoad() {
        //WorldGuard hook
        try {
            WORLDGUARD_IS_ENABLED = WorldGuardCompatibility.initialize();
        } catch (NoClassDefFoundError | IllegalStateException ex) {
            Bukkit.getLogger().warning("[EliteMobs] 进行 WorldGuard 兼容时出现了问题. EliteMobs 指定的 flags 将不会生效." +
                    " 除非你刚刚重载了插件, 在这种情况下他们将正常工作。");
            WORLDGUARD_IS_ENABLED = false;
        }

    }

    @Override
    public void onDisable() {

        Bukkit.getServer().getScheduler().cancelTasks(MetadataHandler.PLUGIN);

        EntityTracker.shutdownPurger();

        validWorldList.clear();

        //save cached data
        Bukkit.getScheduler().cancelTask(PlayerData.databaseSyncTaskID);
        Bukkit.getLogger().info("[EliteMobs] 正在保存 Elitemobs 数据库...");
        PlayerData.saveDatabases();
        Bukkit.getLogger().info("[EliteMobs] 保存完毕, 再见!");
        PlayerData.clearPlayerData();

    }

    public static void initializeConfigs() {
        EnchantmentsConfig.initializeConfigs();
        CustomEnchantmentsConfig.initializeConfig();
        AntiExploitConfig.initializeConfig();
        CombatTagConfig.initializeConfig();
        GuildRankData.initializeConfig();
        PlayerMoneyData.initializeConfig();
        CustomBossesConfig.initializeConfigs();
        CustomLootConfig.initializeConfigs();
        CustomItem.initializeCustomItems();
        AntiExploitConfig.initializeConfig();
        AdventurersGuildConfig.initializeConfig();
        ValidWorldsConfig.initializeConfig();
        ValidMobsConfig.initializeConfig();
        NPCsConfig.initializeConfigs();
        MenusConfig.initializeConfigs();
        PowersConfig.initializeConfigs();
        MobPropertiesConfig.initializeConfigs();
    }

    public static void worldScanner() {
        for (World world : Bukkit.getWorlds())
            if (ValidWorldsConfig.getBoolean("Valid worlds." + world.getName()))
                validWorldList.add(world);
    }

    /*
    Repeating tasks that run as long as the server is on
     */
    public void launchRunnables() {
        int eggTimerInterval = 20 * 60 * 10 / ConfigValues.defaultConfig.getInt(DefaultConfig.SUPERMOB_STACK_AMOUNT);

        new EntityScanner().runTaskTimer(this, 20, 20 * 10);
        new PotionEffectApplier().runTaskTimer(this, 20, 20 * 5);
        if (ConfigValues.defaultConfig.getBoolean(DefaultConfig.ENABLE_POWER_SCOREBOARDS))
            new ScoreboardUpdater().runTaskTimer(this, 20, 20);
        if (MobPropertiesConfig.getMobProperties().get(EntityType.CHICKEN).isEnabled() &&
                ConfigValues.defaultConfig.getInt(DefaultConfig.SUPERMOB_STACK_AMOUNT) > 0)
            new EggRunnable().runTaskTimer(this, eggTimerInterval, eggTimerInterval);
    }

}
