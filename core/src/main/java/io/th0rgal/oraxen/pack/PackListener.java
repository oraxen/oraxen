package io.th0rgal.oraxen.pack;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.config.Settings;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerResourcePackStatusEvent;

public class PackListener implements Listener {

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerConnect(PlayerJoinEvent event) {
        if (!Settings.PACK_SEND_ON_JOIN.toBool()) return;
        Player player = event.getPlayer();
        int delay = (int) Settings.PACK_SEND_DELAY.getValue();
        if (delay <= 0) OraxenPlugin.get().packServer().sendPack(player);
        else Bukkit.getScheduler().runTaskLaterAsynchronously(OraxenPlugin.get(), () ->
                OraxenPlugin.get().packServer().sendPack(player), delay * 20L);
    }

    @EventHandler
    public void onPlayerStatus(PlayerResourcePackStatusEvent event) {

    }
}
