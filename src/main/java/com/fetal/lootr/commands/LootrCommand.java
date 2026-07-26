package com.fetal.lootr.commands;

import com.fetal.lootr.LootrPlugin;
import com.fetal.lootr.manager.LootManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.stream.Collectors;

public class LootrCommand implements CommandExecutor, TabCompleter {

    private final LootrPlugin plugin;
    private final LootManager lootManager;
    private static final List<String> SUBS = Arrays.asList("help", "reload", "info", "stats", "clear", "reset", "list");

    public LootrCommand(LootrPlugin plugin, LootManager lootManager) {
        this.plugin = plugin;
        this.lootManager = lootManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!sender.hasPermission("lootr.admin")) {
            sender.sendMessage(plugin.getPrefix() + plugin.getMsgNoPermission());
            return true;
        }

        if (args.length == 0) {
            showHelp(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "help": showHelp(sender); break;
            case "reload": handleReload(sender); break;
            case "info": handleInfo(sender); break;
            case "stats": handleStats(sender); break;
            case "clear": handleClear(sender, args); break;
            case "reset": handleReset(sender); break;
            case "list": handleList(sender, args); break;
            default: sender.sendMessage(plugin.getPrefix() + ChatColor.RED + "Unknown command!"); break;
        }
        return true;
    }

    private void showHelp(CommandSender sender) {
        sender.sendMessage("");
        sender.sendMessage(ChatColor.GOLD + "═══════ LootrPlugin Help ═══════");
        sender.sendMessage(ChatColor.YELLOW + "/lootr reload " + ChatColor.GRAY + "- Reload config");
        sender.sendMessage(ChatColor.YELLOW + "/lootr info " + ChatColor.GRAY + "- Chest info");
        sender.sendMessage(ChatColor.YELLOW + "/lootr stats " + ChatColor.GRAY + "- Plugin stats");
        sender.sendMessage(ChatColor.YELLOW + "/lootr clear <player|all> " + ChatColor.GRAY + "- Clear data");
        sender.sendMessage(ChatColor.YELLOW + "/lootr reset " + ChatColor.GRAY + "- Reset chest");
        sender.sendMessage(ChatColor.YELLOW + "/lootr list [page] " + ChatColor.GRAY + "- List chests");
        sender.sendMessage(ChatColor.GOLD + "═════════════════════════════════");
    }

    private void handleReload(CommandSender sender) {
        long start = System.currentTimeMillis();
        plugin.reloadPlugin();
        sender.sendMessage(plugin.getPrefix() + ChatColor.GREEN + "Reloaded in " + (System.currentTimeMillis() - start) + "ms!");
    }

