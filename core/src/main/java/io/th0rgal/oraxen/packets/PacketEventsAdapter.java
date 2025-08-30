package io.th0rgal.oraxen.packets;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListener;
import com.github.retrooper.packetevents.event.PacketListenerCommon;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.manager.server.ServerVersion;
import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.mechanics.provided.gameplay.efficiency.EfficiencyMechanicFactory;
import io.th0rgal.oraxen.packets.packetevents.InventoryPacketListener;
import io.th0rgal.oraxen.packets.packetevents.ScoreboardPacketListener;
import io.th0rgal.oraxen.packets.packetevents.TitlePacketListener;
import io.th0rgal.oraxen.packets.packetevents.mechanics.provided.gameplay.efficiency.EfficiencyMechanicListener;
import io.th0rgal.oraxen.utils.SnapshotVersion;

public class PacketEventsAdapter implements PacketAdapter {
    private PacketListenerCommon scoreboardPacketListener;

    private PacketListenerCommon titlePacketListener;
    private PacketListenerCommon inventoryPacketListener;

    private PacketListenerCommon efficiencyMechanicListener;

    @Override
    public boolean isEnabled() {
        return PacketAdapter.isPacketEventsEnabled();
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
    public void reregisterEfficencyMechanicListener(EfficiencyMechanicFactory efficiencyMechanicFactory) {
        if (efficiencyMechanicListener != null)
            PacketEvents.getAPI().getEventManager().unregisterListener(efficiencyMechanicListener);
        efficiencyMechanicListener =register(new EfficiencyMechanicListener(efficiencyMechanicFactory), PacketListenerPriority.LOW);
    }

    @Override
    public String getLatestMCVersion() {
        return ServerVersion.getLatest().getReleaseName();
    }

    @Override
    public boolean isNewer(SnapshotVersion snapshot) {
        return true; // no way to know
    }

    private PacketListenerCommon register(PacketListener listener, PacketListenerPriority priority) {
        return PacketEvents.getAPI().getEventManager().registerListener(listener, priority);
    }
}
