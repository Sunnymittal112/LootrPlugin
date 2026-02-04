package com.fetal.lootr.config;

import com.fetal.lootr.LootrPlugin;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.List;

public class ConfigManager {
    
    private final LootrPlugin plugin;
    private FileConfiguration config;
    
    public ConfigManager(LootrPlugin plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfig();
    }
    
    public void reload() {
        plugin.reloadConfig();
        this.config = plugin.getConfig();
    }
    
    public boolean isDebug() { return config.getBoolean("settings.debug", false); }
    public boolean isAutoConvert() { return config.getBoolean("settings.auto-convert", true); }
    public boolean isParticlesEnabled() { return config.getBoolean("settings.particles", true); }
    public boolean isSoundsEnabled() { return config.getBoolean("settings.sounds", true); }
    public boolean useColoredBlocks() { return config.getBoolean("visual.use-colored-blocks", true); }
    public String getUnopenedParticle() { return config.getString("visual.unopened-particle", "GOLD"); }
    public String getOpenedParticle() { return config.getString("visual.opened-particle", "WATER"); }
    
    public boolean isWorldEnabled(String worldName) {
        List<String> enabled = config.getStringList("settings.enabled-worlds");
        return enabled.isEmpty() || enabled.contains(worldName);
    }
    
    public boolean isWorldBlacklisted(String worldName) {
        return config.getStringList("settings.blacklisted-worlds").contains(worldName);
    }
    
    public boolean isLootTableBlacklisted(String table) {
        return config.getStringList("loot.blacklisted-tables").contains(table);
    }
    
    public int getRefreshHours() { return config.getInt("loot.refresh-hours", 0); }
    public int getDecayHours() { return config.getInt("loot.decay-hours", 0); }
    public boolean preventBreak() { return config.getBoolean("protection.prevent-break", true); }
    public boolean preventHopper() { return config.getBoolean("protection.prevent-hopper", true); }
    public boolean preventExplosion() { return config.getBoolean("protection.prevent-explosion", true); }
    public boolean requirePermissionToBreak() { return config.getBoolean("protection.require-permission-to-break", false); }
    public String getBreakWarning() { return config.getString("protection.break-warning", "&c&l[!] &eThis chest is instanced per-player. Sneak + break to remove."); }
    public String getMessage(String path) { return config.getString("messages." + path, ""); }
    
    // Datapack settings
    public boolean isDatapackEnabled() { return config.getBoolean("datapack.enabled", true); }
    public boolean isDatapackAutoScan() { return config.getBoolean("datapack.auto-scan", true); }
    public boolean allowCustomNamespaces() { return config.getBoolean("datapack.allow-custom-namespaces", true); }
    public boolean isDebugTables() { return config.getBoolean("datapack.debug-tables", false); }
    public boolean isFallbackToVanilla() { return config.getBoolean("datapack.fallback-to-vanilla", true); }
}