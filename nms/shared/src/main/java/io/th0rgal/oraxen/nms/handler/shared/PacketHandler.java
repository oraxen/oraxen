package io.th0rgal.oraxen.nms.handler.shared;

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
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.network.protocol.game.ClientboundSetPassengersPacket;
import net.minecraft.network.protocol.game.ClientboundOpenScreenPacket;
import net.minecraft.network.protocol.game.ClientboundPlaceGhostRecipePacket;
import net.minecraft.network.protocol.game.ClientboundRecipeBookRemovePacket;
import net.minecraft.network.protocol.game.ClientboundRecipeBookAddPacket;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetObjectivePacket;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityType;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.craftbukkit.entity.CraftEntity;
import org.bukkit.entity.Entity;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

final class PacketHandler {

    private final NamespacedKey key;
    private final TridentPacketHandler tridents = new TridentPacketHandler();
    private final Map<Integer, net.minecraft.world.entity.Entity> furnitureHitboxes = new ConcurrentHashMap<>();
    private volatile boolean formatInventoryTitles;
    private volatile boolean formatTitles;
    private volatile boolean hideScoreboardNumbers;

    PacketHandler() {
        key = new NamespacedKey(OraxenPlugin.get(), "packet_formatting");
        ChannelInitializeListenerHolder.removeListener(key);
        ChannelInitializeListenerHolder.addListener(key, this::install);
        for (var player : Bukkit.getOnlinePlayers()) {
            try {
                install(((CraftPlayer) player).getHandle().connection.connection.channel);
            } catch (RuntimeException ignored) {
                // The connection closed concurrently, it no longer needs the handler
            }
        }
    }

    private void install(Channel channel) {
        var pipeline = channel.pipeline();
        if (pipeline.get(key.asString()) != null) pipeline.remove(key.asString());
        pipeline.addBefore("packet_handler", key.asString(), new ChannelDuplexHandler() {
            private final TridentPacketHandler.Session tridentSession = tridents.session();
            private final RecipeBookDisplays recipeBook = new RecipeBookDisplays();
            @Override
            public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) {
                try {
                    msg = tridentSession.transform(msg);
                } catch (ReflectiveOperationException | LinkageError exception) {
                    Logs.logWarning("Failed to render custom trident.\n" + exception.getMessage());
                }
                ctx.write(transform(msg, recipeBook), promise);
            }
        });
    }

    void shutdown() {
        tridents.shutdown();
        furnitureHitboxes.clear();
        ChannelInitializeListenerHolder.removeListener(key);
        for (var player : Bukkit.getOnlinePlayers()) {
            try {
                Channel channel = ((CraftPlayer) player).getHandle().connection.connection.channel;
                if (channel.pipeline().get(key.asString()) != null) channel.pipeline().remove(key.asString());
            } catch (RuntimeException ignored) {
                // A closing channel may drop the handler between get and remove; keep cleaning the others
            }
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

    void trackFurnitureHitbox(Entity entity) {
        // Folia guards getHandle by region, so keep the handle for reads from Netty threads.
        furnitureHitboxes.put(entity.getEntityId(), ((CraftEntity) entity).getHandle());
    }

    void untrackFurnitureHitbox(Entity entity) {
        furnitureHitboxes.computeIfPresent(entity.getEntityId(),
                (id, tracked) -> tracked.getUUID().equals(entity.getUniqueId()) ? null : tracked);
    }

    private Object transform(Object packet, RecipeBookDisplays recipeBook) {
        try {
            if (packet instanceof ClientboundBundlePacket bundle) return transformBundle(bundle, recipeBook);
            if (packet instanceof ClientboundAddEntityPacket spawn) return transformFurnitureHitboxSpawn(spawn);
            if (packet instanceof ClientboundSetPassengersPacket passengers) return transformFurnitureHitboxPassengers(passengers);
            if (packet instanceof ClientboundRecipeBookAddPacket recipes) return recipeBook.add(recipes);
            if (packet instanceof ClientboundRecipeBookRemovePacket recipes) recipeBook.remove(recipes);
            if (packet instanceof ClientboundPlaceGhostRecipePacket ghost) return recipeBook.ghost(ghost);
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
    private ClientboundBundlePacket transformBundle(ClientboundBundlePacket bundle, RecipeBookDisplays recipeBook) {
        List<Packet<? super ClientGamePacketListener>> packets = new ArrayList<>();
        boolean changed = false;
        for (Packet<? super ClientGamePacketListener> subPacket : bundle.subPackets()) {
            Object transformed = transform(subPacket, recipeBook);
            if (transformed != subPacket) changed = true;
            if (transformed instanceof ClientboundBundlePacket expanded) {
                expanded.subPackets().forEach(packets::add);
            } else {
                packets.add(transformed instanceof Packet<?> packet
                        ? (Packet<? super ClientGamePacketListener>) packet
                        : subPacket);
            }
        }
        return changed ? new ClientboundBundlePacket(packets) : bundle;
    }

    private Object transformFurnitureHitboxSpawn(ClientboundAddEntityPacket spawn) {
        var hitbox = furnitureHitboxes.get(spawn.getId());
        if (hitbox == null || !spawn.getUUID().equals(hitbox.getUUID()) || Settings.DEBUG.toBool()) return spawn;
        // A client can render the spawn before a separate invisibility update arrives.
        // Bundle them so the first rendered frame already has the invisible flag.
        var invisible = SynchedEntityData.DataValue.create(
                new EntityDataAccessor<>(0, EntityDataSerializers.BYTE), (byte) 0x20);
        return new ClientboundBundlePacket(List.of(spawn,
                new ClientboundSetEntityDataPacket(spawn.getId(), List.of(invisible))));
    }

    // Mounting resets a shulker's attached face on the client, so resend its data after each passenger update.
    private Object transformFurnitureHitboxPassengers(ClientboundSetPassengersPacket packet) {
        List<Packet<? super ClientGamePacketListener>> packets = null;
        for (int id : packet.getPassengers()) {
            var hitbox = furnitureHitboxes.get(id);
            if (hitbox == null || hitbox.getType() != EntityType.SHULKER) continue;
            List<SynchedEntityData.DataValue<?>> values = hitbox.getEntityData().getNonDefaultValues();
            if (values == null) continue;
            if (packets == null) packets = new ArrayList<>(List.of(packet));
            packets.add(new ClientboundSetEntityDataPacket(id, values));
        }
        return packets == null ? packet : new ClientboundBundlePacket(packets);
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
