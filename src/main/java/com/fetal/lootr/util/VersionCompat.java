package com.fetal.lootr.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public final class VersionCompat {

    private static final LegacyComponentSerializer LEGACY_SERIALIZER = LegacyComponentSerializer.legacyAmpersand();

    private VersionCompat() {
    }

    public static Component toComponent(String text) {
        return LEGACY_SERIALIZER.deserialize(text == null ? "" : text);
    }

    public static Inventory createInventory(InventoryHolder holder, int size, String title) {
        try {
            return (Inventory) Bukkit.class.getMethod("createInventory", InventoryHolder.class, int.class, Component.class)
                    .invoke(null, holder, size, toComponent(title));
        } catch (ReflectiveOperationException ex) {
            try {
                return (Inventory) Bukkit.class.getMethod("createInventory", InventoryHolder.class, int.class, String.class)
                        .invoke(null, holder, size, title);
            } catch (ReflectiveOperationException ignored) {
                return Bukkit.createInventory(holder, size);
            }
        }
    }

    // soundKey is a sound key such as "block.chest.open"; Sound is no longer an enum on 26.x
    public static void playSound(Player player, Location location, String soundKey, float volume, float pitch) {
        player.playSound(location, soundKey, volume, pitch);
    }
}
