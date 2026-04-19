package io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.text;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListener;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.player.User;
import com.github.retrooper.packetevents.protocol.entity.data.EntityData;
import com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.world.Location;
import com.github.retrooper.packetevents.util.Quaternion4f;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.util.Vector3f;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerDestroyEntities;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityMetadata;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSetPassengers;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSpawnEntity;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * PacketEvents listener that appends virtual text-display entities to furniture
 * when it is spawned client-side, and destroys them when the base entity is
 * destroyed client-side.
 *
 * <p>No real entities are created server-side; we simply ride on top of the
 * server's existing visibility/tracker logic for the base furniture entity.</p>
 */
public class FurnitureTextPacketListener implements PacketListener {

    private static final int DATA_SHARED_FLAGS = 0;
    private static final int DATA_DISPLAY_TRANSLATION = 11;
    private static final int DATA_DISPLAY_SCALE = 12;
    private static final int DATA_DISPLAY_LEFT_ROTATION = 13;
    private static final int DATA_DISPLAY_BILLBOARD = 15;
    private static final int DATA_DISPLAY_VIEW_RANGE = 17;
    private static final int DATA_TEXT_DISPLAY_TEXT = 23;
    private static final int DATA_TEXT_DISPLAY_LINE_WIDTH = 24;
    private static final int DATA_TEXT_DISPLAY_BACKGROUND = 25;
    private static final int DATA_TEXT_DISPLAY_OPACITY = 26;
    private static final int DATA_TEXT_DISPLAY_FLAGS = 27;

    private static final byte FLAG_SHADOW = 0x01;
    private static final byte FLAG_SEE_THROUGH = 0x02;
    private static final byte FLAG_DEFAULT_BACKGROUND = 0x04;
    private static final byte FLAG_ALIGN_LEFT = 0x08;
    private static final byte FLAG_ALIGN_RIGHT = 0x10;

    @Override
    public void onPacketSend(PacketSendEvent event) {
        if (FurnitureTextRegistry.isEmpty()) return;

        if (event.getPacketType() == PacketType.Play.Server.SPAWN_ENTITY) {
            handleSpawn(event);
        } else if (event.getPacketType() == PacketType.Play.Server.DESTROY_ENTITIES) {
            handleDestroy(event);
        } else if (event.getPacketType() == PacketType.Play.Server.SET_PASSENGERS) {
            handlePassengers(event);
        }
    }

