package com.fetal.lootr.manager;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.block.DoubleChest;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import com.fetal.lootr.LootrHolder;
import com.fetal.lootr.LootrPlugin;

public class LootManager {

    private final LootrPlugin plugin;
    private final Map<String, ChestData> registeredChests = new ConcurrentHashMap<>();
    private final Map<String, Map<UUID, ItemStack[]>> playerLoot = new ConcurrentHashMap<>();

    public LootManager(LootrPlugin plugin) {
        this.plugin = plugin;
    }

    // Location helpers
    public String toKey(Location loc) {
        if (loc == null || loc.getWorld() == null) return "unknown,0,0,0";

        Location normalized = normalizeChestLocation(loc);
        return formatLocationKey(normalized);
    }

    private String formatLocationKey(Location loc) {
        return loc.getWorld().getName() + "," + loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ();
    }

    private Location normalizeChestLocation(Location loc) {
        Block block = loc.getBlock();
        if (!(block.getState() instanceof Chest)) return loc;

        Chest chest = (Chest) block.getState();
        InventoryHolder holder = chest.getInventory().getHolder();
        if (!(holder instanceof DoubleChest)) return loc;

        DoubleChest doubleChest = (DoubleChest) holder;
        InventoryHolder left = doubleChest.getLeftSide();
        InventoryHolder right = doubleChest.getRightSide();
        if (!(left instanceof Chest) || !(right instanceof Chest)) return loc;

        Location leftLoc = ((Chest) left).getLocation();
        Location rightLoc = ((Chest) right).getLocation();
        return compareLocations(leftLoc, rightLoc) <= 0 ? leftLoc : rightLoc;
    }

    private int compareLocations(Location a, Location b) {
        int worldCmp = a.getWorld().getName().compareTo(b.getWorld().getName());
        if (worldCmp != 0) return worldCmp;

        if (a.getBlockX() != b.getBlockX()) return Integer.compare(a.getBlockX(), b.getBlockX());
        if (a.getBlockY() != b.getBlockY()) return Integer.compare(a.getBlockY(), b.getBlockY());
        return Integer.compare(a.getBlockZ(), b.getBlockZ());
    }

    private Location toLocation(String key) {
        try {
            String[] p = key.split(",");
            World world = Bukkit.getWorld(p[0]);
            if (world == null) return null;
            return new Location(world, Integer.parseInt(p[1]), Integer.parseInt(p[2]), Integer.parseInt(p[3]));
        } catch (Exception e) {
            return null;
        }
    }

    // Registration
    public void registerChest(String key, String lootTableKey, long seed, int invSize) {
        registeredChests.put(key, new ChestData(lootTableKey, seed, invSize));
    }

    public void unregisterChest(String key) {
        registeredChests.remove(key);
        playerLoot.remove(key);
    }

    public boolean isRegistered(String key) {
        return registeredChests.containsKey(key);
    }

    // Per-player loot
    public Inventory getOrCreateLoot(String chestKey, Player player) {
        ChestData data = registeredChests.get(chestKey);
        if (data == null) return null;

        UUID lookupId = plugin.isPerPlayerLoot()
                ? player.getUniqueId()
                : UUID.fromString("00000000-0000-0000-0000-000000000000");

        LootrHolder holder = new LootrHolder(chestKey, player.getUniqueId());
        Inventory inv = Bukkit.createInventory(holder, data.invSize, plugin.getInventoryTitle());
        holder.setInventory(inv);

        Map<UUID, ItemStack[]> chestMap = playerLoot.get(chestKey);

        if (chestMap != null && chestMap.containsKey(lookupId)) {
            ItemStack[] saved = chestMap.get(lookupId);
            ItemStack[] contents = new ItemStack[data.invSize];
            for (int i = 0; i < Math.min(saved.length, data.invSize); i++) {
                contents[i] = saved[i] != null ? saved[i].clone() : null;
            }
            inv.setContents(contents);
        } else {
            generateLoot(inv, data, chestKey, player);
            savePlayerLoot(chestKey, lookupId, inv.getContents());
        }

        return inv;
    }

