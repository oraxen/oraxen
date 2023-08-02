package io.th0rgal.oraxen.pack.dispatch;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.config.Settings;
import io.th0rgal.oraxen.pack.upload.hosts.HostingProvider;
import io.th0rgal.oraxen.protocol.ProtocolInjector;
import io.th0rgal.oraxen.protocol.packet.ResourcePackRequest;
import io.th0rgal.oraxen.utils.AdventureUtils;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class AdvancedPackSender extends PackSender implements Listener {

    private final ProtocolInjector protocolInjector;
    private final String prompt;

    public AdvancedPackSender(HostingProvider hostingProvider) {
        super(hostingProvider);
        protocolInjector = OraxenPlugin.get().getProtocolInjector();
        prompt = GsonComponentSerializer.gson()
                .serialize(AdventureUtils.MINI_MESSAGE.deserialize(Settings.SEND_PACK_ADVANCED_MESSAGE.toString()));
    }

    @Override
    public void register() {
        Bukkit.getPluginManager().registerEvents(this, OraxenPlugin.get());
    }

    @Override
    public void unregister() {
        HandlerList.unregisterAll(this);
    }

    @Override
    public void sendPack(Player player) {
        protocolInjector.sendPacket(player, new ResourcePackRequest(
                hostingProvider.getMinecraftPackURL(), hostingProvider.getOriginalSHA1(),
                Settings.SEND_PACK_ADVANCED_MANDATORY.toBool(), prompt));
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerConnect(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (Settings.SEND_JOIN_MESSAGE.toBool())
            sendWelcomeMessage(player, true);
        if (Settings.SEND_PACK.toBool()) {
            int delay = (int) Settings.SEND_PACK_DELAY.getValue();
            if (delay < 1) sendPack(player);
            else Bukkit.getScheduler().runTaskLaterAsynchronously(OraxenPlugin.get(),
                    () -> sendPack(player), delay * 20L);
        }
    }

}