    private void handleInfo(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Players only!");
            return;
        }
        Player p = (Player) sender;
        Block target = p.getTargetBlockExact(5);
        if (target == null) {
            sender.sendMessage(plugin.getPrefix() + ChatColor.RED + "Look at a chest!");
            return;
        }
        String key = lootManager.toKey(target.getLocation());
        if (!lootManager.isRegistered(key)) {
            sender.sendMessage(plugin.getPrefix() + ChatColor.RED + "Not a Lootr chest.");
            return;
        }
        sender.sendMessage("");
        sender.sendMessage(ChatColor.GOLD + "══ Lootr Chest Info ══");
        sender.sendMessage(ChatColor.YELLOW + "Location: " + ChatColor.WHITE + key);
        sender.sendMessage(ChatColor.YELLOW + "Loot Table: " + ChatColor.WHITE + lootManager.getChestLootTable(key));
        sender.sendMessage(ChatColor.YELLOW + "Players Looted: " + ChatColor.WHITE + lootManager.getPlayersLootedCount(key));
        sender.sendMessage(ChatColor.YELLOW + "You Looted: " + ChatColor.WHITE + (lootManager.hasPlayerLooted(key, p.getUniqueId()) ? "Yes" : "No"));
        sender.sendMessage(ChatColor.GOLD + "════════════════════════");
    }

    private void handleStats(CommandSender sender) {
        sender.sendMessage("");
        sender.sendMessage(ChatColor.GOLD + "══ LootrPlugin Stats ══");
        sender.sendMessage(ChatColor.YELLOW + "Version: " + ChatColor.WHITE + plugin.getDescription().getVersion());
        sender.sendMessage(ChatColor.YELLOW + "Uptime: " + ChatColor.WHITE + plugin.getFormattedUptime());
        sender.sendMessage(ChatColor.YELLOW + "Registered Chests: " + ChatColor.WHITE + lootManager.getRegisteredChestCount());
        sender.sendMessage(ChatColor.YELLOW + "Player Entries: " + ChatColor.WHITE + lootManager.getTotalPlayerEntries());
        sender.sendMessage(ChatColor.YELLOW + "Session Registered: " + ChatColor.WHITE + plugin.getSessionChestsRegistered());
        sender.sendMessage(ChatColor.YELLOW + "Session Generated: " + ChatColor.WHITE + plugin.getSessionLootsGenerated());
        sender.sendMessage(ChatColor.GOLD + "═════════════════════════");
    }

    private void handleClear(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(plugin.getPrefix() + ChatColor.RED + "Usage: /lootr clear <player|all>");
            return;
        }
        if (args[1].equalsIgnoreCase("all")) {
            int count = lootManager.clearAllPlayerData();
            sender.sendMessage(plugin.getPrefix() + ChatColor.GREEN + "Cleared all! (" + count + " entries)");
        } else {
            UUID uuid = resolvePlayerUuid(args[1]);
            if (uuid == null) {
                sender.sendMessage(plugin.getPrefix() + ChatColor.RED + "Player not found: " + args[1]);
                return;
            }
            int count = lootManager.clearPlayerData(uuid);
            sender.sendMessage(plugin.getPrefix() + ChatColor.GREEN + "Cleared " + args[1] + "! (" + count + " entries)");
        }
        lootManager.saveAllData();
    }

    private UUID resolvePlayerUuid(String playerName) {
        Player online = Bukkit.getPlayerExact(playerName);
        if (online != null) {
            return online.getUniqueId();
        }

        for (Player candidate : Bukkit.getOnlinePlayers()) {
            if (candidate.getName().equalsIgnoreCase(playerName)) {
                return candidate.getUniqueId();
            }
        }

        return null;
    }

    private void handleReset(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Players only!");
            return;
        }
        Player p = (Player) sender;
        Block target = p.getTargetBlockExact(5);
        if (target == null) {
            sender.sendMessage(plugin.getPrefix() + ChatColor.RED + "Look at a chest!");
            return;
        }
        String key = lootManager.toKey(target.getLocation());
        if (!lootManager.isRegistered(key)) {
            sender.sendMessage(plugin.getPrefix() + ChatColor.RED + "Not a Lootr chest.");
            return;
        }
        int count = lootManager.resetChest(key);
        sender.sendMessage(plugin.getPrefix() + ChatColor.GREEN + "Reset! " + count + " entries cleared.");
    }

    private void handleList(CommandSender sender, String[] args) {
        Set<String> all = lootManager.getAllChestKeys();
        if (all.isEmpty()) {
            sender.sendMessage(plugin.getPrefix() + ChatColor.YELLOW + "No chests registered.");
            return;
        }
        int page = 1;
        if (args.length >= 2) {
            try { page = Integer.parseInt(args[1]); } catch (Exception e) { page = 1; }
        }
        int perPage = 8;
        List<String> list = new ArrayList<>(all);
        int totalPages = (int) Math.ceil(list.size() / (double) perPage);
        page = Math.max(1, Math.min(page, totalPages));
        int start = (page - 1) * perPage;
        int end = Math.min(start + perPage, list.size());

        sender.sendMessage("");
        sender.sendMessage(ChatColor.GOLD + "══ Lootr Chests (" + page + "/" + totalPages + ") ══");
        for (int i = start; i < end; i++) {
            String key = list.get(i);
            sender.sendMessage(ChatColor.GRAY + "• " + ChatColor.WHITE + key);
            sender.sendMessage(ChatColor.GRAY + "  └ " + ChatColor.YELLOW + lootManager.getChestLootTable(key));
        }
        if (page < totalPages) {
            sender.sendMessage(ChatColor.GRAY + "Use /lootr list " + (page + 1) + " for more");
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        if (!sender.hasPermission("lootr.admin")) return Collections.emptyList();

        if (args.length == 1) {
            return SUBS.stream().filter(s -> s.startsWith(args[0].toLowerCase())).collect(Collectors.toList());
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("clear")) {
            List<String> suggestions = new ArrayList<>();
            suggestions.add("all");
            Bukkit.getOnlinePlayers().forEach(p -> {
                if (p.getName().toLowerCase().startsWith(args[1].toLowerCase())) suggestions.add(p.getName());
            });
            return suggestions;
        }
        return Collections.emptyList();
    }
}