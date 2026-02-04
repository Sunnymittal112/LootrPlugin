package com.fetal.lootr.utils;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

public class MessageUtils {
    
    public static void send(Player player, String message) {
        if (message == null || message.isEmpty()) return;
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', message));
    }
    
    public static String color(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }
}