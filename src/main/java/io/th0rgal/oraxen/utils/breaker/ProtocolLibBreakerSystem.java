package io.th0rgal.oraxen.utils.breaker;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.reflect.StructureModifier;
import com.comphenix.protocol.wrappers.BlockPosition;
import com.comphenix.protocol.wrappers.EnumWrappers;
import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.utils.SchedulerUtil;
import io.th0rgal.oraxen.utils.VersionUtil;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;

public class ProtocolLibBreakerSystem extends BreakerSystem {
    private final PacketAdapter listener = new PacketAdapter(OraxenPlugin.get(),
        ListenerPriority.LOW, PacketType.Play.Client.BLOCK_DIG) {
        @Override
        public void onPacketReceiving(final PacketEvent event) {
            final PacketContainer packet = event.getPacket();
            final Player player = event.getPlayer();

            final StructureModifier<BlockPosition> dataTemp = packet.getBlockPositionModifier();
            final StructureModifier<EnumWrappers.Direction> dataDirection = packet.getDirections();
            final StructureModifier<EnumWrappers.PlayerDigType> data = packet
                .getEnumModifier(EnumWrappers.PlayerDigType.class, 2);
            EnumWrappers.PlayerDigType type;
            try {
                type = data.getValues().getFirst();
            } catch (IllegalArgumentException exception) {
                type = EnumWrappers.PlayerDigType.SWAP_HELD_ITEMS;
            }

            final BlockPosition pos = dataTemp.getValues().getFirst();
            final BlockFace blockFace = dataDirection.size() > 0 ?
                BlockFace.valueOf(dataDirection.read(0).name()) :
                BlockFace.UP;
            final boolean startedDigging = type == EnumWrappers.PlayerDigType.START_DESTROY_BLOCK;
            final boolean finishedDigging = type == EnumWrappers.PlayerDigType.STOP_DESTROY_BLOCK;

            // ProtocolLib packet callbacks can run off the owning region thread. On Folia,
            // defer all Bukkit block access to the target block's region.
            if (VersionUtil.isFoliaServer()) {
                SchedulerUtil.runForEntity(player, () -> {
                    final World world = player.getWorld();
                    final Location location = new Location(world, pos.getX(), pos.getY(), pos.getZ());
                    SchedulerUtil.runAtLocation(location, () -> {
                        if (!world.isChunkLoaded(pos.getX() >> 4, pos.getZ() >> 4)) return;
                        final Block block = world.getBlockAt(pos.getX(), pos.getY(), pos.getZ());
                        handleEvent(player, block, location, blockFace, world, () -> {}, startedDigging, finishedDigging);
                    });
                }, null);
                return;
            }

            final World world = player.getWorld();
            if (!world.isChunkLoaded(pos.getX() >> 4, pos.getZ() >> 4)) return;
            final Block block = world.getBlockAt(pos.getX(), pos.getY(), pos.getZ());
            final Location location = block.getLocation();

            // Protocol action 2 is STOP_DESTROY_BLOCK, meaning the block was fully broken.
            // ABORT_DESTROY_BLOCK is action 1 and means the player released mid-dig.
            handleEvent(player, block, location, blockFace, world, () -> event.setCancelled(true),
                    startedDigging, finishedDigging);
        }
    };

    @Override
    protected void sendBlockBreak(final Player player, final Location location, final int stage) {
        final PacketContainer packet = ProtocolLibrary.getProtocolManager().createPacket(PacketType.Play.Server.BLOCK_BREAK_ANIMATION);
        packet.getIntegers().write(0, location.hashCode()).write(1, stage);
        packet.getBlockPositionModifier().write(0, new BlockPosition(location.toVector()));

        ProtocolLibrary.getProtocolManager().sendServerPacket(player, packet);
    }

    @Override
    public void registerListener() {
        ProtocolLibrary.getProtocolManager().addPacketListener(listener);
    }
}
