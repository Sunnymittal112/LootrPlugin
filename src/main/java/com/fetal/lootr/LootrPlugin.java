package com.fetal.lootr;

import com.fetal.lootr.commands.LootrCommand;
import com.fetal.lootr.listeners.ChestListener;
import com.fetal.lootr.listeners.ChestProtectionListener;
import com.fetal.lootr.manager.LootManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class LootrPlugin extends JavaPlugin {

    private static LootrPlugin instance;
    private LootManager lootManager;
    private BukkitTask autoSaveTask;
    private BukkitTask particleTask;

    // Config values
    private String prefix;
    private boolean debug;
    private boolean perPlayerLoot;
    private int maxTrackedChests;
    private List<String> disabledWorlds;
    private String inventoryTitle;
    private boolean playSound;
    private boolean preventBreak;
    private boolean preventHopper;
    private boolean preventExplosion;
    private boolean preventPiston;
    private int breakConfirmTime;
    private boolean bypassProtection;
    private boolean particlesEnabled;
    private String particleType;
    private boolean particlesOnlyUnlooted;
    private int autoSaveInterval;

    // Compatibility config values
    private boolean ignoreQuickShop;
    private boolean ignoreMetadataChests;
    private List<String> metadataBlacklist;
    private List<String> ignoreChestNames;
    private boolean excludeShopProtection;
    private boolean particlesExcludeShops;
    private int waitForQuickShop;

    // Messages
    private String msgFirstOpen;
    private String msgAlreadyLooted;
    private String msgCannotBreak;
    private String msgBreakConfirm;
    private String msgChestBroken;
    private String msgNoPermission;
    private String msgShopIgnored;

    // Stats
    private int sessionChestsRegistered = 0;
    private int sessionLootsGenerated = 0;
    private long startTime;

    @Override
    public void onEnable() {
        instance = this;
        startTime = System.currentTimeMillis();

        // Create folders
        if (!getDataFolder().exists()) {
            getDataFolder().mkdirs();
        }
        new File(getDataFolder(), "playerdata").mkdirs();

        // Save default config
        saveDefaultConfig();

        // Load config
        loadConfigValues();

        // Wait for QuickShop if configured
        if (waitForQuickShop > 0 && !Bukkit.getPluginManager().isPluginEnabled("QuickShop-Hikari")) {
            getLogger().info("Waiting for QuickShop-Hikari to load...");
            Bukkit.getScheduler().runTaskLater(this, () -> {
                initializePlugin();
            }, 20L * waitForQuickShop);
        } else {
            initializePlugin();
        }
    }

    private void initializePlugin() {
        // Check for shop plugins
        checkShopPlugins();

        // Initialize manager
        lootManager = new LootManager(this);
        lootManager.loadData();

        // Register listeners
        getServer().getPluginManager().registerEvents(new ChestListener(this, lootManager), this);
        getServer().getPluginManager().registerEvents(new ChestProtectionListener(this, lootManager), this);

        // Register commands
        PluginCommand cmd = getCommand("lootr");
        if (cmd != null) {
            LootrCommand executor = new LootrCommand(this, lootManager);
            cmd.setExecutor(executor);
            cmd.setTabCompleter(executor);
        }

        // Start tasks
        startTasks();

        // Banner
        printBanner();

        getLogger().info("LootrPlugin enabled! Author: FeTaL");
    }

    private void checkShopPlugins() {
        if (Bukkit.getPluginManager().isPluginEnabled("QuickShop-Hikari")) {
            getLogger().info("§aQuickShop-Hikari detected! Compatibility mode enabled.");
        }
        if (Bukkit.getPluginManager().isPluginEnabled("ChestShop")) {
            getLogger().info("§aChestShop detected! Will ignore shop chests.");
        }
        if (Bukkit.getPluginManager().isPluginEnabled("ShopChest")) {
            getLogger().info("§aShopChest detected! Will ignore shop chests.");
        }
    }

    @Override
    public void onDisable() {
        if (autoSaveTask != null) autoSaveTask.cancel();
        if (particleTask != null) particleTask.cancel();

        if (lootManager != null) {
            lootManager.saveAllData();
            getLogger().info("Data saved!");
        }

        getLogger().info("LootrPlugin disabled!");
        instance = null;
    }

    public void loadConfigValues() {
        reloadConfig();
        FileConfiguration config = getConfig();

        prefix = color(config.getString("general.prefix", "&6[&eLootr&6] &r"));
        debug = config.getBoolean("general.debug", false);

        // Compatibility settings
        ignoreQuickShop = config.getBoolean("compatibility.ignore-quickshop", true);
        ignoreMetadataChests = config.getBoolean("compatibility.ignore-metadata-chests", false);
        metadataBlacklist = config.getStringList("compatibility.metadata-blacklist");
        ignoreChestNames = config.getStringList("compatibility.ignore-chest-names-containing");
        waitForQuickShop = config.getInt("compatibility.wait-for-quickshop", 5);
        excludeShopProtection = config.getBoolean("protection.exclude-shop-protection", true);
        particlesExcludeShops = config.getBoolean("visuals.particles-exclude-shops", true);

        perPlayerLoot = config.getBoolean("loot.per-player-loot", true);
        maxTrackedChests = config.getInt("loot.max-tracked-chests", 50000);
        disabledWorlds = config.getStringList("loot.disabled-worlds");
        if (disabledWorlds == null) disabledWorlds = new ArrayList<>();

        inventoryTitle = color(config.getString("inventory.title", "&8[&6✦&8] &eLoot Chest"));
        playSound = config.getBoolean("inventory.play-sound", true);

        preventBreak = config.getBoolean("protection.prevent-break", true);
        preventHopper = config.getBoolean("protection.prevent-hopper", true);
        preventExplosion = config.getBoolean("protection.prevent-explosion", true);
        preventPiston = config.getBoolean("protection.prevent-piston", true);
        breakConfirmTime = config.getInt("protection.break-confirm-time", 3);
        bypassProtection = config.getBoolean("protection.bypass-protection", true);

        particlesEnabled = config.getBoolean("visuals.particles-enabled", true);
        particleType = config.getString("visuals.particle-type", "VILLAGER_HAPPY");
        particlesOnlyUnlooted = config.getBoolean("visuals.particles-only-unlooted", true);

        autoSaveInterval = config.getInt("storage.auto-save-interval", 5);
        if (autoSaveInterval < 1) autoSaveInterval = 1;

        msgFirstOpen = color(config.getString("messages.first-open", "&aThis chest has unique loot just for you!"));
        msgAlreadyLooted = color(config.getString("messages.already-looted", "&7You have already looted this chest."));
        msgCannotBreak = color(config.getString("messages.cannot-break", "&cYou cannot break this chest!"));
        msgBreakConfirm = color(config.getString("messages.break-confirm", "&eBreak again within 3 seconds to confirm!"));
        msgChestBroken = color(config.getString("messages.chest-broken", "&aLootr chest removed!"));
        msgNoPermission = color(config.getString("messages.no-permission", "&cYou don't have permission!"));
        msgShopIgnored = color(config.getString("messages.shop-ignored", "&7This is a shop chest."));

        getLogger().info("Config loaded!");
    }

    private void startTasks() {
        long saveInterval = 20L * 60 * autoSaveInterval;
        autoSaveTask = Bukkit.getScheduler().runTaskTimerAsynchronously(this, () -> {
            lootManager.saveAllData();
            debug("Auto-saved data");
        }, saveInterval, saveInterval);

        if (particlesEnabled) {
            particleTask = Bukkit.getScheduler().runTaskTimer(this, () -> {
                lootManager.spawnParticles();
            }, 20L, 40L);
        }

        getLogger().info("Tasks started!");
    }

    public void reloadPlugin() {
        lootManager.saveAllData();
        if (autoSaveTask != null) autoSaveTask.cancel();
        if (particleTask != null) particleTask.cancel();

        loadConfigValues();
        lootManager.loadData();
        startTasks();

        getLogger().info("Plugin reloaded!");
    }

    private void printBanner() {
        getLogger().info("╔═══════════════════════════════════════╗");
        getLogger().info("║          LootrPlugin v1.2.4" + getDescription().getVersion() + "      ║");
        getLogger().info("║            Author: FeTaL              ║");
        getLogger().info("╠═══════════════════════════════════════╣");
        getLogger().info("║                                       ║");
        getLogger().info("║  Per-Player: " + perPlayerLoot);      
        getLogger().info("║  QuickShop Mode: " + ignoreQuickShop);
        getLogger().info("╚═══════════════════════════════════════╝");
    }

    public String color(String text) {
        if (text == null) return "";
        return ChatColor.translateAlternateColorCodes('&', text);
    }

    public void debug(String msg) {
        if (debug) getLogger().info("[DEBUG] " + msg);
    }

    public void incrementChestsRegistered() { sessionChestsRegistered++; }
    public void incrementLootsGenerated() { sessionLootsGenerated++; }
    public int getSessionChestsRegistered() { return sessionChestsRegistered; }
    public int getSessionLootsGenerated() { return sessionLootsGenerated; }

    public String getFormattedUptime() {
        long ms = System.currentTimeMillis() - startTime;
        long sec = ms / 1000;
        long min = sec / 60;
        long hr = min / 60;
        if (hr > 0) return hr + "h " + (min % 60) + "m";
        if (min > 0) return min + "m " + (sec % 60) + "s";
        return sec + "s";
    }

    // Getters
    public static LootrPlugin getInstance() { return instance; }
    public LootManager getLootManager() { return lootManager; }
    public String getPrefix() { return prefix; }
    public boolean isDebug() { return debug; }
    public boolean isPerPlayerLoot() { return perPlayerLoot; }
    public int getMaxTrackedChests() { return maxTrackedChests; }
    public boolean isWorldDisabled(String world) { return disabledWorlds.contains(world); }
    public String getInventoryTitle() { return inventoryTitle; }
    public boolean isPlaySound() { return playSound; }
    public boolean isPreventBreak() { return preventBreak; }
    public boolean isPreventHopper() { return preventHopper; }
    public boolean isPreventExplosion() { return preventExplosion; }
    public boolean isPreventPiston() { return preventPiston; }
    public int getBreakConfirmTime() { return breakConfirmTime; }
    public boolean isBypassProtection() { return bypassProtection; }
    public boolean isParticlesEnabled() { return particlesEnabled; }
    public String getParticleType() { return particleType; }
    public boolean isParticlesOnlyUnlooted() { return particlesOnlyUnlooted; }
    public String getMsgFirstOpen() { return msgFirstOpen; }
    public String getMsgAlreadyLooted() { return msgAlreadyLooted; }
    public String getMsgCannotBreak() { return msgCannotBreak; }
    public String getMsgBreakConfirm() { return msgBreakConfirm; }
    public String getMsgChestBroken() { return msgChestBroken; }
    public String getMsgNoPermission() { return msgNoPermission; }
    public String getMsgShopIgnored() { return msgShopIgnored; }
    
    // Compatibility getters
    public boolean isIgnoreQuickShop() { return ignoreQuickShop; }
    public boolean isIgnoreMetadataChests() { return ignoreMetadataChests; }
    public List<String> getMetadataBlacklist() { return metadataBlacklist; }
    public List<String> getIgnoreChestNames() { return ignoreChestNames; }
    public boolean isExcludeShopProtection() { return excludeShopProtection; }
    public boolean isParticlesExcludeShops() { return particlesExcludeShops; }
}
