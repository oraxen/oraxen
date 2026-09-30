package io.th0rgal.oraxen.packets;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListener;
import com.github.retrooper.packetevents.event.PacketListenerCommon;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.manager.server.ServerVersion;
import com.github.retrooper.packetevents.protocol.entity.data.EntityData;
import com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerDestroyEntities;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityMetadata;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSpawnEntity;
import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.packets.packetevents.InventoryPacketListener;
import io.th0rgal.oraxen.packets.packetevents.ScoreboardPacketListener;
import io.th0rgal.oraxen.packets.packetevents.TitlePacketListener;
import io.th0rgal.oraxen.utils.SnapshotVersion;
import io.th0rgal.oraxen.utils.VersionUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class PacketEventsAdapter implements PacketAdapter {
    private PacketListenerCommon scoreboardPacketListener;

    private PacketListenerCommon titlePacketListener;
    private PacketListenerCommon inventoryPacketListener;

    @Override
    public boolean isEnabled() {
        return PacketAdapter.isPacketEventsEnabled();
    }

    @Override
    public String backendName() {
        return "Legacy PacketEvents";
    }

    @Override
    public void registerInventoryListener() {
        if(inventoryPacketListener!= null) {
            OraxenPlugin.get().getLogger().severe("[PacketEventsAdapter]: Inventory Listener is already registered!");
            return;
        }
        inventoryPacketListener = register(new InventoryPacketListener(), PacketListenerPriority.MONITOR);
    }

    @Override
    public void registerScoreboardListener() {
        if(scoreboardPacketListener != null) {
            OraxenPlugin.get().getLogger().severe("[PacketEventsAdapter]: Scoreboard Listener is already registered!");
            return;
        }
        scoreboardPacketListener = register(new ScoreboardPacketListener(), PacketListenerPriority.MONITOR);
    }

    @Override
    public void registerTitleListener() {
        if(titlePacketListener != null) {
            OraxenPlugin.get().getLogger().severe("[PacketEventsAdapter]: Title Listener is already registered!");
            return;
        }
        titlePacketListener = register(new TitlePacketListener(), PacketListenerPriority.MONITOR);
    }

    @Override
    public void removeInventoryListener() {
        if(inventoryPacketListener != null)
            PacketEvents.getAPI().getEventManager().unregisterListener(inventoryPacketListener);
        inventoryPacketListener = null;
    }

    @Override
    public void removeTitleListener() {
        if(titlePacketListener != null)
            PacketEvents.getAPI().getEventManager().unregisterListener(titlePacketListener);
        titlePacketListener = null;
    }

    @Override
    public String getLatestMCVersion() {
        return ServerVersion.getLatest().getReleaseName();
    }

    @Override
    public boolean isNewer(SnapshotVersion snapshot) {
        return true; // no way to know
    }

    @Override
    public void spawnTextDisplay(Player viewer, int entityId, UUID uuid, Location location) {
        PacketEvents.getAPI().getPlayerManager().sendPacket(viewer, new WrapperPlayServerSpawnEntity(
                entityId,
                Optional.of(uuid),
                EntityTypes.TEXT_DISPLAY,
                new Vector3d(location.getX(), location.getY(), location.getZ()),
                location.getPitch(),
                location.getYaw(),
                0f,
                0,
                Optional.of(new Vector3d(0.0, 0.0, 0.0))
        ));
    }

    @Override
    public void sendTextDisplayMetadata(Player viewer, int entityId, Component text, Vector3f scale,
                                        byte billboard, float viewRange, int lineWidth,
                                        int backgroundArgb, byte textOpacity, byte flags) {
        int offset = VersionUtil.atOrAbove("1.20.2") ? 0 : -1;
        List<EntityData<?>> data = new ArrayList<>(9);
        data.add(new EntityData<>(5, EntityDataTypes.BOOLEAN, true));
        data.add(new EntityData<>(12 + offset, EntityDataTypes.VECTOR3F,
                new com.github.retrooper.packetevents.util.Vector3f(scale.x, scale.y, scale.z)));
        data.add(new EntityData<>(15 + offset, EntityDataTypes.BYTE, billboard));
        data.add(new EntityData<>(17 + offset, EntityDataTypes.FLOAT, viewRange));
        data.add(new EntityData<>(23 + offset, EntityDataTypes.ADV_COMPONENT, text));
        data.add(new EntityData<>(24 + offset, EntityDataTypes.INT, lineWidth));
        data.add(new EntityData<>(25 + offset, EntityDataTypes.INT, backgroundArgb));
        data.add(new EntityData<>(26 + offset, EntityDataTypes.BYTE, textOpacity));
        data.add(new EntityData<>(27 + offset, EntityDataTypes.BYTE, flags));
        PacketEvents.getAPI().getPlayerManager().sendPacket(viewer,
                new WrapperPlayServerEntityMetadata(entityId, data));
    }

    @Override
    public void destroyEntities(Player viewer, int... entityIds) {
        PacketEvents.getAPI().getPlayerManager().sendPacket(viewer,
                new WrapperPlayServerDestroyEntities(entityIds));
    }

    @Nullable
    @Override
    public Plugin getPlugin() {
        return PacketAdapter.getPacketEventsPlugin();
    }

    private PacketListenerCommon register(PacketListener listener, PacketListenerPriority priority) {
        return PacketEvents.getAPI().getEventManager().registerListener(listener, priority);
    }
}
