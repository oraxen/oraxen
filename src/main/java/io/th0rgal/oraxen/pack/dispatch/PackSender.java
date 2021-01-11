package io.th0rgal.oraxen.pack.dispatch;

import io.th0rgal.oraxen.settings.Pack;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class PackSender implements Listener {

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerConnect(PlayerJoinEvent event) {
        if ((boolean) Pack.SEND_JOIN_MESSAGE.getValue())
            PackDispatcher.sendWelcomeMessage(event.getPlayer(), true);
        if ((boolean) Pack.SEND_PACK.getValue()) {
            PackDispatcher.sendPack(event.getPlayer());
        }
    }

}
