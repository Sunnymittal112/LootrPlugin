package com.fetal.lootr.listeners;

import com.fetal.lootr.LootrPlugin;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;

public class InventoryListener implements Listener {
    
    private final LootrPlugin plugin;
    
    public InventoryListener(LootrPlugin plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) return;
        
        Player player = (Player) event.getPlayer();
        Inventory inv = event.getInventory();
        
        if (event.getView().getTitle().contains("Loot Chest") || 
            event.getView().getTitle().contains("Custom Loot")) {
            
            player.playSound(player.getLocation(), Sound.BLOCK_CHEST_CLOSE, 1.0f, 1.0f);
            plugin.getChestListener().getOpenChests().remove(player.getUniqueId());
        }
    }
}