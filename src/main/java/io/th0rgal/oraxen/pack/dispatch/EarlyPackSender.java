package io.th0rgal.oraxen.pack.dispatch;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.config.Settings;
import io.th0rgal.oraxen.pack.upload.hosts.HostingProvider;
import org.bukkit.entity.Player;

import java.lang.reflect.InvocationTargetException;

public class EarlyPackSender extends PackSender {

    private final ProtocolManager protocolManager;

    public EarlyPackSender(HostingProvider hostingProvider) {
        super(hostingProvider);
        this.protocolManager = ProtocolLibrary.getProtocolManager();
    }

    public void register() {
        if (Settings.SEND_PACK.toBool())
            protocolManager.addPacketListener(listener);
    }

    public void unregister() {
        protocolManager.removePacketListener(listener);
    }

    @Override
    public void sendPack(Player player) {
        PacketContainer handle = protocolManager.createPacket(PacketType.Play.Server.RESOURCE_PACK_SEND);
        handle.getStrings().write(0, hostingProvider.getMinecraftPackURL());
        handle.getStrings().write(1, hostingProvider.getOriginalSHA1());
        try {
            protocolManager.sendServerPacket(player, handle);
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
    }


    private final PacketAdapter listener =
            new PacketAdapter(OraxenPlugin.get(), ListenerPriority.LOW, PacketType.Play.Server.LOGIN) {
                @Override
                public void onPacketSending(PacketEvent event) {
                    sendPack(event.getPlayer());
                }
            };
}
