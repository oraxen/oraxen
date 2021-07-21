package io.th0rgal.oraxen.utils.breaker;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.reflect.StructureModifier;
import com.comphenix.protocol.wrappers.BlockPosition;
import com.comphenix.protocol.wrappers.EnumWrappers;
import io.th0rgal.oraxen.OraxenPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class BreakerSystem {

    private final Map<Location, BukkitScheduler> breakerPerLocation = new HashMap<>();
    public static final List<HardnessModifier> MODIFIERS = new ArrayList<>();
    private final ProtocolManager protocolManager;

    public BreakerSystem() {
        this.protocolManager = ProtocolLibrary.getProtocolManager();
    }

    private void sendBlockBreak(Player player, Location location, int stage) {
        PacketContainer fakeAnimation = protocolManager.createPacket(PacketType.Play.Server.BLOCK_BREAK_ANIMATION);
        fakeAnimation.getIntegers().write(0, player.getEntityId() + 1).write(1, stage);
        fakeAnimation.getBlockPositionModifier().write(0, new BlockPosition(location.toVector()));
        try {
            protocolManager.sendServerPacket(player, fakeAnimation);
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
    }

    public void registerListener() {
        protocolManager
                .addPacketListener(
                        new PacketAdapter(OraxenPlugin.get(), ListenerPriority.LOW, PacketType.Play.Client.BLOCK_DIG) {
                            @Override
                            public void onPacketReceiving(PacketEvent event) {
                                PacketContainer packet = event.getPacket();
                                Player player = event.getPlayer();
                                ItemStack item = player.getInventory().getItemInMainHand();

                                StructureModifier<BlockPosition> dataTemp = packet.getBlockPositionModifier();
                                StructureModifier<EnumWrappers.PlayerDigType> data = packet
                                        .getEnumModifier(EnumWrappers.PlayerDigType.class, 2);

                                EnumWrappers.PlayerDigType type = data.getValues().get(0);

                                BlockPosition pos = dataTemp.getValues().get(0);
                                World world = player.getWorld();
                                Block block = world.getBlockAt(pos.getX(), pos.getY(), pos.getZ());

                                HardnessModifier triggeredModifier = null;
                                for (HardnessModifier modifier : MODIFIERS)
                                    if (modifier.isTriggered(player, block, item)) {
                                        triggeredModifier = modifier;
                                        break;
                                    }
                                if (triggeredModifier == null)
                                    return;
                                event.setCancelled(true);

                                Location location = block.getLocation();
                                if (type == EnumWrappers.PlayerDigType.START_DESTROY_BLOCK) {

                                    long period = triggeredModifier.getPeriod(player, block, item);

                                    if (breakerPerLocation.containsKey(location))
                                        breakerPerLocation.get(location).cancelTasks(OraxenPlugin.get());
                                    BukkitScheduler scheduler = Bukkit.getScheduler();
                                    breakerPerLocation.put(location, scheduler);

                                    HardnessModifier modifier = triggeredModifier;
                                    scheduler.runTaskTimer(OraxenPlugin.get(), new Consumer<>() {
                                        int value = 0;

                                        @Override
                                        public void accept(BukkitTask bukkitTask) {
                                            if (!breakerPerLocation.containsKey(location)) {
                                                bukkitTask.cancel();
                                                return;
                                            }
                                            value += 1;
                                            for (Entity entity : world.getNearbyEntities(location, 16, 16, 16))
                                                if (entity instanceof Player viewer)
                                                    sendBlockBreak(viewer, location, value);

                                            if (value < 10)
                                                return;

                                            BlockBreakEvent blockBreakEvent = new BlockBreakEvent(block, player);
                                            Bukkit.getPluginManager().callEvent(blockBreakEvent);
                                            if (!blockBreakEvent.isCancelled())
                                                modifier.breakBlock(player, block, item);

                                            bukkitTask.cancel();
                                        }
                                    }, period, period);

                                } else {
                                    breakerPerLocation.remove(location);
                                    sendBlockBreak(player, location, 10);
                                }
                            }
                        });
    }


}
