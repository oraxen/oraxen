package io.th0rgal.oraxen.packets.packetevents;

import com.github.retrooper.packetevents.event.PacketListener;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.util.Vector3i;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPickItemFromBlock;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPickItemFromEntity;
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
 * Packet listeners fire off the owning region thread, so no Bukkit world access may happen
 * synchronously here (world chunk/entity lookups would trip Paper's AsyncCatcher). The packet is
 * cancelled up-front and the actual lookup + item handling is deferred to the player's thread.
 */
public class PickItemPacketListener implements PacketListener {

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() != PacketType.Play.Client.PICK_ITEM_FROM_BLOCK
                && event.getPacketType() != PacketType.Play.Client.PICK_ITEM_FROM_ENTITY) return;
        final Player player = event.getPlayer();
        if (player == null) return;

        // Cancel up-front so the vanilla packet never processes on the Netty thread and the
        // substitute item (or the vanilla fallback) is applied once we're on the main thread.
        event.setCancelled(true);
        SchedulerUtil.runForEntity(player, () -> {
            final World world = player.getWorld();

            if (event.getPacketType() == PacketType.Play.Client.PICK_ITEM_FROM_BLOCK) {
                final Vector3i pos = new WrapperPlayClientPickItemFromBlock(event).getBlockPos();
                if (pos == null) return;
                if (!world.isChunkLoaded(pos.getX() >> 4, pos.getZ() >> 4)) return;
                final Block block = world.getBlockAt(pos.getX(), pos.getY(), pos.getZ());
                if (!PickItemHandler.handleBlockPick(player, block))
                    PickItemHandler.pickBlockFallback(player, block);
            } else {
                final int entityId = new WrapperPlayClientPickItemFromEntity(event).getEntityId();
                final Entity entity = PickItemHandler.getEntityById(world, entityId);
                if (entity != null) PickItemHandler.handleEntityPick(player, entity);
            }
        }, null);
    }
}