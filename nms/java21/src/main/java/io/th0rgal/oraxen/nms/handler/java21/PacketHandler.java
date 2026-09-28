package io.th0rgal.oraxen.nms.handler.java21;

import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.papermc.paper.adventure.PaperAdventure;
import io.papermc.paper.network.ChannelInitializeListenerHolder;
import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.configs.Settings;
import io.th0rgal.oraxen.utils.PacketHelpers;
import io.th0rgal.oraxen.utils.logs.Logs;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.numbers.BlankFormat;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBundlePacket;
import net.minecraft.network.protocol.game.ClientboundOpenScreenPacket;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetObjectivePacket;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.craftbukkit.entity.CraftPlayer;

import java.util.ArrayList;
import java.util.List;

final class PacketHandler {

    private final NamespacedKey key;
    private volatile boolean formatInventoryTitles;
    private volatile boolean formatTitles;
    private volatile boolean hideScoreboardNumbers;

    PacketHandler() {
        key = new NamespacedKey(OraxenPlugin.get(), "packet_formatting");
        ChannelInitializeListenerHolder.removeListener(key);
        ChannelInitializeListenerHolder.addListener(key, this::install);
        for (var player : Bukkit.getOnlinePlayers())
            install(((CraftPlayer) player).getHandle().connection.connection.channel);
    }

    private void install(Channel channel) {
        var pipeline = channel.pipeline();
        if (pipeline.get(key.asString()) != null) pipeline.remove(key.asString());
        pipeline.addBefore("packet_handler", key.asString(), new ChannelDuplexHandler() {
            @Override
            public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) {
                ctx.write(transform(msg), promise);
            }
        });
    }

    void shutdown() {
        ChannelInitializeListenerHolder.removeListener(key);
        for (var player : Bukkit.getOnlinePlayers()) {
            Channel channel = ((CraftPlayer) player).getHandle().connection.connection.channel;
            if (channel.pipeline().get(key.asString()) != null) channel.pipeline().remove(key.asString());
        }
    }

    void formatInventoryTitles(boolean enabled) {
        formatInventoryTitles = enabled;
    }

    void formatTitles(boolean enabled) {
        formatTitles = enabled;
    }

    void hideScoreboardNumbers(boolean enabled) {
        hideScoreboardNumbers = enabled;
    }

    private Object transform(Object packet) {
        try {
            if (packet instanceof ClientboundBundlePacket bundle) return transformBundle(bundle);
            if (packet instanceof ClientboundOpenScreenPacket openScreen) return transformOpenScreen(openScreen);
            if (packet instanceof ClientboundSetTitleTextPacket title) return transformTitle(title);
            if (packet instanceof ClientboundSetSubtitleTextPacket subtitle) return transformSubtitle(subtitle);
            if (packet instanceof ClientboundSetActionBarTextPacket actionBar) return transformActionBar(actionBar);
            if (packet instanceof ClientboundSetObjectivePacket objective) return transformObjective(objective);
        } catch (Throwable exception) {
            if (Settings.DEBUG.toBool())
                Logs.logWarning("Failed to transform outgoing packet " + packet.getClass().getSimpleName() + ": " + exception.getMessage());
        }
        return packet;
    }

    @SuppressWarnings("unchecked")
    private ClientboundBundlePacket transformBundle(ClientboundBundlePacket bundle) {
        List<Packet<? super ClientGamePacketListener>> packets = new ArrayList<>();
        boolean changed = false;
        for (Packet<? super ClientGamePacketListener> subPacket : bundle.subPackets()) {
            Object transformed = transform(subPacket);
            if (transformed != subPacket) changed = true;
            packets.add(transformed instanceof Packet<?> packet
                    ? (Packet<? super ClientGamePacketListener>) packet
                    : subPacket);
        }
        return changed ? new ClientboundBundlePacket(packets) : bundle;
    }

    private ClientboundOpenScreenPacket transformOpenScreen(ClientboundOpenScreenPacket packet) {
        if (!formatInventoryTitles) return packet;
        return new ClientboundOpenScreenPacket(packet.getContainerId(), packet.getType(), transform(packet.getTitle()));
    }

    private ClientboundSetTitleTextPacket transformTitle(ClientboundSetTitleTextPacket packet) {
        if (!formatTitles || !Settings.FORMAT_TITLES.toBool()) return packet;
        return new ClientboundSetTitleTextPacket(transform(packet.text()));
    }

    private ClientboundSetSubtitleTextPacket transformSubtitle(ClientboundSetSubtitleTextPacket packet) {
        if (!formatTitles || !Settings.FORMAT_SUBTITLES.toBool()) return packet;
        return new ClientboundSetSubtitleTextPacket(transform(packet.text()));
    }

    private ClientboundSetActionBarTextPacket transformActionBar(ClientboundSetActionBarTextPacket packet) {
        if (!formatTitles || !Settings.FORMAT_ACTION_BAR.toBool()) return packet;
        return new ClientboundSetActionBarTextPacket(transform(packet.text()));
    }

    private ClientboundSetObjectivePacket transformObjective(ClientboundSetObjectivePacket packet) {
        if (!hideScoreboardNumbers || packet.getMethod() == ClientboundSetObjectivePacket.METHOD_REMOVE) return packet;
        Objective nativeObjective = new Objective(new Scoreboard(), packet.getObjectiveName(), ObjectiveCriteria.DUMMY,
                packet.getDisplayName(), packet.getRenderType(), false, BlankFormat.INSTANCE);
        return new ClientboundSetObjectivePacket(nativeObjective, packet.getMethod());
    }

    private static Component transform(Component component) {
        return PaperAdventure.asVanilla(PacketHelpers.translateTitle(PaperAdventure.asAdventure(component)));
    }
}
