package io.th0rgal.oraxen.pack.dispatch;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.config.Settings;
import io.th0rgal.oraxen.pack.upload.hosts.HostingProvider;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class BukkitPackSender extends PackSender implements Listener {

    public BukkitPackSender(HostingProvider hostingProvider) {
        super(hostingProvider);
    }

    public void register() {
        Bukkit.getPluginManager().registerEvents(this, OraxenPlugin.get());
    }

    public void unregister() {
        HandlerList.unregisterAll(this);
    }

    @Override
    public void sendPack(Player player) {
        player.setResourcePack(hostingProvider.getMinecraftPackURL(), hostingProvider.getSHA1());
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerConnect(PlayerJoinEvent event) {
        if (Settings.SEND_JOIN_MESSAGE.toBool())
            sendWelcomeMessage(event.getPlayer(), true);
        if (Settings.SEND_PACK.toBool()) {
            int delay = (int) Settings.SEND_PACK_DELAY.getValue();
            if (delay <= 0)
                sendPack(event.getPlayer());
            else Bukkit.getScheduler().runTaskLaterAsynchronously(OraxenPlugin.get(),
                    () -> sendPack(event.getPlayer()),
                    delay * 20L);
        }
    }
}
