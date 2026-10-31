package com.fetal.lootr.listeners;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.block.Container;
import org.bukkit.block.DoubleChest;
import org.bukkit.block.Lidded;
import org.bukkit.entity.Player;
import org.bukkit.entity.minecart.StorageMinecart;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.loot.Lootable;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;

import com.fetal.lootr.LootrHolder;
import com.fetal.lootr.LootrPlugin;
import com.fetal.lootr.manager.LootManager;
import com.fetal.lootr.util.VersionCompat;

public class ChestListener implements Listener {

    private final LootrPlugin plugin;
    private final LootManager lootManager;

    // QuickShop compatibility
    private boolean quickShopEnabled = false;
    private Object quickShopAPI = null;

    // Lootr chests whose real lid is open, with the players currently viewing them
    private final Map<String, LidState> openLids = new HashMap<>();

    // Interactions a claim plugin let through only to hide its message (see GriefPreventionHook)
    private final Set<Event> claimBlockedEvents = Collections.newSetFromMap(new WeakHashMap<>());

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
    public void onMinecartChestInteract(PlayerInteractEntityEvent event) {
        // Treat it exactly like an interaction the claim blocked
        if (claimBlockedEvents.remove(event)) event.setCancelled(true);

        if (event.getHand() != EquipmentSlot.HAND) return;
        if (!(event.getRightClicked() instanceof StorageMinecart minecartChest)) return;

        Player player = event.getPlayer();
        String chestKey = lootManager.toKey(minecartChest.getLocation());

        // Claim plugins (e.g. GriefPrevention) cancel this. Lootr opens its own per-player inventory, so it can
        // ignore that for Lootr chests. The event stays cancelled, so the real minecart is never opened.
        if (event.isCancelled() && !(plugin.isBypassProtection()
                && (lootManager.isRegistered(chestKey) || minecartChest.getLootTable() != null))) {
            return;
        }

        if (player.hasPermission("lootr.bypass")) return;
        if (plugin.isWorldDisabled(minecartChest.getWorld().getName())) return;

        boolean justRegistered = false;
        if (minecartChest instanceof Lootable lootable) {
            if (lootable.getLootTable() != null) {
                if (lootManager.getRegisteredChestCount() >= plugin.getMaxTrackedChests()) {
                    plugin.getLogger().warning("Max chests reached!");
                    return;
                }

                String lootTableKey = lootable.getLootTable().getKey().toString();
                long seed = lootable.getSeed();
                int invSize = minecartChest.getInventory().getSize();

                lootManager.registerChest(chestKey, lootTableKey, seed, invSize);
                lootable.setLootTable(null);
                lootable.setSeed(0);
                minecartChest.getInventory().clear();

                plugin.debug("Registered minecart chest: " + chestKey);
                justRegistered = true;
            }
        }

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
                player.sendMessage(plugin.getPrefix() + "§cError!");
                return;
            }

            event.setCancelled(true);
            if (plugin.isPlaySound()) {
                VersionCompat.playSound(player, minecartChest.getLocation(), "block.chest.open", 1.0f, 1.0f);
            }

            if (firstTime) {
                if (justRegistered) {
                    plugin.incrementChestsRegistered();
                }
                sendChestMessage(player, plugin.getMsgFirstOpen());
                plugin.incrementLootsGenerated();
            } else if (inv.isEmpty()) {
                // Only worth saying when there's nothing left; otherwise the player can see their loot
                sendChestMessage(player, plugin.getMsgAlreadyLooted());
            }

