package io.th0rgal.oraxen.packets;

import io.th0rgal.oraxen.nms.NMSHandler;
import io.th0rgal.oraxen.nms.NMSHandlers;
import io.th0rgal.oraxen.utils.SnapshotVersion;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.UUID;

public class NativePacketAdapter implements PacketAdapter {

    private NMSHandler handler() {
        return NMSHandlers.getHandler();
    }

    @Override
    public boolean isEnabled() {
        return handler().supportsNativePacketHandling();
    }

    @Override
    public String backendName() {
        return "Native";
    }

    @Override
    public void registerInventoryListener() {
        handler().formatInventoryTitles(true);
    }

    @Override
    public void registerScoreboardListener() {
        handler().hideScoreboardNumbers(true);
    }

    @Override
    public void registerTitleListener() {
        handler().formatTitles(true);
    }

    @Override
    public void removeInventoryListener() {
        handler().formatInventoryTitles(false);
    }

    @Override
    public void removeTitleListener() {
        handler().formatTitles(false);
    }

    @Override
    public String getLatestMCVersion() {
        return PacketAdapter.EmptyAdapter.latestMCVersion(Bukkit.getMinecraftVersion());
    }

    @Override
    public boolean isNewer(SnapshotVersion snapshot) {
        return true;
    }

    @Override
    public void spawnTextDisplay(Player viewer, int entityId, UUID uuid, Location location) {
        handler().spawnTextDisplay(viewer, entityId, uuid, location);
    }

    @Override
    public void sendTextDisplayMetadata(Player viewer, int entityId, Component text, Vector3f scale,
                                        byte billboard, float viewRange, int lineWidth,
                                        int backgroundArgb, byte textOpacity, byte flags) {
        handler().sendTextDisplayMetadata(viewer, entityId, text, scale, billboard, viewRange,
                lineWidth, backgroundArgb, textOpacity, flags);
    }

    @Override
    public void destroyEntities(Player viewer, int... entityIds) {
        handler().sendEntityDestroy(viewer, entityIds);
    }

    @Nullable
    @Override
    public Plugin getPlugin() {
        return null;
    }
}
