package io.th0rgal.oraxen.utils.fastinv;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.plugin.Plugin;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Manager for handling inventory events to FastInv
 *
 * @author MrMicky
 */
public final class FastInvManager {

    private static final AtomicBoolean REGISTER = new AtomicBoolean(false);

    /**
     * Register events for FastInv
     *
     * @param plugin Plugin to register
     * @throws NullPointerException  if plugin is null
     * @throws IllegalStateException if FastInv is already registered
     */
    public static void register(Plugin plugin) {
        Objects.requireNonNull(plugin, "plugin");

        if (REGISTER.getAndSet(true)) {
            throw new IllegalStateException("FastInv is already registered");
        }

        Bukkit.getPluginManager().registerEvents(new Listener() {

            @EventHandler(priority = EventPriority.LOW)
            public void onInventoryClick(InventoryClickEvent e) {
                if (e.getInventory().getHolder() instanceof FastInv && e.getClickedInventory() != null) {
                    FastInv inv = (FastInv) e.getInventory().getHolder();

                    boolean wasCancelled = e.isCancelled();
                    e.setCancelled(true);

                    inv.handleClick(e);

                    // This prevent to uncancel the event if an other plugin cancelled it before
                    if (!wasCancelled && !e.isCancelled()) {
                        e.setCancelled(false);
                    }
                }
            }

            @EventHandler
            public void onInventoryOpen(InventoryOpenEvent e) {
                if (e.getInventory().getHolder() instanceof FastInv) {
                    FastInv inv = (FastInv) e.getInventory().getHolder();

                    inv.handleOpen(e);
                }
            }

            @EventHandler
            public void onInventoryClose(InventoryCloseEvent e) {
                if (e.getInventory().getHolder() instanceof FastInv) {
                    FastInv inv = (FastInv) e.getInventory().getHolder();

                    if (inv.handleClose(e)) {
                        Bukkit.getScheduler().runTask(plugin, () -> inv.open((Player) e.getPlayer()));
                    }
                }
            }

            @EventHandler
            public void onPluginDisable(PluginDisableEvent e) {
                if (e.getPlugin() == plugin) {
                    closeAll();

                    REGISTER.set(false);
                }
            }
        }, plugin);
    }

    /**
     * Close all open FastInv inventories
     */
    public static void closeAll() {
        Bukkit.getOnlinePlayers().stream().filter(p -> p.getOpenInventory().getTopInventory().getHolder() instanceof FastInv).forEach(Player::closeInventory);
    }
}
