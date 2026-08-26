package io.th0rgal.oraxen.packets.protocollib;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.BlockPosition;
import io.th0rgal.oraxen.OraxenPlugin;
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
 * <p>
 * ProtocolLib maps the newer PickItemFromEntity packet to {@code PICK_ITEM} (sharing its id with
 * the legacy PickItem packet), so the packet class name is used to tell them apart.
 */
public class PickItemPacketListener extends PacketAdapter {

    public PickItemPacketListener() {
        super(OraxenPlugin.get(), ListenerPriority.LOW,
                PacketType.Play.Client.PICK_ITEM_FROM_BLOCK,
                PacketType.Play.Client.PICK_ITEM);
    }

    @Override
    public void onPacketReceiving(PacketEvent event) {
        final Player player = event.getPlayer();
        if (player == null) return;
        final PacketContainer packet = event.getPacket();

        if (packet.getType() == PacketType.Play.Client.PICK_ITEM_FROM_BLOCK) {
            final BlockPosition pos = packet.getBlockPositionModifier().read(0);
            if (pos == null) return;
            // ProtocolLib invokes this listener from Netty. On Folia, reading block state from
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
        } else if (packet.getType() == PacketType.Play.Client.PICK_ITEM) {
            // ProtocolLib maps both the legacy PickItem packet and the newer PickItemFromEntity
            // packet to PICK_ITEM; only the latter carries an entity id + includeData flag.
            if (!packet.getHandle().getClass().getSimpleName().equals("ServerboundPickItemFromEntityPacket")) return;
            final int entityId = packet.getIntegers().read(0);
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
