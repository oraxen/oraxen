package io.th0rgal.oraxen.nms;

import io.th0rgal.oraxen.config.Settings;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class NMSListeners implements Listener {

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (NMSHandlers.getHandler() != null && Settings.NMS_GLYPHS.toBool())
            NMSHandlers.getHandler().inject(event.getPlayer());
    }
}
