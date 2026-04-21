package io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.text;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListener;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.player.User;
import com.github.retrooper.packetevents.protocol.entity.data.EntityData;
import com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.util.Vector3f;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerDestroyEntities;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityMetadata;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSpawnEntity;
import io.th0rgal.oraxen.nms.NMSHandlers;
import io.th0rgal.oraxen.utils.VersionUtil;
import io.th0rgal.oraxen.utils.SchedulerUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * PacketEvents listener that spawns virtual text-display entities beside
 * furniture when it is spawned client-side, and destroys them when the base
 * entity is destroyed client-side.
 *
 * <p>No real entities are created server-side; we simply ride on top of the
 * server's existing visibility/tracker logic for the base furniture entity.</p>
 */
public class FurnitureTextPacketListener implements PacketListener {

    private static final int DATA_NO_GRAVITY = 5;
    private static final MetadataIndices DATA = MetadataIndices.current();

    private static final byte FLAG_SHADOW = 0x01;
    private static final byte FLAG_SEE_THROUGH = 0x02;
    private static final byte FLAG_DEFAULT_BACKGROUND = 0x04;
    private static final byte FLAG_ALIGN_LEFT = 0x08;
    private static final byte FLAG_ALIGN_RIGHT = 0x10;

    @Override
    public void onPacketSend(PacketSendEvent event) {
        if (event.isCancelled()) return;
        if (FurnitureTextRegistry.isEmpty()) return;

        if (event.getPacketType() == PacketType.Play.Server.SPAWN_ENTITY) {
            handleSpawn(event);
        } else if (event.getPacketType() == PacketType.Play.Server.DESTROY_ENTITIES) {
            handleDestroy(event);
        }
    }

    private void handleSpawn(PacketSendEvent event) {
        WrapperPlayServerSpawnEntity spawn = new WrapperPlayServerSpawnEntity(event);
        UUID baseUuid = spawn.getUUID().orElse(null);
        if (baseUuid == null) return;

        FurnitureTextEntry entry = FurnitureTextRegistry.byUuid(baseUuid);
        if (entry == null) return;
        int baseEntityId = spawn.getEntityId();

        User user = event.getUser();
        if (user == null) return;
        Player viewer = viewerFromEvent(event);
        if (viewer != null) entry.addViewer(viewer.getUniqueId());

        Vector3d basePos = spawn.getPosition();
        SchedulerUtil.runTaskLater(1L, () -> {
            FurnitureTextEntry current = FurnitureTextRegistry.byUuid(baseUuid);
            if (current == null || current.getBaseEntityId() != baseEntityId) return;
            sendTextEntry(current, viewer, null, basePos);
        });
    }

    void sendTextEntry(FurnitureTextEntry entry, Player viewer, boolean ignoreRange) {
        if (entry == null || viewer == null || !viewer.isOnline()) return;
        if (!ignoreRange && !isWithinRange(entry, viewer)) return;
        entry.addViewer(viewer.getUniqueId());
        org.bukkit.Location location = entry.getBaseLocation();
        Vector3d basePos = new Vector3d(location.getX(), location.getY(), location.getZ());
        sendTextEntry(entry, viewer, null, basePos);
    }

    void sendTextMetadata(FurnitureTextEntry entry, Player viewer, boolean ignoreRange) {
        if (entry == null || viewer == null || !viewer.isOnline()) return;
        if (!ignoreRange && !isWithinRange(entry, viewer)) return;
        entry.addViewer(viewer.getUniqueId());
        for (int i = 0; i < entry.size(); i++) {
            FurnitureTextDefinition def = entry.getDefinitions().get(i);
            Component text = def.renderComponent(viewer);
            if (NMSHandlers.getHandler().sendTextDisplayMetadata(viewer, entry.virtualEntityId(i), text,
                    def.getScale(), billboardByte(def), def.getViewRange(), def.getLineWidth(),
                    def.getBackgroundArgb(), def.getTextOpacity(), textFlags(def))) {
                continue;
            }
            PacketEvents.getAPI().getPlayerManager().sendPacket(viewer,
                    new WrapperPlayServerEntityMetadata(entry.virtualEntityId(i), buildMetadata(def, viewer, text)));
        }
    }

