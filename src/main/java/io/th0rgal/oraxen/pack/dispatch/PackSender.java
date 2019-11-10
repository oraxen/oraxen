package io.th0rgal.oraxen.pack.dispatch;

import io.th0rgal.oraxen.OraxenPlugin;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import org.bukkit.event.player.PlayerJoinEvent;

public class PackSender implements Listener {

    private final String url;

    public PackSender(String url) {
        this.url = url;
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onPlayerConnect(PlayerJoinEvent event) {
        Bukkit.getScheduler().runTaskLater(OraxenPlugin.get(), () ->
                        event.getPlayer().setResourcePack(url), 20L*3);
    }

}
