package io.th0rgal.oraxen.pack.dispatch;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import org.bukkit.event.player.PlayerJoinEvent;

public class PackSender implements Listener {

    private final String url;
    private final byte[] sha1;
    public PackSender(String url, byte[] sha1) {
        this.url = url;
        this.sha1 = sha1;
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onPlayerConnect(PlayerJoinEvent event) {
        event.getPlayer().setResourcePack(url, sha1);
    }

}
