package io.th0rgal.oraxen.pack;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.config.Settings;
import io.th0rgal.oraxen.nms.NMSHandlers;
import io.th0rgal.oraxen.utils.VersionUtil;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerResourcePackStatusEvent;

public class PackListener implements Listener {

    public static NamespacedKey CONFIG_PHASE_PACKET_LISTENER = NamespacedKey.fromString("configuration_listener", OraxenPlugin.get());

    public PackListener() {
        NMSHandlers.getHandler().unregisterConfigPhaseListener();
        if (Settings.PACK_SEND_PRE_JOIN.toBool() && VersionUtil.isPaperServer() && VersionUtil.atOrAbove("1.20.3"))
            NMSHandlers.getHandler().registerConfigPhaseListener();
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerConnect(PlayerJoinEvent event) {
        if (!Settings.PACK_SEND_ON_JOIN.toBool()) return;
        if (Settings.PACK_SEND_PRE_JOIN.toBool() && VersionUtil.isPaperServer() && event.getPlayer().hasResourcePack()) return;

        Player player = event.getPlayer();
        int delay = (int) Settings.PACK_SEND_DELAY.getValue();
        if (delay <= 0) OraxenPlugin.get().packServer().sendPack(player);
        else OraxenPlugin.get().getScheduler().runTaskLaterAsynchronously(() ->
                OraxenPlugin.get().packServer().sendPack(player), delay * 20L);
    }

    @EventHandler
    public void onPlayerStatus(PlayerResourcePackStatusEvent event) {

    }
}
