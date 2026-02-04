package com.fetal.lootr.listeners;

import com.fetal.lootr.LootrPlugin;
import com.fetal.lootr.utils.MessageUtils;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.inventory.InventoryType;

public class BlockListener implements Listener {
    
    private final LootrPlugin plugin;
    
    public BlockListener(LootrPlugin plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        Location loc = block.getLocation();
        
        if (!plugin.getStorageManager().isLootrChest(loc)) return;
        
        Player player = event.getPlayer();
        
        if (player.hasPermission("lootr.bypass")) return;
        if (!plugin.getConfigManager().preventBreak()) return;
        
        // Check sneak + permission
        if (player.isSneaking()) {
            if (!plugin.getConfigManager().requirePermissionToBreak() || 
                player.hasPermission("lootr.break")) {
                
                plugin.getStorageManager().removeChest(loc);
                player.sendMessage("§c[Lootr] Chest removed!");
                return;
            }
        }
        
        event.setCancelled(true);
        MessageUtils.send(player, plugin.getConfigManager().getBreakWarning());
    }
    
    @EventHandler
    public void onHopper(InventoryMoveItemEvent event) {
        if (!plugin.getConfigManager().preventHopper()) return;
        
        // Check if source is Lootr chest
        Location loc = event.getSource().getLocation();
        if (loc != null && plugin.getStorageManager().isLootrChest(loc)) {
            event.setCancelled(true);
        }
    }
    
    @EventHandler
    public void onExplosion(BlockExplodeEvent event) {
        if (!plugin.getConfigManager().preventExplosion()) return;
        
        event.blockList().removeIf(block -> 
            plugin.getStorageManager().isLootrChest(block.getLocation())
        );
    }
    
    @EventHandler
    public void onEntityExplosion(EntityExplodeEvent event) {
        if (!plugin.getConfigManager().preventExplosion()) return;
        
        event.blockList().removeIf(block -> 
            plugin.getStorageManager().isLootrChest(block.getLocation())
        );
    }
}