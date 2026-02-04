package com.fetal.lootr.storage;

import org.bukkit.inventory.ItemStack;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Stores per-player loot data for custom chests
 */
public class PlayerLootData implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    // Store custom loot for specific chests (if needed)
    private final Map<String, ChestLoot> chestLootMap;
    
    public PlayerLootData() {
        this.chestLootMap = new HashMap<>();
    }
    
    /**
     * Store custom loot for a specific chest
     */
    public void setChestLoot(String chestId, ItemStack[] items) {
        chestLootMap.put(chestId, new ChestLoot(items, System.currentTimeMillis()));
    }
    
    /**
     * Get stored loot for a chest
     */
    public ItemStack[] getChestLoot(String chestId) {
        ChestLoot loot = chestLootMap.get(chestId);
        return loot != null ? loot.getItems() : null;
    }
    
    /**
     * Check if player has loot for this chest
     */
    public boolean hasLoot(String chestId) {
        return chestLootMap.containsKey(chestId);
    }
    
    /**
     * Remove chest loot data
     */
    public void removeChestLoot(String chestId) {
        chestLootMap.remove(chestId);
    }
    
    /**
     * Inner class to store loot with timestamp
     */
    private static class ChestLoot implements Serializable {
        private static final long serialVersionUID = 1L;
        
        private final ItemStack[] items;
        private final long timestamp;
        
        public ChestLoot(ItemStack[] items, long timestamp) {
            this.items = items;
            this.timestamp = timestamp;
        }
        
        public ItemStack[] getItems() {
            return items;
        }
        
        public long getTimestamp() {
            return timestamp;
        }
    }
}