    private void sendTextEntry(FurnitureTextEntry entry, Player viewer, User user, Vector3d packetBasePos) {
        org.bukkit.Location baseLocation = entry.getBaseLocation();
        float pitch = baseLocation.getPitch();
        float yaw = baseLocation.getYaw();
        for (int i = 0; i < entry.size(); i++) {
            FurnitureTextDefinition def = entry.getDefinitions().get(i);
            int virtualId = entry.virtualEntityId(i);
            UUID virtualUuid = entry.virtualUuid(i);
            Vector3f offset = rotateOffset(convertVector(def.getTranslation()), yaw);
            Vector3d basePos = packetBasePos != null
                    ? packetBasePos
                    : new Vector3d(baseLocation.getX(), baseLocation.getY(), baseLocation.getZ());
            Vector3d textPos = new Vector3d(basePos.x + offset.x, basePos.y + offset.y, basePos.z + offset.z);

            WrapperPlayServerSpawnEntity textSpawn = new WrapperPlayServerSpawnEntity(
                    virtualId,
                    Optional.of(virtualUuid),
                    EntityTypes.TEXT_DISPLAY,
                    textPos,
                    pitch,
                    yaw,
                    0f,
                    0,
                    Optional.of(new Vector3d(0.0, 0.0, 0.0))
            );

            Component text = def.renderComponent(viewer);
            org.bukkit.Location textLocation = new org.bukkit.Location(baseLocation.getWorld(), textPos.x, textPos.y, textPos.z, yaw, pitch);
            if (viewer != null && NMSHandlers.getHandler().spawnTextDisplay(viewer, virtualId, virtualUuid, textLocation,
                    text, def.getScale(), billboardByte(def), def.getViewRange(), def.getLineWidth(),
                    def.getBackgroundArgb(), def.getTextOpacity(), textFlags(def))) {
                continue;
            }

            WrapperPlayServerEntityMetadata textMeta = new WrapperPlayServerEntityMetadata(
                    virtualId,
                    buildMetadata(def, viewer, text)
            );
            sendPacket(user, viewer, textSpawn);
            SchedulerUtil.runTaskLater(1L, () -> sendPacket(user, viewer, textMeta));
        }
    }

    private static void sendPacket(User user, Player viewer, Object packet) {
        if (user != null) {
            user.sendPacket(packet);
        } else if (viewer != null) {
            PacketEvents.getAPI().getPlayerManager().sendPacket(viewer, packet);
        }
    }

    private void handleDestroy(PacketSendEvent event) {
        WrapperPlayServerDestroyEntities destroy = new WrapperPlayServerDestroyEntities(event);
        int[] ids = destroy.getEntityIds();
        if (ids == null || ids.length == 0) return;

        List<Integer> extra = null;
        for (int id : ids) {
            FurnitureTextEntry entry = FurnitureTextRegistry.byEntityId(id);
            if (entry == null) continue;
            int[] vids = entry.getVirtualEntityIds();
            if (vids.length == 0) continue;
            if (extra == null) extra = new ArrayList<>();
            for (int v : vids) extra.add(v);
        }

        if (extra == null || extra.isEmpty()) return;

        Player viewer = viewerFromEvent(event);
        if (viewer != null) {
            for (int id : ids) {
                FurnitureTextEntry entry = FurnitureTextRegistry.byEntityId(id);
                if (entry != null) entry.removeViewer(viewer.getUniqueId());
            }
        }

        int[] combined = new int[ids.length + extra.size()];
        System.arraycopy(ids, 0, combined, 0, ids.length);
        for (int i = 0; i < extra.size(); i++) combined[ids.length + i] = extra.get(i);
        destroy.setEntityIds(combined);
        event.markForReEncode(true);
    }

    void refresh(long tick) {
        if (!FurnitureTextRegistry.hasRefreshableEntries()) return;

        for (FurnitureTextEntry entry : FurnitureTextRegistry.all()) {
            if (!entry.needsRefresh() || !entry.shouldRefresh(tick)) continue;

            for (UUID viewerId : entry.getViewers()) {
                Player viewer = Bukkit.getPlayer(viewerId);
                if (viewer == null || !viewer.isOnline()) {
                    entry.removeViewer(viewerId);
                    continue;
                }
                if (!isWithinRange(entry, viewer)) continue;

                for (int i = 0; i < entry.size(); i++) {
                    FurnitureTextDefinition def = entry.getDefinitions().get(i);
                    if (!entry.shouldRefresh(def, tick)) continue;
                    Component text = def.renderComponent(viewer);
                    if (NMSHandlers.getHandler().sendTextDisplayMetadata(viewer, entry.virtualEntityId(i), text,
                            def.getScale(), billboardByte(def), def.getViewRange(), def.getLineWidth(),
                            def.getBackgroundArgb(), def.getTextOpacity(), textFlags(def))) {
                        continue;
                    }
                    PacketEvents.getAPI().getPlayerManager().sendPacket(viewer,
                            new WrapperPlayServerEntityMetadata(entry.virtualEntityId(i), buildMetadata(def, viewer, text)));
                }
            }
        }
    }

