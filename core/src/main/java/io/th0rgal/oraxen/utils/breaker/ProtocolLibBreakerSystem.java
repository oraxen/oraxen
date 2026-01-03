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
import io.th0rgal.oraxen.api.OraxenBlocks;
import io.th0rgal.oraxen.api.OraxenFurniture;
import io.th0rgal.oraxen.mechanics.provided.gameplay.noteblock.NoteBlockMechanic;
import io.th0rgal.oraxen.mechanics.provided.gameplay.stringblock.StringBlockMechanic;
import io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.FurnitureMechanic;
import io.th0rgal.oraxen.utils.SchedulerUtil;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class ProtocolLibBreakerSystem extends BreakerSystem {
    private final PacketAdapter listener = new PacketAdapter(OraxenPlugin.get(),
        ListenerPriority.LOW, PacketType.Play.Client.BLOCK_DIG) {
        @Override
        public void onPacketReceiving(final PacketEvent event) {
            final PacketContainer packet = event.getPacket();
            final Player player = event.getPlayer();
            final ItemStack item = player.getInventory().getItemInMainHand();
            if (player.getGameMode() == GameMode.CREATIVE) return;

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
            final World world = player.getWorld();
            final Location location = new Location(world, pos.getX(), pos.getY(), pos.getZ());
            final BlockFace blockFace = dataDirection.size() > 0 ?
                BlockFace.valueOf(dataDirection.read(0).name()) :
                BlockFace.UP;

            final EnumWrappers.PlayerDigType digType = type;

            final boolean isCustomBlock = shouldHandleBlock(location.getBlock());
            if (isCustomBlock) {
                event.setCancelled(true);
            }

            SchedulerUtil.runAtLocation(location, () -> {
                final Block block = location.getBlock();
                if (shouldHandleBlock(block)) {
                    handleEvent(
                        player,
                        block,
                        location,
                        blockFace,
                        world,
                        () -> {},
                        digType == EnumWrappers.PlayerDigType.START_DESTROY_BLOCK
                    );
                }
            });
        }
    };

    private boolean shouldHandleBlock(Block block) {
        return OraxenBlocks.getNoteBlockMechanic(block) != null
            || OraxenBlocks.getStringMechanic(block) != null
            || OraxenFurniture.getFurnitureMechanic(block) != null
            || block.getType() == Material.CHORUS_PLANT
            || block.getType() == Material.BEDROCK;
    }

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
