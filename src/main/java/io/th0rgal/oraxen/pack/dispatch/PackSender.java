package io.th0rgal.oraxen.pack.dispatch;

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
        event.getPlayer().setResourcePack(url);
    }



}
