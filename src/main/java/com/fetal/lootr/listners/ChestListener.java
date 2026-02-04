package com.fetal.lootr.listeners;

import com.fetal.lootr.LootrPlugin;
import com.fetal.lootr.storage.StorageManager;
import com.fetal.lootr.storage.StorageManager.ChestData;
import com.fetal.lootr.utils.ChestUtils;
import com.fetal.lootr.utils.MessageUtils;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class ChestListener implements Listener {
    
    private final LootrPlugin plugin;
    private final Map<UUID, Location> openChests;
    
    // Safe particles for all versions 1.18-1.21
    private Particle unopenedParticle;
    private Particle openedParticle;
    private Particle blockBreakParticle;
    private Particle enchantParticle;
    
    public ChestListener(LootrPlugin plugin) {
        this.plugin = plugin;
        this.openChests = new HashMap<>();
        initParticles();
    }
    
    private void initParticles() {
        unopenedParticle = getSafeParticle("VILLAGER_HAPPY", "HAPPY_VILLAGER", Particle.FLAME);
        openedParticle = getSafeParticle("DRIP_WATER", "WATER_DROP", Particle.FLAME);
        blockBreakParticle = getSafeParticle("BLOCK_CRACK", "BLOCK", Particle.EXPLOSION_NORMAL);
        enchantParticle = getSafeParticle("PORTAL", "ENCHANT", Particle.PORTAL);
    }
    
    private Particle getSafeParticle(String oldName, String newName, Particle fallback) {
        try {
            return Particle.valueOf(newName);
        } catch (IllegalArgumentException e) {
            try {
                return Particle.valueOf(oldName);
            } catch (IllegalArgumentException e2) {
                return fallback;
            }
        }
    }
    
    @EventHandler(priority = EventPriority.HIGH)
    public void onChestOpen(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        
        Block block = event.getClickedBlock();
        if (block == null) return;
        
        if (!isConvertibleContainer(block)) return;
        
        Player player = event.getPlayer();
        Location loc = block.getLocation();
        
        String worldName = loc.getWorld().getName();
        if (plugin.getConfigManager().isWorldBlacklisted(worldName)) return;
        if (!plugin.getConfigManager().isWorldEnabled(worldName)) return;
        
        event.setCancelled(true);
        
        StorageManager storage = plugin.getStorageManager();
        ChestData data = storage.getChestData(loc);
        
        // Check if this is already opened by this player (convert to normal chest)
        if (data != null && data.hasOpened(player.getUniqueId())) {
            //  CONVERT TO NORMAL CHEST - Remove Lootr data
            convertToNormalChest(player, block, data);
            return;
        }
        
        if (data == null && plugin.getConfigManager().isAutoConvert()) {
            String lootTable = ChestUtils.getLootTable(block);
            
            if (lootTable != null && !plugin.getConfigManager().isLootTableBlacklisted(lootTable)) {
                if (plugin.getDatapackCompat().isValidLootTable(lootTable)) {
                    data = new ChestData(loc, lootTable);
                    storage.setChestData(loc, data);
                    spawnConversionParticles(loc);
                    
                    if (plugin.getConfigManager().isDebug()) {
                        player.sendMessage("§7[Debug] Datapack table: " + lootTable);
                    }
                }
            } else {
                event.setCancelled(false);
                return;
            }
        }
        
        if (data == null) {
            event.setCancelled(false);
            return;
        }
        
        if (checkDecay(player, data)) return;
        
        // First time opening - give loot
        openLootChest(player, block, data);
    }
    
    private void openLootChest(Player player, Block block, ChestData data) {
        Location loc = block.getLocation();
        
        //  FIXED: Pass block parameter for datapack compatibility
        List<ItemStack> loot = ChestUtils.generateLoot(
            data.getLootTable(), 
            loc, 
            player,
            plugin,
            block
        );
        
        String title = ChatColor.GOLD + "✦ Loot Chest";
        if (data.isCustom()) title = ChatColor.GREEN + "✦ Custom Loot";
        
        Inventory inv = Bukkit.createInventory(null, 27, title);
        
        Random random = new Random();
        for (ItemStack item : loot) {
            int slot;
            int attempts = 0;
            do {
                slot = random.nextInt(27);
                attempts++;
            } while (inv.getItem(slot) != null && attempts < 50);
            
            if (attempts < 50) {
                inv.setItem(slot, item);
            }
        }
        
        // Mark as opened
        data.markOpened(player.getUniqueId());
        plugin.getStorageManager().saveChest(loc, data);
        
        if (plugin.getConfigManager().isParticlesEnabled()) {
            safeSpawnParticle(player.getWorld(), unopenedParticle, loc.clone().add(0.5, 1, 0.5), 20, 0.3, 0.3, 0.3);
        }
        if (plugin.getConfigManager().isSoundsEnabled()) {
            player.playSound(loc, Sound.BLOCK_CHEST_OPEN, 1.0f, 1.0f);
        }
        
        MessageUtils.send(player, plugin.getConfigManager().getMessage("open-first"));
        
        openChests.put(player.getUniqueId(), loc);
        player.openInventory(inv);
        
        updateChestVisual(block, true);
    }
    
    private void convertToNormalChest(Player player, Block block, ChestData data) {
        Location loc = block.getLocation();
        
        //  Remove Lootr data - convert to normal chest
        plugin.getStorageManager().removeChest(loc);
        
        // Open as normal vanilla chest
        if (block.getState() instanceof Chest) {
            Chest chest = (Chest) block.getState();
            player.openInventory(chest.getInventory());
            player.playSound(loc, Sound.BLOCK_CHEST_OPEN, 1.0f, 1.0f);
        }
        
        if (plugin.getConfigManager().isDebug()) {
            player.sendMessage("§7[Debug] Converted to normal chest");
        }
    }
    
    private boolean checkDecay(Player player, ChestData data) {
        int decayHours = plugin.getConfigManager().getDecayHours();
        if (decayHours <= 0) return false;
        
        if (data.getFirstOpened() > 0) {
            long decayTime = decayHours * 3600000L;
            if (System.currentTimeMillis() - data.getFirstOpened() > decayTime) {
                data.setDecayed(true);
                MessageUtils.send(player, plugin.getConfigManager().getMessage("decayed"));
                
                Location loc = data.getLocation();
                loc.getBlock().setType(Material.AIR);
                loc.getWorld().playSound(loc, Sound.BLOCK_WOOD_BREAK, 1.0f, 1.0f);
                
                safeSpawnParticle(loc.getWorld(), blockBreakParticle, loc.clone().add(0.5, 0.5, 0.5), 
                    20, 0.3, 0.3, 0.3);
                
                plugin.getStorageManager().removeChest(loc);
                return true;
            }
        }
        return false;
    }
    
    private void updateChestVisual(Block block, boolean opened) {
        if (!plugin.getConfigManager().useColoredBlocks()) return;
        
        Location loc = block.getLocation().clone().add(0.5, 0.5, 0.5);
        Particle particle = opened ? openedParticle : unopenedParticle;
        safeSpawnParticle(block.getWorld(), particle, loc, 10, 0.2, 0.2, 0.2);
    }
    
    private void spawnConversionParticles(Location loc) {
        safeSpawnParticle(loc.getWorld(), enchantParticle, loc.clone().add(0.5, 1, 0.5), 30, 0.3, 0.5, 0.3);
        loc.getWorld().playSound(loc, Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1.0f, 1.0f);
    }
    
    private void playOpenedEffect(Location loc) {
        loc.getWorld().playSound(loc, Sound.BLOCK_CHEST_LOCKED, 0.5f, 1.0f);
    }
    
    private void safeSpawnParticle(World world, Particle particle, Location loc, int count, double dx, double dy, double dz) {
        try {
            world.spawnParticle(particle, loc, count, dx, dy, dz);
        } catch (Exception e) {
            try {
                world.spawnParticle(Particle.SMOKE_NORMAL, loc, count / 2, dx, dy, dz);
            } catch (Exception ignored) {
            }
        }
    }
    
    private boolean isConvertibleContainer(Block block) {
        return block.getType() == Material.CHEST || 
               block.getType() == Material.TRAPPED_CHEST ||
               block.getType() == Material.BARREL;
    }
    
    public Map<UUID, Location> getOpenChests() {
        return openChests;
    }
}