            player.openInventory(inv);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onChestOpen(PlayerInteractEvent event) {
        // Treat it exactly like an interaction the claim blocked
        if (claimBlockedEvents.remove(event)) event.setCancelled(true);

        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (event.getHand() != EquipmentSlot.HAND) return;

        Block block = event.getClickedBlock();
        if (block == null) return;

        BlockState state = block.getState();
        if (!(state instanceof Container)) return;

        String chestKey = lootManager.toKey(block.getLocation());

        // Protection bypass logic: claim plugins (e.g. GriefPrevention) cancel this. Lootr opens its own
        // per-player inventory, so it can carry on for Lootr chests while leaving the event cancelled.
        // Anything Lootr ends up not handling (disabled world, vanilla fallback, ...) then stays blocked.
        boolean bypassedProtection = false;
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
                    
                    bypassedProtection = true;
                    plugin.debug("Bypassed protection for: " + chestKey);
                }
            }
        }

        if (event.isCancelled() && !bypassedProtection) return;

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

            if (firstTime) {
                if (justRegistered) {
                    plugin.incrementChestsRegistered();
                }
                sendChestMessage(player, plugin.getMsgFirstOpen());
                plugin.incrementLootsGenerated();
            } else if (inv.isEmpty()) {
                // Only worth saying when there's nothing left; otherwise the player can see their loot
                sendChestMessage(player, plugin.getMsgAlreadyLooted());
            }

            player.openInventory(inv);

            // Open the real lid (vanilla animation, sound and trapped chest signal) only when the player was allowed
            // to open the block. Otherwise a Lootr trapped chest inside a claim could trigger the owner's redstone.
            boolean lidOpened = !bypassedProtection
                    && player.getOpenInventory().getTopInventory().getHolder() instanceof LootrHolder
                    && openLid(chestKey, block, player);
            if (!lidOpened && plugin.isPlaySound()) {
                VersionCompat.playSound(player, block.getLocation(), "block.chest.open", 1.0f, 1.0f);
            }
        }
    }

    /**
     * Returns why Lootr would skip this container when it is opened, or null if it would not.
     * Uses the same checks as onChestOpen so manual registration can refuse chests that would never open as Lootr.
     */
    public String getIgnoreReason(Block block) {
        BlockState state = block.getState();
        if (!(state instanceof Container container)) return null;

        if (plugin.isIgnoreQuickShop() && isQuickShopChest(block)) {
            return "it is a QuickShop shop (compatibility.ignore-quickshop)";
        }
        if (isMetadataIgnored(block)) {
            return "another plugin has marked it (compatibility.metadata-blacklist)";
        }
        if (isNameIgnored(container)) {
            return "its name \"" + container.getCustomName() + "\" contains an ignored word (compatibility.ignore-chest-names-containing)";
        }
        return null;
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
            boolean lidClosed = closeLid(holder.getChestKey(), player.getUniqueId());

            if (!player.getUniqueId().equals(holder.getViewerUuid())) {
                plugin.debug("Skipped save for non-owner close: " + player.getName());
                return;
            }

            lootManager.savePlayerLoot(holder.getChestKey(), holder.getStorageUuid(), inv.getContents());

            if (!lidClosed && plugin.isPlaySound()) {
                VersionCompat.playSound(player, player.getLocation(), "block.chest.close", 1.0f, 1.0f);
            }

            plugin.debug("Saved loot for " + player.getName());
        }
    }

    /**
     * Whether a claim plugin's refusal of this interaction would be overridden anyway, because it opens a Lootr chest.
     * Mirrors the checks in onChestOpen and onMinecartChestInteract.
     */
    public boolean shouldBypassClaim(Event event) {
        if (!plugin.isBypassProtection()) return false;

        if (event instanceof PlayerInteractEvent interact) {
            Block block = interact.getClickedBlock();
            if (interact.getAction() != Action.RIGHT_CLICK_BLOCK || interact.getHand() != EquipmentSlot.HAND
                    || block == null) return false;
            if (!canUseLootr(interact.getPlayer(), block.getWorld().getName())) return false;

            BlockState state = block.getState();
            if (!(state instanceof Container)) return false;
            boolean isLootr = lootManager.isRegistered(lootManager.toKey(block.getLocation()))
                    || (state instanceof Lootable lootable && lootable.getLootTable() != null);
            return isLootr && getIgnoreReason(block) == null;
        }

        if (event instanceof PlayerInteractEntityEvent interact
                && interact.getRightClicked() instanceof StorageMinecart minecartChest) {
            if (interact.getHand() != EquipmentSlot.HAND) return false;
            if (!canUseLootr(interact.getPlayer(), minecartChest.getWorld().getName())) return false;
            return lootManager.isRegistered(lootManager.toKey(minecartChest.getLocation()))
                    || minecartChest.getLootTable() != null;
        }

        return false;
    }

    public void markClaimBlocked(Event event) {
        claimBlockedEvents.add(event);
    }

    private boolean canUseLootr(Player player, String worldName) {
        return !player.hasPermission("lootr.bypass") && !plugin.isWorldDisabled(worldName);
    }

    // An empty message in config.yml turns that message off
    private void sendChestMessage(Player player, String message) {
        if (message != null && !message.isEmpty()) {
            player.sendMessage(plugin.getPrefix() + message);
        }
    }

    /**
     * Opens the real lid of a Lootr chest, barrel or shulker box for a viewer.
     * Returns false if the block has no lid, in which case nothing was changed.
     */
    private boolean openLid(String chestKey, Block block, Player player) {
        LidState state = openLids.get(chestKey);
        if (state == null) {
            List<Block> lidBlocks = getLidBlocks(block);
            if (lidBlocks.isEmpty()) return false;

            for (Block lidBlock : lidBlocks) {
                if (lidBlock.getState() instanceof Lidded lidded) lidded.open();
                updateTrappedChestSignal(lidBlock);
            }
            state = new LidState(lidBlocks);
            openLids.put(chestKey, state);
        }
        state.viewers.add(player.getUniqueId());
        return true;
    }

    /**
     * Removes a viewer and closes the lid once nobody is viewing. Returns false if this viewer didn't open the lid.
     */
    private boolean closeLid(String chestKey, UUID viewer) {
        LidState state = openLids.get(chestKey);
        if (state == null || !state.viewers.remove(viewer)) return false;

        if (state.viewers.isEmpty()) {
            openLids.remove(chestKey);
            closeBlocks(state.blocks);
        }
        return true;
    }

    public void closeAllLids() {
        for (LidState state : openLids.values()) {
            closeBlocks(state.blocks);
        }
        openLids.clear();
    }

    private void closeBlocks(List<Block> blocks) {
        // Re-read the block state in case the block was changed while it was open
        for (Block lidBlock : blocks) {
            if (lidBlock.getState() instanceof Lidded lidded) lidded.close();
            updateTrappedChestSignal(lidBlock);
        }
    }

    // Lidded.open()/close() only animate the lid. A trapped chest's signal is its opener count, which vanilla only
    // recounts on a block tick, and that recount includes the API's forced-open flag. Ticking applies it right away.
    private void updateTrappedChestSignal(Block block) {
        if (block.getType() == Material.TRAPPED_CHEST) block.tick();
    }

    private List<Block> getLidBlocks(Block block) {
        BlockState state = block.getState();
        if (state instanceof Chest chest && chest.getInventory().getHolder() instanceof DoubleChest doubleChest
                && doubleChest.getLeftSide() instanceof Chest left && doubleChest.getRightSide() instanceof Chest right) {
            return List.of(left.getBlock(), right.getBlock());
        }
        return state instanceof Lidded ? List.of(block) : List.of();
    }

    private static class LidState {
        final List<Block> blocks;
        final Set<UUID> viewers = new HashSet<>();

        LidState(List<Block> blocks) {
            this.blocks = blocks;
        }
    }
}

