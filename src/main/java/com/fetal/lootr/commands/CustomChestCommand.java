package com.fetal.lootr.commands;

import com.fetal.lootr.LootrPlugin;
import com.fetal.lootr.storage.StorageManager;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CustomChestCommand implements CommandExecutor {
    
    private final LootrPlugin plugin;
    
    public CustomChestCommand(LootrPlugin plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cPlayer only!");
            return true;
        }
        
        Player player = (Player) sender;
        
        if (!player.hasPermission("lootr.custom")) {
            player.sendMessage("§cNo permission!");
            return true;
        }
        
        Block target = player.getTargetBlockExact(5);
        if (target == null || (target.getType() != Material.CHEST && target.getType() != Material.BARREL)) {
            player.sendMessage("§cLook at a chest/barrel!");
            return true;
        }
        
        Location loc = target.getLocation();
        
        if (plugin.getStorageManager().isLootrChest(loc)) {
            player.sendMessage("§cAlready a Lootr chest!");
            return true;
        }
        
        StorageManager.ChestData data = new StorageManager.ChestData(loc, "custom");
        data.setCustom(true);
        plugin.getStorageManager().setChestData(loc, data);
        
        player.sendMessage("§a§l[Lootr] §aCustom chest created!");
        
        return true;
    }
}