    static boolean isWithinRange(FurnitureTextEntry entry, Player viewer) {
        org.bukkit.Location baseLocation = entry.getBaseLocation();
        if (baseLocation.getWorld() == null || !baseLocation.getWorld().equals(viewer.getWorld())) {
            return false;
        }
        double maxRange = 0.0;
        for (FurnitureTextDefinition definition : entry.getDefinitions()) {
            maxRange = Math.max(maxRange, definition.getViewRange());
        }
        double range = Math.max(8.0, maxRange);
        return baseLocation.distanceSquared(viewer.getLocation()) <= range * range;
    }

    List<EntityData<?>> buildMetadata(FurnitureTextDefinition def, Player viewer) {
        return buildMetadata(def, viewer, def.renderComponent(viewer));
    }

    private List<EntityData<?>> buildMetadata(FurnitureTextDefinition def, Player viewer, Component text) {
        List<EntityData<?>> data = new ArrayList<>(8);

        data.add(new EntityData<>(DATA_NO_GRAVITY, EntityDataTypes.BOOLEAN, true));

        Vector3f scale = convertVector(def.getScale());
        data.add(new EntityData<>(DATA.displayScale, EntityDataTypes.VECTOR3F, scale));

        byte billboard = billboardByte(def);
        data.add(new EntityData<>(DATA.displayBillboard, EntityDataTypes.BYTE, billboard));
        data.add(new EntityData<>(DATA.displayViewRange, EntityDataTypes.FLOAT, def.getViewRange()));

        data.add(new EntityData<>(DATA.textDisplayText, EntityDataTypes.ADV_COMPONENT, text));
        data.add(new EntityData<>(DATA.textDisplayLineWidth, EntityDataTypes.INT, def.getLineWidth()));
        data.add(new EntityData<>(DATA.textDisplayBackground, EntityDataTypes.INT, def.getBackgroundArgb()));
        data.add(new EntityData<>(DATA.textDisplayOpacity, EntityDataTypes.BYTE, def.getTextOpacity()));

        byte flags = textFlags(def);
        data.add(new EntityData<>(DATA.textDisplayFlags, EntityDataTypes.BYTE, flags));

        return data;
    }

    private static byte billboardByte(FurnitureTextDefinition def) {
        return switch (def.getBillboard()) {
            case FIXED -> (byte) 0;
            case VERTICAL -> (byte) 1;
            case HORIZONTAL -> (byte) 2;
            case CENTER -> (byte) 3;
        };
    }

    private static byte textFlags(FurnitureTextDefinition def) {
        byte flags = 0;
        if (def.hasShadow()) flags |= FLAG_SHADOW;
        if (def.isSeeThrough()) flags |= FLAG_SEE_THROUGH;
        if (def.hasDefaultBackground()) flags |= FLAG_DEFAULT_BACKGROUND;
        switch (def.getAlignment()) {
            case LEFT -> flags |= FLAG_ALIGN_LEFT;
            case RIGHT -> flags |= FLAG_ALIGN_RIGHT;
            case CENTER -> { /* default */ }
        }
        return flags;
    }

    private static Vector3f convertVector(org.joml.Vector3f v) {
        return new Vector3f(v.x, v.y, v.z);
    }

    private static Vector3f rotateOffset(Vector3f offset, float yaw) {
        double radians = Math.toRadians(-(double) yaw);
        double x = offset.x;
        double z = offset.z;
        return new Vector3f(
                (float) (Math.cos(radians) * x - Math.sin(radians) * z),
                offset.y,
                (float) (Math.sin(radians) * x + Math.cos(radians) * z)
        );
    }

    private static Player viewerFromEvent(PacketSendEvent event) {
        Object player = event.getPlayer();
        return player instanceof Player p ? p : null;
    }

    private static final class MetadataIndices {
        private final int displayScale;
        private final int displayBillboard;
        private final int displayViewRange;
        private final int textDisplayText;
        private final int textDisplayLineWidth;
        private final int textDisplayBackground;
        private final int textDisplayOpacity;
        private final int textDisplayFlags;

        private MetadataIndices(int offset) {
            this.displayScale = 12 + offset;
            this.displayBillboard = 15 + offset;
            this.displayViewRange = 17 + offset;
            this.textDisplayText = 23 + offset;
            this.textDisplayLineWidth = 24 + offset;
            this.textDisplayBackground = 25 + offset;
            this.textDisplayOpacity = 26 + offset;
            this.textDisplayFlags = 27 + offset;
        }

        private static MetadataIndices current() {
            return new MetadataIndices(VersionUtil.atOrAbove("1.20.2") ? 0 : -1);
        }
    }
}
