package com.fetal.lootr.commands;

import com.fetal.lootr.LootrPlugin;
import com.fetal.lootr.storage.StorageManager;
import com.fetal.lootr.utils.ChestUtils;
import com.fetal.lootr.utils.MessageUtils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class LootrCommand implements CommandExecutor {
    
    private final LootrPlugin plugin;
    
    public LootrCommand(LootrPlugin plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("lootr.admin")) {
            sender.sendMessage("§cNo permission!");
            return true;
        }
        
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }
        
        switch (args[0].toLowerCase()) {
            case "reload":
                plugin.reload();
                sender.sendMessage("§a[Lootr] Config and datapacks reloaded!");
                break;
                
            case "convert":
                if (!(sender instanceof Player)) {
                    sender.sendMessage("§cPlayer only!");
                    return true;
                }
                convertNearby((Player) sender);
                break;
                
            case "custom":
                if (!(sender instanceof Player)) {
                    sender.sendMessage("§cPlayer only!");
                    return true;
                }
                createCustom((Player) sender);
                break;
                
            case "refresh":
                if (!(sender instanceof Player)) {
                    sender.sendMessage("§cPlayer only!");
                    return true;
                }
                refreshChest((Player) sender);
                break;
                
            case "debug":
                if (!(sender instanceof Player)) {
                    sender.sendMessage("§cPlayer only!");
                    return true;
                }
                plugin.getDatapackCompat().debugLootTables(sender);
                break;
                
            default:
                sendHelp(sender);
        }
        
        return true;
    }
    
    private void convertNearby(Player player) {
        int radius = 10;
        int count = 0;
        Location center = player.getLocation();
        
        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    Location loc = center.clone().add(x, y, z);
                    Block block = loc.getBlock();
                    
                    if (block.getType() == Material.CHEST || block.getType() == Material.BARREL) {
                        String lootTable = ChestUtils.getLootTable(block);
                        if (lootTable != null && 
                            !plugin.getStorageManager().isLootrChest(loc) &&
                            plugin.getDatapackCompat().isValidLootTable(lootTable)) {
                            
                            StorageManager.ChestData data = new StorageManager.ChestData(loc, lootTable);
                            plugin.getStorageManager().setChestData(loc, data);
                            count++;
                        }
                    }
                }
            }
        }
        
        player.sendMessage("§a[Lootr] Converted §e" + count + " §achests (including datapack)!");
    }
    
    private void createCustom(Player player) {
        Block target = player.getTargetBlockExact(5);
        if (target == null || target.getType() != Material.CHEST) {
            player.sendMessage("§cLook at a chest!");
            return;
        }
        
        Location loc = target.getLocation();
        StorageManager.ChestData data = new StorageManager.ChestData(loc, "custom");
        data.setCustom(true);
        plugin.getStorageManager().setChestData(loc, data);
        player.sendMessage("§a[Lootr] Custom chest created!");
    }
    
    private void refreshChest(Player player) {
        Block target = player.getTargetBlockExact(5);
        if (target == null) {
            player.sendMessage("§cLook at a Lootr chest!");
            return;
        }
        
        Location loc = target.getLocation();
        StorageManager.ChestData data = plugin.getStorageManager().getChestData(loc);
        
        if (data == null) {
            player.sendMessage("§cNot a Lootr chest!");
            return;
        }
        
        data.setLastRefresh(0);
        player.sendMessage("§a[Lootr] Chest refreshed!");
    }
    
    private void sendHelp(CommandSender sender) {
        sender.sendMessage("§6§l=== Lootr Commands ===");
        sender.sendMessage("§e/lootr reload §7- Reload config & datapacks");
        sender.sendMessage("§e/lootr convert §7- Convert nearby chests");
        sender.sendMessage("§e/lootr custom §7- Create custom chest");
        sender.sendMessage("§e/lootr refresh §7- Refresh target chest");
        sender.sendMessage("§e/lootr debug §7- Debug datapack tables");
        sender.sendMessage("§e/lootrdebug §7- List all loot tables");
    }
}