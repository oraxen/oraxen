package io.th0rgal.oraxen.pack.dispatch;

import io.th0rgal.oraxen.config.Settings;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class PackSender implements Listener {

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerConnect(PlayerJoinEvent event) {
        if (Settings.SEND_JOIN_MESSAGE.toBool())
            PackDispatcher.sendWelcomeMessage(event.getPlayer(), true);
        if (Settings.SEND_PACK.toBool()) {
            PackDispatcher.sendPack(event.getPlayer());
        }
    }

}
