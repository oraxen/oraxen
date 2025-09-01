package io.th0rgal.oraxen.utils.breaker;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListener;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.DiggingAction;
import com.github.retrooper.packetevents.util.Vector3i;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerDigging;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerBlockBreakAnimation;
import io.th0rgal.oraxen.OraxenPlugin;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;

public class PacketEventsBreakerSystem extends BreakerSystem {
    private final PacketListener listener = new PacketListener() {
        @Override public void onPacketReceive(PacketReceiveEvent event) {
            if (!event.getPacketType().equals(PacketType.Play.Client.PLAYER_DIGGING)) return;
            final var wrapper = new WrapperPlayClientPlayerDigging(event);
            final Player player = event.getPlayer();

            final Vector3i pos = wrapper.getBlockPosition();
            BlockFace blockFace;
            try {
                blockFace = BlockFace.valueOf(wrapper.getBlockFace().name());
            } catch (IllegalArgumentException e) {
                OraxenPlugin.get().getLogger().warning("[PacketEvents] Failed to decode BlockFace: " + wrapper.getBlockFace().name());
                blockFace = BlockFace.UP;
            }

            final World world = player.getWorld();
            final Block block = world.getBlockAt(pos.getX(), pos.getY(), pos.getZ());
            final Location location = block.getLocation();

            handleEvent(player, block, location, blockFace, world, () -> event.setCancelled(true), wrapper.getAction() == DiggingAction.START_DIGGING);
        }
    };

    @Override
    protected void sendBlockBreak(final Player player, final Location location, final int stage) {
        var wrapper = new WrapperPlayServerBlockBreakAnimation(
            player.getEntityId(),
            new Vector3i(location.getBlockX(), location.getBlockY(), location.getBlockZ()),
            (byte) stage
        );
        PacketEvents.getAPI().getPlayerManager().sendPacket(player, wrapper);
    }

    @Override
    public void registerListener() {
        PacketEvents.getAPI().getEventManager().registerListener(listener, PacketListenerPriority.LOWEST);
    }
}
