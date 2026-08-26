package io.th0rgal.oraxen.packets.packetevents;

import com.github.retrooper.packetevents.event.PacketListener;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.util.Vector3i;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPickItemFromBlock;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPickItemFromEntity;
import io.th0rgal.oraxen.packets.PickItemHandler;
import io.th0rgal.oraxen.utils.SchedulerUtil;
import io.th0rgal.oraxen.utils.VersionUtil;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

/**
 * Intercepts creative middle-click picks (since MC 1.21.4 these arrive as
 * {@code PICK_ITEM_FROM_BLOCK} / {@code PICK_ITEM_FROM_ENTITY} packets which no longer fire
 * {@code InventoryCreativeEvent}) and substitutes the real Oraxen furniture item.
 */
public class PickItemPacketListener implements PacketListener {

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() != PacketType.Play.Client.PICK_ITEM_FROM_BLOCK
                && event.getPacketType() != PacketType.Play.Client.PICK_ITEM_FROM_ENTITY) return;
        final Player player = event.getPlayer();
        if (player == null) return;

        if (event.getPacketType() == PacketType.Play.Client.PICK_ITEM_FROM_BLOCK) {
            final Vector3i pos = new WrapperPlayClientPickItemFromBlock(event).getBlockPos();
            if (pos == null) return;
            // PacketEvents invokes this listener from Netty. On Folia, reading block state from
            // that thread can crash CraftBlock#getType because no region world data is bound.
            if (VersionUtil.isFoliaServer()) {
                event.setCancelled(true);
                SchedulerUtil.runForEntity(player, () -> {
                    final World world = player.getWorld();
                    final Location location = new Location(world, pos.getX(), pos.getY(), pos.getZ());
                    SchedulerUtil.runAtLocation(location, () -> {
                        if (!world.isChunkLoaded(pos.getX() >> 4, pos.getZ() >> 4)) return;
                        final Block block = world.getBlockAt(pos.getX(), pos.getY(), pos.getZ());
                        if (!PickItemHandler.handleBlockPick(player, block))
                            PickItemHandler.pickBlockFallback(player, block);
                    });
                }, null);
                return;
            }
            final Block block = player.getWorld().getBlockAt(pos.getX(), pos.getY(), pos.getZ());
            if (PickItemHandler.handleBlockPick(player, block)) event.setCancelled(true);
        } else {
            final int entityId = new WrapperPlayClientPickItemFromEntity(event).getEntityId();
            if (VersionUtil.isFoliaServer()) {
                event.setCancelled(true);
                SchedulerUtil.runForEntity(player, () -> {
                    final Entity entity = PickItemHandler.getEntityById(player.getWorld(), entityId);
                    if (entity != null) PickItemHandler.handleEntityPick(player, entity);
                }, null);
                return;
            }
            final Entity entity = PickItemHandler.getEntityById(player.getWorld(), entityId);
            if (entity != null && PickItemHandler.handleEntityPick(player, entity)) event.setCancelled(true);
        }
    }
}
