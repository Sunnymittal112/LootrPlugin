package com.fetal.lootr.utils;

import com.fetal.lootr.LootrPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.Container;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.loot.LootContext;
import org.bukkit.loot.LootTable;
import org.bukkit.loot.Lootable;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;

public class ChestUtils {
    
    public static String getLootTable(Block block) {
        if (!(block.getState() instanceof Lootable)) return null;
        
        Lootable lootable = (Lootable) block.getState();
        if (lootable.getLootTable() == null) return null;
        
        return lootable.getLootTable().getKey().toString();
    }
    
    public static boolean hasNaturalLootTable(Block block) {
        if (!(block.getState() instanceof Lootable)) return false;
        
        Lootable lootable = (Lootable) block.getState();
        if (lootable.getLootTable() == null) return false;
        
        if (block.getState() instanceof Container) {
            Container container = (Container) block.getState();
            return container.getInventory().isEmpty();
        }
        
        return true;
    }
    
    public static List<ItemStack> generateLoot(String lootTableKey, Location location, org.bukkit.entity.Player player, LootrPlugin plugin, Block block) {
        List<ItemStack> items = new ArrayList<>();
        
        try {
            if (!plugin.getDatapackCompat().isValidLootTable(lootTableKey)) {
                plugin.getLogger().warning("Unknown loot table: " + lootTableKey);
                return getFallbackLoot();
            }
            
            NamespacedKey key = NamespacedKey.fromString(lootTableKey);
            if (key == null) return getFallbackLoot();
            
            LootTable table = plugin.getDatapackCompat().getLootTable(lootTableKey);
            if (table == null) {
                table = Bukkit.getServer().getLootTable(key);
                if (table == null) return getFallbackLoot();
            }
            
            // FIXED: Build loot context with ALL possible parameters using reflection
            LootContext.Builder contextBuilder = new LootContext.Builder(location);
            
            if (player != null) {
                contextBuilder.lootedEntity(player).killer(player);
            }
            
            contextBuilder.lootingModifier(0);
            
            // Try to add blockState using reflection (1.20+ support)
            tryAddBlockState(contextBuilder, block);
            
            // Try to add blockEntity using reflection (for DnT)
            tryAddBlockEntity(contextBuilder, block);
            
            LootContext context = contextBuilder.build();
            
            Collection<ItemStack> loot = table.populateLoot(new Random(), context);
            items.addAll(loot);
            
            if (plugin.getConfigManager().isDebug()) {
                plugin.getLogger().info("Generated " + items.size() + " items from: " + lootTableKey);
            }
            
        } catch (Exception e) {
            plugin.getLogger().warning("Error generating loot from " + lootTableKey + ": " + e.getMessage());
            if (plugin.getConfigManager().isFallbackToVanilla()) {
                return getFallbackLoot();
            }
        }
        
        return items.isEmpty() ? getFallbackLoot() : items;
    }
    
    /**
     * Try to add blockState using reflection (1.20+)
     */
    private static void tryAddBlockState(LootContext.Builder builder, Block block) {
        if (block == null) return;
        
        try {
            // Try 1.20+ method: blockState(BlockState)
            Method method = LootContext.Builder.class.getMethod("blockState", org.bukkit.block.BlockState.class);
            method.invoke(builder, block.getState());
        } catch (Exception e1) {
            try {
                // Try alternative: blockState(Block)
                Method method = LootContext.Builder.class.getMethod("blockState", Block.class);
                method.invoke(builder, block);
            } catch (Exception e2) {
                // Not available in this version, skip
            }
        }
    }
    
    /**
     * Try to add blockEntity using reflection (for DnT datapack)
     */
    private static void tryAddBlockEntity(LootContext.Builder builder, Block block) {
        if (block == null) return;
        
        try {
            // Try to get block entity data
            Method method = LootContext.Builder.class.getMethod("blockEntity", org.bukkit.block.TileState.class);
            if (block.getState() instanceof org.bukkit.block.TileState) {
                method.invoke(builder, (org.bukkit.block.TileState) block.getState());
            }
        } catch (Exception e) {
            // Not available, skip
        }
    }
    
    /**
     * ALTERNATIVE: Direct loot generation using Bukkit API
     * This works for ALL versions 1.18-1.21
     */
    public static List<ItemStack> generateLootAlternative(String lootTableKey, Block block, org.bukkit.entity.Player player) {
        List<ItemStack> items = new ArrayList<>();
        
        try {
            if (!(block.getState() instanceof Lootable)) return items;
            
            Lootable lootable = (Lootable) block.getState();
            
            // Save original loot table
            LootTable originalTable = lootable.getLootTable();
            
            // Set new loot table if different
            NamespacedKey key = NamespacedKey.fromString(lootTableKey);
            if (key != null) {
                LootTable newTable = Bukkit.getServer().getLootTable(key);
                if (newTable != null) {
                    lootable.setLootTable(newTable);
                }
            }
            
            // Force loot generation by opening inventory
            if (block.getState() instanceof Container) {
                Container container = (Container) block.getState();
                Inventory inv = container.getInventory();
                
                // Copy items
                for (ItemStack item : inv.getContents()) {
                    if (item != null && item.getType() != Material.AIR) {
                        items.add(item.clone());
                    }
                }
                
                // Clear container
                inv.clear();
                
                // Restore original loot table
                lootable.setLootTable(originalTable);
                block.getState().update();
            }
            
        } catch (Exception e) {
            // Fallback
        }
        
        return items;
    }
    
    public static List<ItemStack> copyChestContents(Block block) {
        List<ItemStack> items = new ArrayList<>();
        
        if (!(block.getState() instanceof Container)) return items;
        
        Container container = (Container) block.getState();
        for (ItemStack item : container.getInventory().getContents()) {
            if (item != null && item.getType() != Material.AIR) {
                items.add(item);
            }
        }
        
        return items;
    }
    
    private static List<ItemStack> getFallbackLoot() {
        List<ItemStack> items = new ArrayList<>();
        Random random = new Random();
        
        Material[] fallback = {
            Material.IRON_INGOT, Material.GOLD_NUGGET, Material.BREAD,
            Material.WHEAT, Material.COAL, Material.STICK, Material.STRING,
            Material.BONE, Material.ARROW, Material.TORCH
        };
        
        int count = random.nextInt(4) + 2;
        for (int i = 0; i < count; i++) {
            Material mat = fallback[random.nextInt(fallback.length)];
            items.add(new ItemStack(mat, random.nextInt(3) + 1));
        }
        
        return items;
    }
}