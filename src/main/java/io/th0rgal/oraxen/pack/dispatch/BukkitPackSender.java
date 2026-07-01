package io.th0rgal.oraxen.pack.dispatch;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.configs.Settings;
import io.th0rgal.oraxen.pack.upload.hosts.HostingProvider;
import io.th0rgal.oraxen.utils.SchedulerUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class BukkitPackSender extends PackSender implements Listener {

    private final String prompt = Settings.SEND_PACK_PROMPT.toString();
    private final boolean mandatory = Settings.SEND_PACK_MANDATORY.toBool();
    private Listener preJoinListener;

    public BukkitPackSender(HostingProvider hostingProvider) {
        super(hostingProvider);
    }

    public void register() {
        Bukkit.getPluginManager().registerEvents(this, OraxenPlugin.get());
        registerPreJoinListener();
    }

    public void unregister() {
        HandlerList.unregisterAll(this);
        if (preJoinListener != null) {
            HandlerList.unregisterAll(preJoinListener);
            preJoinListener = null;
        }
    }

    @Override
    public void sendPack(Player player) {
        if (!PackDispatchFilter.canSendPack(player)) return;
        sendResourcePack(player, hostingProvider.getPackUUID(), hostingProvider.getPackURL(),
                hostingProvider.getSHA1(), prompt, mandatory);
    }

    private void sendPreJoinPack(net.kyori.adventure.audience.Audience audience) {
        sendResourcePack(audience, hostingProvider.getPackUUID(), hostingProvider.getPackURL(),
                hostingProvider.getSHA1(), prompt, mandatory);
    }

    private void registerPreJoinListener() {
        if (preJoinListener != null || !PackSender.isPreJoinDispatchActive()) return;
        preJoinListener = new PaperPreJoinPackSender((connection, audience) -> sendPreJoinPack(audience));
        Bukkit.getPluginManager().registerEvents(preJoinListener, OraxenPlugin.get());
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerConnect(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        boolean sendOnJoin = PackSender.isSendOnJoinConfigured();
        boolean sendPreJoin = PackSender.isSendPreJoinConfigured();
        if (!sendOnJoin && !sendPreJoin) return;
        boolean preJoinActive = PackSender.isPreJoinDispatchActive();
        if (preJoinActive) return;
        // If pre-join is configured but not active (unsupported server), fall through to on-join dispatch
        if (!sendOnJoin && sendPreJoin) sendOnJoin = true;
        if (!sendOnJoin) return;

        int delay = (int) Settings.SEND_PACK_DELAY.getValue();
        if (delay <= 0) sendPack(player);
        else SchedulerUtil.runForEntityLater(player, delay * 20L, () ->
                sendPack(player), null);
    }
}
