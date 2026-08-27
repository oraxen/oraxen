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
 * <p>
 * Packet listeners fire off the owning region thread, so no Bukkit world access may happen
 * synchronously here (world chunk/entity lookups would trip Paper's AsyncCatcher). The packet is
 * cancelled up-front and the actual lookup + item handling is deferred to the player's thread.
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
        // Cancel up-front so the vanilla packet never processes on the Netty thread and the
        // substitute item (or the vanilla fallback) is applied once we're on the main thread.
        event.setCancelled(true);
        SchedulerUtil.runForEntity(player, () -> {
            final World world = player.getWorld();

            if (packet.getType() == PacketType.Play.Client.PICK_ITEM_FROM_BLOCK) {
                final BlockPosition pos = packet.getBlockPositionModifier().read(0);
                if (pos == null) return;
                if (!world.isChunkLoaded(pos.getX() >> 4, pos.getZ() >> 4)) return;
                final Block block = world.getBlockAt(pos.getX(), pos.getY(), pos.getZ());
                if (!PickItemHandler.handleBlockPick(player, block))
                    PickItemHandler.pickBlockFallback(player, block);
            } else if (packet.getType() == PacketType.Play.Client.PICK_ITEM) {
                // ProtocolLib maps both the legacy PickItem packet and the newer PickItemFromEntity
                // packet to PICK_ITEM; only the latter carries an entity id + includeData flag.
                if (!packet.getHandle().getClass().getSimpleName().equals("ServerboundPickItemFromEntityPacket")) return;
                final int entityId = packet.getIntegers().read(0);
                final Entity entity = PickItemHandler.getEntityById(world, entityId);
                if (entity != null) PickItemHandler.handleEntityPick(player, entity);
            }
        }, null);
    }
}