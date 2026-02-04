package com.fetal.lootr;

import com.fetal.lootr.commands.CustomChestCommand;
import com.fetal.lootr.commands.LootrCommand;
import com.fetal.lootr.compat.DatapackCompatibility;
import com.fetal.lootr.config.ConfigManager;
import com.fetal.lootr.listeners.BlockListener;
import com.fetal.lootr.listeners.ChestListener;
import com.fetal.lootr.listeners.InventoryListener;
import com.fetal.lootr.storage.StorageManager;
import org.bukkit.plugin.java.JavaPlugin;

public class LootrPlugin extends JavaPlugin {
    
    private static LootrPlugin instance;
    private ConfigManager configManager;
    private StorageManager storageManager;
    private DatapackCompatibility datapackCompat;
    private ChestListener chestListener;
    
    @Override
    public void onEnable() {
        instance = this;
        
        saveDefaultConfig();
        
        this.configManager = new ConfigManager(this);
        this.storageManager = new StorageManager(this);
        this.datapackCompat = new DatapackCompatibility(this);
        this.chestListener = new ChestListener(this);
        
        getServer().getPluginManager().registerEvents(chestListener, this);
        getServer().getPluginManager().registerEvents(new BlockListener(this), this);
        getServer().getPluginManager().registerEvents(new InventoryListener(this), this);
        
        getCommand("lootr").setExecutor(new LootrCommand(this));
        getCommand("customchest").setExecutor(new CustomChestCommand(this));
        getCommand("lootrdebug").setExecutor((sender, command, label, args) -> {
            if (!sender.hasPermission("lootr.admin")) return false;
            datapackCompat.debugLootTables(sender);
            return true;
        });
        
        // Simple console message
        getLogger().info("========================================");
        getLogger().info("       Lootr Plugin Enabled!");
        getLogger().info("       Version: " + getDescription().getVersion());
        getLogger().info("       Author: FeTaL                    ");
        getLogger().info("========================================");
    }
    
    @Override
    public void onDisable() {
        if (storageManager != null) storageManager.saveAll();
        getLogger().info("Lootr Plugin Disabled!");
    }
    
    public static LootrPlugin getInstance() { return instance; }
    public ConfigManager getConfigManager() { return configManager; }
    public StorageManager getStorageManager() { return storageManager; }
    public DatapackCompatibility getDatapackCompat() { return datapackCompat; }
    public ChestListener getChestListener() { return chestListener; }
    
    public void reload() {
        reloadConfig();
        configManager.reload();
        datapackCompat.scanDatapackTables();
    }
}