package com.fetal.lootr.storage;

import com.fetal.lootr.LootrPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Location;

import java.io.*;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class StorageManager {
    
    private final LootrPlugin plugin;
    private final File dataFolder;
    private final Map<Location, ChestData> chestDataMap;
    
    public StorageManager(LootrPlugin plugin) {
        this.plugin = plugin;
        this.dataFolder = new File(plugin.getDataFolder(), "data");
        this.chestDataMap = new ConcurrentHashMap<>();
        
        if (!dataFolder.exists()) dataFolder.mkdirs();
        
        loadAll();
        
        // Auto-save every 5 minutes
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, this::saveAll, 6000L, 6000L);
    }
    
    public void loadAll() {
        File[] files = dataFolder.listFiles((dir, name) -> name.endsWith(".dat"));
        if (files == null) return;
        
        for (File file : files) {
            try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
                ChestData data = (ChestData) ois.readObject();
                chestDataMap.put(data.getLocation(), data);
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to load " + file.getName());
            }
        }
        
        plugin.getLogger().info("Loaded " + chestDataMap.size() + " Lootr chests");
    }
    
    public void saveAll() {
        chestDataMap.forEach(this::saveChest);
    }
    
    public void saveChest(Location loc, ChestData data) {
        File file = new File(dataFolder, locToFileName(loc) + ".dat");
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(file))) {
            oos.writeObject(data);
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to save chest at " + loc);
        }
    }
    
    public ChestData getChestData(Location loc) {
        return chestDataMap.get(loc);
    }
    
    public void setChestData(Location loc, ChestData data) {
        chestDataMap.put(loc, data);
    }
    
    public void removeChest(Location loc) {
        chestDataMap.remove(loc);
        File file = new File(dataFolder, locToFileName(loc) + ".dat");
        if (file.exists()) file.delete();
    }
    
    public boolean isLootrChest(Location loc) {
        return chestDataMap.containsKey(loc);
    }
    
    private String locToFileName(Location loc) {
        return loc.getWorld().getName() + "_" + loc.getBlockX() + "_" + loc.getBlockY() + "_" + loc.getBlockZ();
    }
    
    // Serializable data class
    public static class ChestData implements Serializable {
        private static final long serialVersionUID = 1L;
        
        private final String world;
        private final int x, y, z;
        private final String lootTable;
        private final long created;
        private final Set<UUID> openedBy;
        private long firstOpened;
        private boolean decayed;
        private long lastRefresh;
        private boolean isCustom;
        private String customLootData;
        
        public ChestData(Location loc, String lootTable) {
            this.world = loc.getWorld().getName();
            this.x = loc.getBlockX();
            this.y = loc.getBlockY();
            this.z = loc.getBlockZ();
            this.lootTable = lootTable;
            this.created = System.currentTimeMillis();
            this.openedBy = ConcurrentHashMap.newKeySet();
            this.firstOpened = 0;
            this.decayed = false;
            this.lastRefresh = 0;
            this.isCustom = false;
            this.customLootData = null;
        }
        
        public Location getLocation() {
            return new Location(Bukkit.getWorld(world), x, y, z);
        }
        
        public boolean hasOpened(UUID player) {
            return openedBy.contains(player);
        }
        
        public void markOpened(UUID player) {
            openedBy.add(player);
            if (firstOpened == 0) firstOpened = System.currentTimeMillis();
        }
        
        // Getters and setters
        public String getLootTable() { return lootTable; }
        public boolean isDecayed() { return decayed; }
        public void setDecayed(boolean decayed) { this.decayed = decayed; }
        public long getFirstOpened() { return firstOpened; }
        public long getLastRefresh() { return lastRefresh; }
        public void setLastRefresh(long time) { this.lastRefresh = time; }
        public boolean isCustom() { return isCustom; }
        public void setCustom(boolean custom) { isCustom = custom; }
        public String getCustomLootData() { return customLootData; }
        public void setCustomLootData(String data) { this.customLootData = data; }
    }
}