    private boolean generateLoot(Inventory inv, ChestData data, String chestKey, Player player) {
        try {
            // For now, just leave inventory empty - loot tables require deeper NMS integration
            // This prevents breaking the plugin while maintaining plugin state
            return true;
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to generate loot for " + chestKey + ": " + e.getMessage());
            return true; // Still allow chest to open
        }
    }

    public void savePlayerLoot(String chestKey, UUID playerUUID, ItemStack[] contents) {
        playerLoot.computeIfAbsent(chestKey, k -> new ConcurrentHashMap<>());
        ItemStack[] cloned = new ItemStack[contents.length];
        for (int i = 0; i < contents.length; i++) {
            cloned[i] = contents[i] != null ? contents[i].clone() : null;
        }
        playerLoot.get(chestKey).put(playerUUID, cloned);
    }

    public boolean hasPlayerLooted(String chestKey, UUID playerUUID) {
        Map<UUID, ItemStack[]> map = playerLoot.get(chestKey);
        return map != null && map.containsKey(playerUUID);
    }

    // Stats
    public int getRegisteredChestCount() {
        return registeredChests.size();
    }

    public int getTotalPlayerEntries() {
        int count = 0;
        for (Map<UUID, ItemStack[]> map : playerLoot.values()) {
            count += map.size();
        }
        return count;
    }

    public int getPlayersLootedCount(String chestKey) {
        Map<UUID, ItemStack[]> map = playerLoot.get(chestKey);
        return map != null ? map.size() : 0;
    }

    public String getChestLootTable(String chestKey) {
        ChestData data = registeredChests.get(chestKey);
        return data != null ? data.lootTableKey : "Unknown";
    }

    public Set<String> getAllChestKeys() {
        return new HashSet<>(registeredChests.keySet());
    }

    // Clear/Reset
    public int clearAllPlayerData() {
        int count = getTotalPlayerEntries();
        playerLoot.clear();
        return count;
    }

    public int clearPlayerData(UUID playerUUID) {
        int count = 0;
        for (Map<UUID, ItemStack[]> map : playerLoot.values()) {
            if (map.remove(playerUUID) != null) count++;
        }
        return count;
    }

    public int resetChest(String chestKey) {
        Map<UUID, ItemStack[]> map = playerLoot.remove(chestKey);
        return map != null ? map.size() : 0;
    }

    // Particles with shop exclusion
    public void spawnParticles() {
        Particle particleType;
        try {
            particleType = Particle.valueOf(plugin.getParticleType());
        } catch (Exception e) {
            particleType = Particle.HAPPY_VILLAGER;
        }

        for (String chestKey : registeredChests.keySet()) {
            Location loc = toLocation(chestKey);
            if (loc == null || loc.getWorld() == null) continue;

            if (!loc.getWorld().isChunkLoaded(loc.getBlockX() >> 4, loc.getBlockZ() >> 4)) continue;

            if (plugin.isParticlesExcludeShops()) {
                Block block = loc.getBlock();
                boolean isShop = false;

                for (String meta : plugin.getMetadataBlacklist()) {
                    if (block.hasMetadata(meta)) {
                        isShop = true;
                        break;
                    }
                }

                if (isShop) continue;
            }

            if (plugin.isParticlesOnlyUnlooted()) {
                boolean anyUnlooted = false;
                for (Player p : loc.getWorld().getPlayers()) {
                    if (p.getLocation().distanceSquared(loc) < 2500) {
                        if (!hasPlayerLooted(chestKey, p.getUniqueId())) {
                            anyUnlooted = true;
                            break;
                        }
                    }
                }
                if (!anyUnlooted) continue;
            }

            loc.getWorld().spawnParticle(particleType, loc.getBlockX() + 0.5, loc.getBlockY() + 1.2, loc.getBlockZ() + 0.5, 3, 0.3, 0.2, 0.3, 0);
        }
    }

