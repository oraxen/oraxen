package io.th0rgal.oraxen.pack.dispatch;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.WrappedChatComponent;
import com.viaversion.viaversion.api.Via;
import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.config.Settings;
import io.th0rgal.oraxen.pack.upload.hosts.HostingProvider;
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

    private final ProtocolManager protocolManager;
    private final WrappedChatComponent component;
    private boolean useViaVersion;

    public AdvancedPackSender(HostingProvider hostingProvider) {
        super(hostingProvider);
        protocolManager = OraxenPlugin.get().getProtocolManager();
        component = WrappedChatComponent.fromJson(GsonComponentSerializer.gson()
                .serialize(AdventureUtils.MINI_MESSAGE.deserialize(Settings.SEND_PACK_ADVANCED_MESSAGE.toString())));
    }

    @SuppressWarnings("unchecked")
    public int getPlayerProtocol(Player player) {
        if (useViaVersion) {
            return Via.getAPI().getPlayerVersion(player);
        } else {
            return protocolManager.getProtocolVersion(player);
        }
    }

    @Override
    public void register() {
        Bukkit.getPluginManager().registerEvents(this, OraxenPlugin.get());
        if (Bukkit.getPluginManager().isPluginEnabled("ViaVersion")) {
            try {
                Class.forName("com.viaversion.viaversion.api.Via");
                useViaVersion = true;
            } catch (ClassNotFoundException ignored) { }
        }
    }

    @Override
    public void unregister() {
        HandlerList.unregisterAll(this);
    }

    @Override
    public void sendPack(Player player) {
        int minProtocol = (int) Settings.SEND_PACK_ADVANCED_MIN_PROTOCOL.getValue();
        int playerProtocol;
        if (minProtocol > 0 && (playerProtocol = getPlayerProtocol(player)) > 0 && playerProtocol < minProtocol) {
            return;
        }
        PacketContainer handle = protocolManager.createPacket(PacketType.Play.Server.RESOURCE_PACK_SEND);
        handle.getStrings().write(0, hostingProvider.getMinecraftPackURL());
        handle.getStrings().write(1, hostingProvider.getOriginalSHA1());
        handle.getBooleans().write(0, Settings.SEND_PACK_ADVANCED_MANDATORY.toBool());
        handle.getChatComponents().write(0, component);
        protocolManager.sendServerPacket(player, handle);
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
