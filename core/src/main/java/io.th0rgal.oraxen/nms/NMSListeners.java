package io.th0rgal.oraxen.nms;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class NMSListeners implements Listener {

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (NMSHandlers.getHandler() != null) {
            NMSHandlers.getHandler().inject(event.getPlayer());
        }
    }
}
