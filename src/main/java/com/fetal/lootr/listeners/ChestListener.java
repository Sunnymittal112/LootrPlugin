package com.fetal.lootr.listeners;

import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Container;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.loot.Lootable;

import com.fetal.lootr.LootrHolder;
import com.fetal.lootr.LootrPlugin;
import com.fetal.lootr.manager.LootManager;

public class ChestListener implements Listener {

    private final LootrPlugin plugin;
    private final LootManager lootManager;

    // QuickShop compatibility
    private boolean quickShopEnabled = false;
    private Object quickShopAPI = null;

    public ChestListener(LootrPlugin plugin, LootManager lootManager) {
        this.plugin = plugin;
        this.lootManager = lootManager;

        // Check if QuickShop is loaded
        if (Bukkit.getPluginManager().isPluginEnabled("QuickShop-Hikari")) {
            quickShopEnabled = true;
            plugin.getLogger().info("QuickShop-Hikari detected in ChestListener!");

            try {
                Class<?> quickShopClass = Class.forName("com.ghostchu.quickshop.QuickShop");
                quickShopAPI = quickShopClass.getMethod("getInstance").invoke(null);
                plugin.getLogger().info("QuickShop API loaded successfully!");
            } catch (Exception e) {
                plugin.getLogger().warning("QuickShop API loading failed: " + e.getMessage());
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onChestOpen(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (event.getHand() != EquipmentSlot.HAND) return;

        Block block = event.getClickedBlock();
        if (block == null) return;

        BlockState state = block.getState();
        if (!(state instanceof Container)) return;

        String chestKey = lootManager.toKey(block.getLocation());

        // Protection bypass logic
        if (event.isCancelled() && plugin.isBypassProtection()) {
            boolean isLootr = lootManager.isRegistered(chestKey);

            // Check if it's a potential Lootr chest (vanilla with loot table)
            if (!isLootr && state instanceof Lootable) {
                if (((Lootable) state).getLootTable() != null) {
                    isLootr = true;
                }
            }

            if (isLootr) {
                // Verify it's not a shop or other ignored chest before bypassing
                if (!(plugin.isIgnoreQuickShop() && isQuickShopChest(block)) && 
                    !isMetadataIgnored(block) && 
                    !isNameIgnored((Container) state)) {
                    
                    event.setCancelled(false);
                    plugin.debug("Bypassed protection for: " + chestKey);
                }
            }
        }

        if (event.isCancelled()) return;

        Player player = event.getPlayer();
        boolean justRegistered = false;

        // QuickShop compatibility check
        if (plugin.isIgnoreQuickShop() && isQuickShopChest(block)) {
            plugin.debug("Ignoring QuickShop chest at " + chestKey);
            if (plugin.isDebug() && player.hasPermission("lootr.admin")) {
                player.sendMessage(plugin.getPrefix() + plugin.getMsgShopIgnored());
            }
            return;
        }

        // Check for other metadata
        if (isMetadataIgnored(block)) {
            return;
        }

        // Check custom name
        if (isNameIgnored((Container) state)) {
            return;
        }

        // Bypass check
        if (player.hasPermission("lootr.bypass")) return;

        // Disabled world check
        if (plugin.isWorldDisabled(block.getWorld().getName())) return;

        // Sneaking + block = building
        if (player.isSneaking() && player.getInventory().getItemInMainHand().getType().isBlock()) {
            return;
        }

        // Register chest if it has loot table
        if (state instanceof Lootable) {
            Lootable lootable = (Lootable) state;

            if (lootable.getLootTable() != null) {
                if (lootManager.getRegisteredChestCount() >= plugin.getMaxTrackedChests()) {
                    plugin.getLogger().warning("Max chests reached!");
                    return;
                }

                String lootTableKey = lootable.getLootTable().getKey().toString();
                long seed = lootable.getSeed();
                int invSize = ((Container) state).getInventory().getSize();

                lootManager.registerChest(chestKey, lootTableKey, seed, invSize);

                // Remove vanilla loot table
                lootable.setLootTable(null);
                lootable.setSeed(0);
                state.update(true, false);

                // Clear vanilla items
                BlockState fresh = block.getState();
                if (fresh instanceof Container) {
                    ((Container) fresh).getInventory().clear();
                }

                plugin.debug("Registered: " + chestKey);
                justRegistered = true;
            }
        }

        // Open per-player inventory
        if (lootManager.isRegistered(chestKey)) {
            boolean firstTime = !lootManager.hasPlayerLooted(chestKey, player.getUniqueId());

            Inventory inv = lootManager.getOrCreateLoot(chestKey, player);
            if (inv == null) {
                if (lootManager.restoreVanillaLoot(chestKey)) {
                    lootManager.unregisterChest(chestKey);
                    plugin.getLogger().info("Falling back to vanilla loot for " + chestKey);
                    return;
                }

                event.setCancelled(true);
                player.sendMessage(plugin.getPrefix() + "\u00A7cError!");
                return;
            }

            event.setCancelled(true);

            if (plugin.isPlaySound()) {
                player.playSound(block.getLocation(), Sound.BLOCK_CHEST_OPEN, 1.0f, 1.0f);
            }

            if (firstTime) {
                if (justRegistered) {
                    plugin.incrementChestsRegistered();
                }
                player.sendMessage(plugin.getPrefix() + plugin.getMsgFirstOpen());
                plugin.incrementLootsGenerated();
            } else {
                player.sendMessage(plugin.getPrefix() + plugin.getMsgAlreadyLooted());
            }

            player.openInventory(inv);
        }
    }

    /**
     * Check if a block is a QuickShop shop chest.
     */
    private boolean isQuickShopChest(Block block) {
        // Method 1: Try QuickShop API
        if (quickShopAPI != null) {
            try {
                Class<?> quickShopClass = quickShopAPI.getClass();
                Object shopManager = quickShopClass.getMethod("getShopManager").invoke(quickShopAPI);
                Object shop = shopManager.getClass()
                        .getMethod("getShop", org.bukkit.Location.class)
                        .invoke(shopManager, block.getLocation());

                if (shop != null) {
                    plugin.debug("QuickShop API: Chest is a shop");
                    return true;
                }
            } catch (Exception e) {
                plugin.debug("QuickShop API check failed: " + e.getMessage());
            }
        }

        // Method 2: Metadata check
        if (block.hasMetadata("hikari-shop") ||
            block.hasMetadata("quickshop") ||
            block.hasMetadata("qs-shop")) {
            plugin.debug("Metadata: Chest is a shop");
            return true;
        }

        // Method 3: Check custom name
        BlockState state = block.getState();
        if (state instanceof Container) {
            Container container = (Container) state;
            String customName = container.getCustomName();
            if (customName != null && customName.contains("$")) {
                plugin.debug("Custom name indicates shop");
                return true;
            }
        }

        return false;
    }

    /**
     * Check if a block should be ignored based on metadata.
     */
    private boolean isMetadataIgnored(Block block) {
        if (plugin.isIgnoreMetadataChests()) {
            for (String meta : plugin.getMetadataBlacklist()) {
                if (block.hasMetadata(meta)) {
                    plugin.debug("Ignoring chest with metadata: " + meta);
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Check if a chest should be ignored based on its custom name.
     */
    private boolean isNameIgnored(Container container) {
        String customName = container.getCustomName();
        if (customName != null) {
            for (String nameCheck : plugin.getIgnoreChestNames()) {
                if (customName.contains(nameCheck)) {
                    plugin.debug("Ignoring chest with name containing: " + nameCheck);
                    return true;
                }
            }
        }
        return false;
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) return;

        Inventory inv = event.getInventory();

        if (inv.getHolder() instanceof LootrHolder) {
            LootrHolder holder = (LootrHolder) inv.getHolder();
            Player player = (Player) event.getPlayer();

            if (!player.getUniqueId().equals(holder.getViewerUuid())) {
                plugin.debug("Skipped save for non-owner close: " + player.getName());
                return;
            }

            lootManager.savePlayerLoot(holder.getChestKey(), holder.getStorageUuid(), inv.getContents());

            if (plugin.isPlaySound()) {
                player.playSound(player.getLocation(), Sound.BLOCK_CHEST_CLOSE, 1.0f, 1.0f);
            }

            plugin.debug("Saved loot for " + player.getName());
        }
    }
}

