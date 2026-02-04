package com.fetal.lootr.api;

import com.fetal.lootr.LootrPlugin;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.UUID;

public class LootrAPI {
    
    private static LootrPlugin plugin;
    
    public static void init(LootrPlugin pl) {
        plugin = pl;
    }
    
    /**
     * Check if location is a Lootr chest
     */
    public static boolean isLootrChest(Location loc) {
        return plugin.getStorageManager().isLootrChest(loc);
    }
    
    /**
     * Check if player has opened this chest
     */
    public static boolean hasPlayerOpened(Location loc, UUID player) {
        var data = plugin.getStorageManager().getChestData(loc);
        return data != null && data.hasOpened(player);
    }
    
    /**
     * Force open Lootr chest for player
     */
    public static void forceOpen(Player player, Location loc) {
        // Implementation for API
    }
}