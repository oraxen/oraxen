package io.th0rgal.oraxen.pack.dispatch;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.config.Settings;
import io.th0rgal.oraxen.pack.upload.hosts.HostingProvider;
import io.th0rgal.oraxen.utils.AdventureUtils;
import io.th0rgal.oraxen.utils.SchedulerUtil;
import io.th0rgal.oraxen.utils.VersionUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class BukkitPackSender extends PackSender implements Listener {

    private static final String prompt = Settings.SEND_PACK_PROMPT.toString();
    private static final boolean mandatory = Settings.SEND_PACK_MANDATORY.toBool();

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
        String layer = Settings.SEND_PACK_LAYER.toString();
        boolean useBungeeLayer = layer != null && !layer.isEmpty();

        // Pre-compute prompt conversions to avoid repetition
        net.kyori.adventure.text.Component componentPrompt = AdventureUtils.MINI_MESSAGE.deserialize(prompt);
        String legacyPrompt = AdventureUtils.LEGACY_SERIALIZER.serialize(componentPrompt);

        if (VersionUtil.atOrAbove("1.20.3")) {
            if (useBungeeLayer) {
                // BungeeCord mode: Remove old Oraxen packs, then add without clearing proxy packs
                player.removeResourcePacks(hostingProvider.getPackUUID());
                player.addResourcePack(hostingProvider.getPackUUID(), hostingProvider.getPackURL(),
                    hostingProvider.getSHA1(), legacyPrompt, mandatory);
            } else if (VersionUtil.isPaperServer()) {
                // Standalone Paper: setResourcePack clears all packs, supports Component prompt
                player.setResourcePack(hostingProvider.getPackUUID(), hostingProvider.getPackURL(),
                    hostingProvider.getSHA1(), componentPrompt, mandatory);
            } else {
                // Standalone Spigot: setResourcePack clears all packs, requires String prompt
                player.setResourcePack(hostingProvider.getPackUUID(), hostingProvider.getPackURL(),
                    hostingProvider.getSHA1(), legacyPrompt, mandatory);
            }
        } else {
            // Pre-1.20.3 versions
            if (VersionUtil.isPaperServer()) {
                player.setResourcePack(hostingProvider.getPackURL(), hostingProvider.getSHA1(),
                    componentPrompt, mandatory);
            } else {
                player.setResourcePack(hostingProvider.getPackURL(), hostingProvider.getSHA1(),
                    legacyPrompt, mandatory);
            }
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerConnect(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (Settings.SEND_JOIN_MESSAGE.toBool()) sendWelcomeMessage(player, true);
        if (!Settings.SEND_PACK.toBool()) return;
        int delay = (int) Settings.SEND_PACK_DELAY.getValue();
        if (delay <= 0) sendPack(player);
        else SchedulerUtil.runTaskLaterAsync(delay * 20L, () ->
                sendPack(player));
    }
}