    private void handleSpawn(PacketSendEvent event) {
        WrapperPlayServerSpawnEntity spawn = new WrapperPlayServerSpawnEntity(event);
        UUID baseUuid = spawn.getUUID().orElse(null);
        if (baseUuid == null) return;

        FurnitureTextEntry entry = FurnitureTextRegistry.byUuid(baseUuid);
        if (entry == null) return;

        User user = event.getUser();
        if (user == null) return;
        Player viewer = viewerFromEvent(event);
        if (viewer != null) entry.addViewer(viewer.getUniqueId());

        Vector3d basePos = spawn.getPosition();
        int baseEntityId = spawn.getEntityId();

        List<Integer> passengerIds = new ArrayList<>(entry.size());

        for (int i = 0; i < entry.size(); i++) {
            FurnitureTextDefinition def = entry.getDefinitions().get(i);
            int virtualId = entry.virtualEntityId(i);
            UUID virtualUuid = entry.virtualUuid(i);

            WrapperPlayServerSpawnEntity textSpawn = new WrapperPlayServerSpawnEntity(
                    virtualId,
                    virtualUuid,
                    EntityTypes.TEXT_DISPLAY,
                    new Location(basePos.x, basePos.y, basePos.z, 0f, 0f),
                    0f,
                    0,
                    null
            );

            WrapperPlayServerEntityMetadata textMeta = new WrapperPlayServerEntityMetadata(
                    virtualId,
                    buildMetadata(def, viewer)
            );

            user.sendPacket(textSpawn);
            user.sendPacket(textMeta);
            passengerIds.add(virtualId);
        }

        if (!passengerIds.isEmpty()) {
            int[] ids = new int[passengerIds.size()];
            for (int i = 0; i < ids.length; i++) ids[i] = passengerIds.get(i);

            WrapperPlayServerSetPassengers setPassengers = new WrapperPlayServerSetPassengers(baseEntityId, ids);
            user.sendPacket(setPassengers);
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

    private void handlePassengers(PacketSendEvent event) {
        WrapperPlayServerSetPassengers passengers = new WrapperPlayServerSetPassengers(event);
        FurnitureTextEntry entry = FurnitureTextRegistry.byEntityId(passengers.getEntityId());
        if (entry == null) return;

        int[] combined = appendUnique(passengers.getPassengers(), entry.getVirtualEntityIds());
        passengers.setPassengers(combined);
        event.markForReEncode(true);
    }

    void refresh(long tick) {
        if (!FurnitureTextRegistry.hasRefreshableEntries()) return;

        for (FurnitureTextEntry entry : FurnitureTextRegistry.all()) {
            if (!entry.needsRefresh() || !entry.shouldRefresh(tick)) continue;

            for (UUID viewerId : entry.getViewers()) {
                Player viewer = Bukkit.getPlayer(viewerId);
                if (viewer == null || !viewer.isOnline() || !isWithinRange(entry, viewer)) {
                    entry.removeViewer(viewerId);
                    continue;
                }

                for (int i = 0; i < entry.size(); i++) {
                    FurnitureTextDefinition def = entry.getDefinitions().get(i);
                    if (def.getRefreshTicks() <= 0 && !def.usesPlaceholders()) continue;
                    PacketEvents.getAPI().getPlayerManager().sendPacket(viewer,
                            new WrapperPlayServerEntityMetadata(entry.virtualEntityId(i), buildMetadata(def, viewer)));
                }
            }
        }
    }

    private static boolean isWithinRange(FurnitureTextEntry entry, Player viewer) {
        if (entry.getBaseLocation().getWorld() == null || !entry.getBaseLocation().getWorld().equals(viewer.getWorld())) {
            return false;
        }
        double maxRange = 0.0;
        for (FurnitureTextDefinition definition : entry.getDefinitions()) {
            maxRange = Math.max(maxRange, definition.getViewRange());
        }
        double range = Math.max(8.0, maxRange);
        return entry.getBaseLocation().distanceSquared(viewer.getLocation()) <= range * range;
    }

    private static int[] appendUnique(int[] base, int[] extra) {
        Set<Integer> ids = new LinkedHashSet<>();
        if (base != null) for (int id : base) ids.add(id);
        for (int id : extra) ids.add(id);
        int[] combined = new int[ids.size()];
        int index = 0;
        for (int id : ids) combined[index++] = id;
        return combined;
    }

    List<EntityData<?>> buildMetadata(FurnitureTextDefinition def, Player viewer) {
        List<EntityData<?>> data = new ArrayList<>(8);

        data.add(new EntityData<>(DATA_SHARED_FLAGS, EntityDataTypes.BYTE, (byte) 0));

        Vector3f translation = convertVector(def.getTranslation());
        Vector3f scale = convertVector(def.getScale());
        data.add(new EntityData<>(DATA_DISPLAY_TRANSLATION, EntityDataTypes.VECTOR3F, translation));
        data.add(new EntityData<>(DATA_DISPLAY_SCALE, EntityDataTypes.VECTOR3F, scale));
        data.add(new EntityData<>(DATA_DISPLAY_LEFT_ROTATION, EntityDataTypes.QUATERNION,
                new Quaternion4f(0f, 0f, 0f, 1f)));

        byte billboard = switch (def.getBillboard()) {
            case FIXED -> (byte) 0;
            case VERTICAL -> (byte) 1;
            case HORIZONTAL -> (byte) 2;
            case CENTER -> (byte) 3;
        };
        data.add(new EntityData<>(DATA_DISPLAY_BILLBOARD, EntityDataTypes.BYTE, billboard));
        data.add(new EntityData<>(DATA_DISPLAY_VIEW_RANGE, EntityDataTypes.FLOAT, def.getViewRange()));

        Component text = def.renderComponent(viewer);
        data.add(new EntityData<>(DATA_TEXT_DISPLAY_TEXT, EntityDataTypes.ADV_COMPONENT, text));
        data.add(new EntityData<>(DATA_TEXT_DISPLAY_LINE_WIDTH, EntityDataTypes.INT, def.getLineWidth()));
        data.add(new EntityData<>(DATA_TEXT_DISPLAY_BACKGROUND, EntityDataTypes.INT, def.getBackgroundArgb()));
        data.add(new EntityData<>(DATA_TEXT_DISPLAY_OPACITY, EntityDataTypes.BYTE, def.getTextOpacity()));

        byte flags = 0;
        if (def.hasShadow()) flags |= FLAG_SHADOW;
        if (def.isSeeThrough()) flags |= FLAG_SEE_THROUGH;
        if (def.hasDefaultBackground()) flags |= FLAG_DEFAULT_BACKGROUND;
        switch (def.getAlignment()) {
            case LEFT -> flags |= FLAG_ALIGN_LEFT;
            case RIGHT -> flags |= FLAG_ALIGN_RIGHT;
            case CENTER -> { /* default */ }
        }
        data.add(new EntityData<>(DATA_TEXT_DISPLAY_FLAGS, EntityDataTypes.BYTE, flags));

        return data;
    }

    private static Vector3f convertVector(org.joml.Vector3f v) {
        return new Vector3f(v.x, v.y, v.z);
    }

    private static Player viewerFromEvent(PacketSendEvent event) {
        Object player = event.getPlayer();
        return player instanceof Player p ? p : null;
    }
}
