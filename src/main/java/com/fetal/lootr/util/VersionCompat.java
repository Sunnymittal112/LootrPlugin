package com.fetal.lootr.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.lang.reflect.Method;

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

    public static void playSound(Player player, Location location, String soundName, float volume, float pitch) {
        try {
            Method method = Player.class.getMethod("playSound", Location.class, String.class, float.class, float.class);
            method.invoke(player, location, soundName, volume, pitch);
            return;
        } catch (ReflectiveOperationException ignored) {
            // Fall back to the modern Sound enum overload if the server exposes it.
        }

        try {
            Method method = Player.class.getMethod("playSound", Location.class, Enum.class, float.class, float.class);
            method.invoke(player, location, Enum.valueOf((Class<Enum>) Class.forName("org.bukkit.Sound"), soundName), volume, pitch);
        } catch (ReflectiveOperationException ex) {
            player.getServer().getLogger().warning("Unsupported sound: " + soundName);
        }
    }
}
