package com.fetal.lootr.compat;

import com.fetal.lootr.LootrPlugin;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.command.CommandSender;
import org.bukkit.loot.LootTable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class DatapackCompatibility {
    
    private final LootrPlugin plugin;
    private final Map<String, LootTableInfo> knownTables;
    private boolean enabled;
    
    public DatapackCompatibility(LootrPlugin plugin) {
        this.plugin = plugin;
        this.knownTables = new HashMap<>();
        this.enabled = plugin.getConfigManager().isDatapackEnabled();
        
        if (enabled) {
            scanDatapackTables();
        }
    }
    
    public void scanDatapackTables() {
        knownTables.clear();
        
        // Vanilla Minecraft loot tables
        registerVanillaTables();
        
        // Scan for datapack tables
        scanCustomTables();
        
        if (plugin.getConfigManager().isDebugTables()) {
            plugin.getLogger().info("Found " + knownTables.size() + " loot tables:");
            knownTables.keySet().forEach(table -> plugin.getLogger().info("  - " + table));
        }
    }
    
    private void registerVanillaTables() {
        String[] vanillaTables = {
            "minecraft:chests/spawn_bonus_chest",
            "minecraft:chests/simple_dungeon",
            "minecraft:chests/abandoned_mineshaft",
            "minecraft:chests/bastion_bridge",
            "minecraft:chests/bastion_hoglin_stable",
            "minecraft:chests/bastion_other",
            "minecraft:chests/bastion_treasure",
            "minecraft:chests/buried_treasure",
            "minecraft:chests/desert_pyramid",
            "minecraft:chests/end_city_treasure",
            "minecraft:chests/igloo_chest",
            "minecraft:chests/jungle_temple",
            "minecraft:chests/jungle_temple_dispenser",
            "minecraft:chests/nether_bridge",
            "minecraft:chests/pillager_outpost",
            "minecraft:chests/ruined_portal",
            "minecraft:chests/shipwreck_map",
            "minecraft:chests/shipwreck_supply",
            "minecraft:chests/shipwreck_treasure",
            "minecraft:chests/stronghold_corridor",
            "minecraft:chests/stronghold_crossing",
            "minecraft:chests/stronghold_library",
            "minecraft:chests/underwater_ruin_big",
            "minecraft:chests/underwater_ruin_small",
            "minecraft:chests/woodland_mansion",
            "minecraft:chests/ancient_city",
            "minecraft:chests/ancient_city_ice_box",
            "minecraft:chests/trial_chambers/corridor",
            "minecraft:chests/trial_chambers/entrance_chest",
            "minecraft:chests/trial_chambers/intersection",
            "minecraft:chests/trial_chambers/intersection_barrel",
            "minecraft:chests/trial_chambers/reward",
            "minecraft:chests/trial_chambers/reward_ominous",
            "minecraft:chests/trial_chambers/supply"
        };
        
        for (String tableKey : vanillaTables) {
            registerTable(tableKey, "vanilla");
        }
    }
    
    private void scanCustomTables() {
        // Check for popular datapack namespaces
        String[] commonDatapackNamespaces = {
            "betterdungeons",
            "bettermineshafts",
            "dungeons_arise",
            "repurposed_structures",
            "mostructures",
            "dungeoncrawl",
            "additionalstructures",
            "structory",
            "explorify",
            "terralith",
            "incendium",
            "nullscape",
            "amplified_nether"
        };
        
        String[] commonPaths = {
            "chests/",
            "chests/dungeon/",
            "chests/structure/",
            "chests/tower/",
            "chests/house/",
            "chests/mansion/",
            "chests/temple/"
        };
        
        for (String namespace : commonDatapackNamespaces) {
            for (String path : commonPaths) {
                // Try common patterns
                for (int i = 1; i <= 20; i++) {
                    tryTable(namespace + ":" + path + "chest" + i);
                    tryTable(namespace + ":" + path + "loot" + i);
                }
                tryTable(namespace + ":" + path + "common");
                tryTable(namespace + ":" + path + "rare");
                tryTable(namespace + ":" + path + "epic");
            }
        }
    }
    
    private void tryTable(String key) {
        NamespacedKey namespacedKey = NamespacedKey.fromString(key);
        if (namespacedKey == null) return;
        
        LootTable table = Bukkit.getServer().getLootTable(namespacedKey);
        if (table != null) {
            registerTable(key, "datapack");
        }
    }
    
    public void registerTable(String key, String source) {
        NamespacedKey namespacedKey = NamespacedKey.fromString(key);
        if (namespacedKey == null) return;
        
        LootTable table = Bukkit.getServer().getLootTable(namespacedKey);
        if (table != null) {
            knownTables.put(key, new LootTableInfo(key, source, table));
        }
    }
    
    public boolean isValidLootTable(String key) {
        if (!enabled) return false;
        if (knownTables.containsKey(key)) return true;
        
        // Dynamic check for unknown tables
        NamespacedKey namespacedKey = NamespacedKey.fromString(key);
        if (namespacedKey == null) return false;
        
        LootTable table = Bukkit.getServer().getLootTable(namespacedKey);
        if (table != null) {
            // Auto-register if found
            String source = namespacedKey.getNamespace().equals("minecraft") ? "vanilla" : "datapack";
            registerTable(key, source);
            return true;
        }
        
        return false;
    }
    
    public LootTable getLootTable(String key) {
        LootTableInfo info = knownTables.get(key);
        if (info != null) return info.getTable();
        
        NamespacedKey namespacedKey = NamespacedKey.fromString(key);
        if (namespacedKey == null) return null;
        
        return Bukkit.getServer().getLootTable(namespacedKey);
    }
    
    public boolean isEnabled() {
        return enabled;
    }
    
    public void debugLootTables(CommandSender sender) {
        sender.sendMessage("§6§l=== Lootr Datapack Debug ===");
        sender.sendMessage("§eTotal tables registered: §f" + knownTables.size());
        sender.sendMessage("");
        
        Map<String, Integer> bySource = new HashMap<>();
        for (LootTableInfo info : knownTables.values()) {
            bySource.merge(info.getSource(), 1, Integer::sum);
        }
        
        sender.sendMessage("§7By Source:");
        bySource.forEach((source, count) -> 
            sender.sendMessage("  §7- §e" + source + ": §f" + count));
        
        sender.sendMessage("");
        sender.sendMessage("§7Sample tables:");
        knownTables.values().stream()
            .limit(10)
            .forEach(info -> sender.sendMessage("  §7- §f" + info.getKey()));
    }
    
    private static class LootTableInfo {
        private final String key;
        private final String source;
        private final LootTable table;
        
        public LootTableInfo(String key, String source, LootTable table) {
            this.key = key;
            this.source = source;
            this.table = table;
        }
        
        public String getKey() { return key; }
        public String getSource() { return source; }
        public LootTable getTable() { return table; }
    }
}