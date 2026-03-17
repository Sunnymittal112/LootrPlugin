package com.fetal.lootr.listeners;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Container;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.inventory.InventoryHolder;

import com.fetal.lootr.LootrPlugin;
import com.fetal.lootr.manager.LootManager;

public class ChestProtectionListener implements Listener {

    private final LootrPlugin plugin;
    private final LootManager lootManager;
    private final Map<String, Long> pendingBreaks = new HashMap<>();

    public ChestProtectionListener(LootrPlugin plugin, LootManager lootManager) {
        this.plugin = plugin;
        this.lootManager = lootManager;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        if (!plugin.isPreventBreak()) return;

        Block block = event.getBlock();
        String chestKey = lootManager.toKey(block.getLocation());

        if (!lootManager.isRegistered(chestKey)) return;

        // Check if shop protection should be excluded
        if (plugin.isExcludeShopProtection() && isShopChest(block)) {
            plugin.debug("Shop chest protection excluded: " + chestKey);
            return;
        }

        Player player = event.getPlayer();

        // No permission
        if (!player.hasPermission("lootr.break")) {
            event.setCancelled(true);
            player.sendMessage(plugin.getPrefix() + plugin.getMsgCannotBreak());
            return;
        }

        // Double-break confirmation
        String confirmKey = player.getUniqueId() + ":" + chestKey;
        long now = System.currentTimeMillis();
        int confirmMs = plugin.getBreakConfirmTime() * 1000;

        if (pendingBreaks.containsKey(confirmKey)) {
            long firstBreak = pendingBreaks.get(confirmKey);

            if (now - firstBreak <= confirmMs) {
                // Confirmed - allow break
                pendingBreaks.remove(confirmKey);
                lootManager.unregisterChest(chestKey);
                player.sendMessage(plugin.getPrefix() + plugin.getMsgChestBroken());
                return;
            } else {
                pendingBreaks.remove(confirmKey);
            }
        }

        // First break - require confirmation
        event.setCancelled(true);
        pendingBreaks.put(confirmKey, now);
        player.sendMessage(plugin.getPrefix() + plugin.getMsgBreakConfirm());

        // Cleanup old entries
        pendingBreaks.entrySet().removeIf(e -> now - e.getValue() > confirmMs + 5000);
    }

    private boolean isShopChest(Block block) {
        // Check metadata
        for (String meta : plugin.getMetadataBlacklist()) {
            if (block.hasMetadata(meta)) {
                return true;
            }
        }
        
        // Check custom name
        BlockState state = block.getState();
        if (state instanceof Container) {
            Container container = (Container) state;
            String customName = container.getCustomName();
            if (customName != null) {
                for (String nameCheck : plugin.getIgnoreChestNames()) {
                    if (customName.contains(nameCheck)) {
                        return true;
                    }
                }
            }
        }
        
        return false;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent event) {
        if (!plugin.isPreventExplosion()) return;

        Iterator<Block> it = event.blockList().iterator();
        while (it.hasNext()) {
            Block block = it.next();
            if (lootManager.isRegistered(lootManager.toKey(block.getLocation()))) {
                it.remove();
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent event) {
        if (!plugin.isPreventExplosion()) return;

        Iterator<Block> it = event.blockList().iterator();
        while (it.hasNext()) {
            Block block = it.next();
            if (lootManager.isRegistered(lootManager.toKey(block.getLocation()))) {
                it.remove();
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onHopperMove(InventoryMoveItemEvent event) {
        if (!plugin.isPreventHopper()) return;

        InventoryHolder source = event.getSource().getHolder();
        if (source instanceof Container) {
            Block block = ((Container) source).getBlock();
            if (lootManager.isRegistered(lootManager.toKey(block.getLocation()))) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPistonExtend(BlockPistonExtendEvent event) {
        if (!plugin.isPreventPiston()) return;

        for (Block block : event.getBlocks()) {
            if (lootManager.isRegistered(lootManager.toKey(block.getLocation()))) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPistonRetract(BlockPistonRetractEvent event) {
        if (!plugin.isPreventPiston()) return;

        for (Block block : event.getBlocks()) {
            if (lootManager.isRegistered(lootManager.toKey(block.getLocation()))) {
                event.setCancelled(true);
                return;
            }
        }
    }
}