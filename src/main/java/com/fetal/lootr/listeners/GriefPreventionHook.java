package com.fetal.lootr.listeners;

import java.lang.reflect.Method;
import java.util.function.Supplier;

import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;

import com.fetal.lootr.LootrPlugin;

/**
 * Hides GriefPrevention's "no permission" message when a player opens a Lootr chest in a claim.
 * GriefPrevention is only accessed through reflection, so servers without it never load its classes.
 */
public final class GriefPreventionHook implements Listener {

    private static final String EVENT_CLASS = "me.ryanhamshire.GriefPrevention.events.ClaimPermissionCheckEvent";

    private final LootrPlugin plugin;
    private final ChestListener chestListener;
    private final Class<? extends Event> eventClass;
    private final Method getRequiredPermission;
    private final Method getTriggeringEvent;
    private final Method getDenialReason;
    private final Method setDenialReason;

    private GriefPreventionHook(LootrPlugin plugin, ChestListener chestListener, Class<? extends Event> eventClass)
            throws NoSuchMethodException {
        this.plugin = plugin;
        this.chestListener = chestListener;
        this.eventClass = eventClass;
        this.getRequiredPermission = eventClass.getMethod("getRequiredPermission");
        this.getTriggeringEvent = eventClass.getMethod("getTriggeringEvent");
        this.getDenialReason = eventClass.getMethod("getDenialReason");
        this.setDenialReason = eventClass.getMethod("setDenialReason", Supplier.class);
    }

    public static void register(LootrPlugin plugin, ChestListener chestListener) {
        Plugin griefPrevention = Bukkit.getPluginManager().getPlugin("GriefPrevention");
        if (griefPrevention == null || !griefPrevention.isEnabled()) return;

        try {
            Class<? extends Event> eventClass = Class
                    .forName(EVENT_CLASS, true, griefPrevention.getClass().getClassLoader())
                    .asSubclass(Event.class);
            GriefPreventionHook hook = new GriefPreventionHook(plugin, chestListener, eventClass);

            // Denied checks count as cancelled, so this must also receive cancelled events
            Bukkit.getPluginManager().registerEvent(eventClass, hook, EventPriority.HIGHEST,
                    (listener, event) -> hook.onPermissionCheck(event), plugin, false);
            plugin.getLogger().info("GriefPrevention detected! Claim messages are hidden for Lootr chests.");
        } catch (ReflectiveOperationException | ClassCastException e) {
            plugin.getLogger().warning("GriefPrevention found, but its permission check event is unavailable"
                    + " (needs 16.18 or newer). Claim messages will still show for Lootr chests.");
        }
    }

    private void onPermissionCheck(Event event) {
        if (!eventClass.isInstance(event)) return;

        try {
            if (getDenialReason.invoke(event) == null) return;
            // Container trust is "Container" (named "Inventory" before GriefPrevention renamed it); leave build checks etc. alone
            String permission = String.valueOf(getRequiredPermission.invoke(event));
            if (!"Container".equals(permission) && !"Inventory".equals(permission)) return;

            Object trigger = getTriggeringEvent.invoke(event);
            if (trigger instanceof Event triggeringEvent && chestListener.shouldBypassClaim(triggeringEvent)) {
                // Lets GriefPrevention through silently; ChestListener re-blocks the real container afterwards
                setDenialReason.invoke(event, (Object) null);
                chestListener.markClaimBlocked(triggeringEvent);
            }
        } catch (ReflectiveOperationException e) {
            plugin.debug("GriefPrevention hook failed: " + e.getMessage());
        }
    }
}