    // Persistence - Load
    public void loadData() {
        registeredChests.clear();
        playerLoot.clear();

        File chestsFile = new File(plugin.getDataFolder(), "chests.yml");
        if (chestsFile.exists()) {
            YamlConfiguration cfg = YamlConfiguration.loadConfiguration(chestsFile);
            for (String yamlKey : cfg.getKeys(false)) {
                String chestKey = yamlKey.replace("|", ",");
                String lt = cfg.getString(yamlKey + ".lootTable");
                long seed = cfg.getLong(yamlKey + ".seed", 0);
                int size = cfg.getInt(yamlKey + ".size", 27);
                if (lt != null) registeredChests.put(chestKey, new ChestData(lt, seed, size));
            }
        }
        plugin.getLogger().info("Loaded " + registeredChests.size() + " chests.");

        File playerDir = new File(plugin.getDataFolder(), "playerdata");
        if (playerDir.exists() && playerDir.isDirectory()) {
            File[] files = playerDir.listFiles((d, n) -> n.endsWith(".yml"));
            if (files != null) {
                int count = 0;
                for (File file : files) {
                    String uuidStr = file.getName().replace(".yml", "");
                    UUID uuid;
                    try {
                        uuid = UUID.fromString(uuidStr);
                    } catch (Exception e) {
                        continue;
                    }
                    YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);
                    for (String yamlKey : cfg.getKeys(false)) {
                        String chestKey = yamlKey.replace("|", ",");
                        @SuppressWarnings("unchecked")
                        List<ItemStack> items = (List<ItemStack>) cfg.getList(yamlKey + ".items");
                        if (items != null) {
                            playerLoot.computeIfAbsent(chestKey, k -> new ConcurrentHashMap<>());
                            playerLoot.get(chestKey).put(uuid, items.toArray(new ItemStack[0]));
                            count++;
                        }
                    }
                }
                plugin.getLogger().info("Loaded " + count + " player entries.");
            }
        }
    }

    // Persistence - Save
    public synchronized void saveAllData() {
        plugin.getDataFolder().mkdirs();

        File chestsFile = new File(plugin.getDataFolder(), "chests.yml");
        YamlConfiguration chestsCfg = new YamlConfiguration();
        for (Map.Entry<String, ChestData> e : registeredChests.entrySet()) {
            String yamlKey = e.getKey().replace(",", "|");
            chestsCfg.set(yamlKey + ".lootTable", e.getValue().lootTableKey);
            chestsCfg.set(yamlKey + ".seed", e.getValue().seed);
            chestsCfg.set(yamlKey + ".size", e.getValue().invSize);
        }
        try {
            chestsCfg.save(chestsFile);
        } catch (IOException ex) {
            plugin.getLogger().severe("Failed to save chests.yml");
        }

        Map<UUID, Map<String, ItemStack[]>> byPlayer = new HashMap<>();
        for (Map.Entry<String, Map<UUID, ItemStack[]>> chestEntry : playerLoot.entrySet()) {
            for (Map.Entry<UUID, ItemStack[]> playerEntry : chestEntry.getValue().entrySet()) {
                byPlayer.computeIfAbsent(playerEntry.getKey(), k -> new HashMap<>());
                byPlayer.get(playerEntry.getKey()).put(chestEntry.getKey(), playerEntry.getValue());
            }
        }

        File playerDir = new File(plugin.getDataFolder(), "playerdata");
        playerDir.mkdirs();

        for (Map.Entry<UUID, Map<String, ItemStack[]>> e : byPlayer.entrySet()) {
            File file = new File(playerDir, e.getKey().toString() + ".yml");
            YamlConfiguration cfg = new YamlConfiguration();
            for (Map.Entry<String, ItemStack[]> ce : e.getValue().entrySet()) {
                cfg.set(ce.getKey().replace(",", "|") + ".items", Arrays.asList(ce.getValue()));
            }
            try {
                cfg.save(file);
            } catch (IOException ex) {
                plugin.getLogger().severe("Failed to save " + e.getKey());
            }
        }
    }

    private static class ChestData {
        final String lootTableKey;
        final long seed;
        final int invSize;

        ChestData(String lootTableKey, long seed, int invSize) {
            this.lootTableKey = lootTableKey;
            this.seed = seed;
            this.invSize = invSize;
        }
    }
}