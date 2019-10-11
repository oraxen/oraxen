package io.th0rgal.oraxen.listeners;

import io.th0rgal.oraxen.settings.Pack;
import io.th0rgal.oraxen.pack.ResourcePack;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class PlayerListener implements Listener {


    @EventHandler
    private void onJoin(PlayerJoinEvent e) {
        if (Boolean.parseBoolean(Pack.SEND.toString()))
        ResourcePack.send(e.getPlayer());
    }


}
