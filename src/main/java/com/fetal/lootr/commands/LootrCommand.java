package com.fetal.lootr.commands;

import com.fetal.lootr.LootrPlugin;
import com.fetal.lootr.listeners.ChestListener;
import com.fetal.lootr.manager.LootManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.block.Barrel;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.block.Container;
import org.bukkit.block.ShulkerBox;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.loot.LootTables;
import org.bukkit.loot.Lootable;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class LootrCommand implements CommandExecutor, TabCompleter {

    private final LootrPlugin plugin;
    private final LootManager lootManager;
    private final ChestListener chestListener;
    private static final List<String> SUBS = Arrays.asList("help", "reload", "info", "stats", "clear", "reset", "list", "register", "settable", "unregister");
    // data/<namespace>/loot_table(s)/<path>.json inside a datapack
    private static final Pattern DATAPACK_TABLE = Pattern.compile("data/([^/]+)/loot_tables?/(.+)\\.json");

    private Set<String> datapackTableCache;

    public LootrCommand(LootrPlugin plugin, LootManager lootManager, ChestListener chestListener) {
        this.plugin = plugin;
        this.lootManager = lootManager;
        this.chestListener = chestListener;
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
            case "register": handleRegister(sender, args); break;
            case "settable": handleSetTable(sender, args); break;
            case "unregister": handleUnregister(sender); break;
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
        sender.sendMessage(ChatColor.YELLOW + "/lootr register <table> " + ChatColor.GRAY + "- Make target chest a Lootr chest");
        sender.sendMessage(ChatColor.YELLOW + "/lootr settable <table> " + ChatColor.GRAY + "- Change target chest's loot table");
        sender.sendMessage(ChatColor.YELLOW + "/lootr unregister " + ChatColor.GRAY + "- Turn target chest back to normal");
        sender.sendMessage(ChatColor.GOLD + "═════════════════════════════════");
    }

    private void handleReload(CommandSender sender) {
        long start = System.currentTimeMillis();
        plugin.reloadPlugin();
        datapackTableCache = null;
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

    private void handleRegister(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(plugin.getPrefix() + ChatColor.RED + "Usage: /lootr register <loot_table>");
            return;
        }
        Block target = getTargetContainer(sender);
        if (target == null) return;

        String key = lootManager.toKey(target.getLocation());
        if (lootManager.isRegistered(key)) {
            sender.sendMessage(plugin.getPrefix() + ChatColor.RED + "Already a Lootr chest. Use /lootr settable to change its table.");
            return;
        }
        // Lootr skips these chests when opened, so registering would silently do nothing
        String ignoreReason = chestListener.getIgnoreReason(target);
        if (ignoreReason != null) {
            sender.sendMessage(plugin.getPrefix() + ChatColor.RED + "Can't register: " + ignoreReason + ".");
            return;
        }
        String lootTable = lootManager.resolveLootTableKey(args[1]);
        if (lootTable == null) {
            sender.sendMessage(plugin.getPrefix() + ChatColor.RED + "Unknown loot table: " + args[1]);
            return;
        }
        if (lootManager.getRegisteredChestCount() >= plugin.getMaxTrackedChests()) {
            sender.sendMessage(plugin.getPrefix() + ChatColor.RED + "Max tracked chests reached!");
            return;
        }

        BlockState state = target.getState();
        Container container = (Container) state;
        // Items left in the real container would be hidden behind the per-player view, so refuse instead of deleting them
        for (ItemStack item : container.getInventory().getContents()) {
            if (item != null && !item.getType().isAir()) {
                sender.sendMessage(plugin.getPrefix() + ChatColor.RED + "Empty the chest first!");
                return;
            }
        }
        if (!canFillContainer(sender, lootTable, target, container)) return;
        if (state instanceof Lootable lootable && lootable.getLootTable() != null) {
            lootable.setLootTable(null);
            lootable.setSeed(0);
            state.update(true, false);
        }

        lootManager.registerChest(key, lootTable, 0L, container.getInventory().getSize());
        lootManager.saveAllData();
        plugin.incrementChestsRegistered();
        sender.sendMessage(plugin.getPrefix() + ChatColor.GREEN + "Registered as Lootr chest with " + ChatColor.YELLOW + lootTable);
    }

    private void handleSetTable(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(plugin.getPrefix() + ChatColor.RED + "Usage: /lootr settable <loot_table>");
            return;
        }
        Block target = getTargetContainer(sender);
        if (target == null) return;

        String key = lootManager.toKey(target.getLocation());
        if (!lootManager.isRegistered(key)) {
            sender.sendMessage(plugin.getPrefix() + ChatColor.RED + "Not a Lootr chest. Use /lootr register first.");
            return;
        }
        String lootTable = lootManager.resolveLootTableKey(args[1]);
        if (lootTable == null) {
            sender.sendMessage(plugin.getPrefix() + ChatColor.RED + "Unknown loot table: " + args[1]);
            return;
        }
        if (!canFillContainer(sender, lootTable, target, (Container) target.getState())) return;

        lootManager.setChestLootTable(key, lootTable);
        // Old rolls came from the previous table, so drop them
        int count = lootManager.resetChest(key);
        lootManager.saveAllData();
        sender.sendMessage(plugin.getPrefix() + ChatColor.GREEN + "Loot table set to " + ChatColor.YELLOW + lootTable
                + ChatColor.GREEN + " (" + count + " entries cleared)");
    }

    private void handleUnregister(CommandSender sender) {
        Block target = getTargetContainer(sender);
        if (target == null) return;

        String key = lootManager.toKey(target.getLocation());
        if (!lootManager.isRegistered(key)) {
            sender.sendMessage(plugin.getPrefix() + ChatColor.RED + "Not a Lootr chest.");
            return;
        }
        lootManager.unregisterChest(key);
        lootManager.saveAllData();
        sender.sendMessage(plugin.getPrefix() + ChatColor.GREEN + "Chest is no longer a Lootr chest.");
    }

    // Some tables (block drops, mob drops) need context Lootr can't provide and would fail every time the chest is opened
    private boolean canFillContainer(CommandSender sender, String lootTable, Block target, Container container) {
        String problem = lootManager.testLootTable(lootTable, target.getLocation(), (Player) sender, container.getInventory().getSize());
        if (problem == null) return true;

        sender.sendMessage(plugin.getPrefix() + ChatColor.RED + "Can't use " + lootTable + " in a container: " + problem);
        return false;
    }

    private Block getTargetContainer(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Players only!");
            return null;
        }
        Block target = ((Player) sender).getTargetBlockExact(5);
        if (target == null || !isSupportedContainer(target.getState())) {
            sender.sendMessage(plugin.getPrefix() + ChatColor.RED + "Look at a chest, barrel or shulker box!");
            return null;
        }
        return target;
    }

    // Lootr shows loot in a chest-style inventory, which needs rows of 9 slots.
    // Furnaces, hoppers, brewing stands etc. are Containers too but don't fit that.
    private boolean isSupportedContainer(BlockState state) {
        return state instanceof Chest || state instanceof Barrel || state instanceof ShulkerBox;
    }

    private Set<String> getLootTableSuggestions() {
        Set<String> tables = new TreeSet<>(getDatapackTables());
        // Chest and pot tables are chest-type tables, so they always work in a Lootr container.
        // Others (mob drops, block drops, fishing...) usually need context Lootr can't provide.
        for (LootTables table : LootTables.values()) {
            String key = table.getKey().toString();
            if (plugin.isSuggestAllVanillaTables() || key.startsWith("minecraft:chests/") || key.startsWith("minecraft:pots/")) {
                tables.add(key);
            }
        }
        return tables;
    }

    private Set<String> getDatapackTables() {
        if (datapackTableCache != null) return datapackTableCache;

        Set<String> found = new TreeSet<>();
        // Datapacks always live in the main world folder. World#getWorldFolder points at a dimension folder on 26.x.
        File datapacks = Bukkit.getServer().getLevelDirectory().resolve("datapacks").toFile();
        File[] packs = datapacks.listFiles();
        if (packs != null) {
            for (File pack : packs) {
                try {
                    if (pack.isDirectory()) {
                        Path root = pack.toPath();
                        try (Stream<Path> paths = Files.walk(root)) {
                            paths.forEach(p -> addTable(found, root.relativize(p).toString().replace(File.separatorChar, '/')));
                        }
                    } else if (pack.getName().endsWith(".zip")) {
                        try (ZipFile zip = new ZipFile(pack)) {
                            Enumeration<? extends ZipEntry> entries = zip.entries();
                            while (entries.hasMoreElements()) {
                                addTable(found, entries.nextElement().getName());
                            }
                        }
                    }
                } catch (IOException e) {
                    plugin.debug("Could not scan datapack " + pack.getName() + ": " + e.getMessage());
                }
            }
        }
        datapackTableCache = found;
        return found;
    }

    private void addTable(Set<String> tables, String path) {
        Matcher m = DATAPACK_TABLE.matcher(path);
        if (m.matches() && !m.group(1).equals("minecraft")) {
            tables.add(m.group(1) + ":" + m.group(2));
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
        if (args.length == 2 && (args[0].equalsIgnoreCase("register") || args[0].equalsIgnoreCase("settable"))) {
            String typed = args[1].toLowerCase();
            return getLootTableSuggestions().stream()
                    .filter(t -> t.startsWith(typed) || t.substring(t.indexOf(':') + 1).startsWith(typed))
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }
}