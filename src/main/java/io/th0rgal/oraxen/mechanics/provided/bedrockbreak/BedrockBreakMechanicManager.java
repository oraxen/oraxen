package io.th0rgal.oraxen.mechanics.provided.bedrockbreak;

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
import io.th0rgal.oraxen.items.OraxenItems;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class BedrockBreakMechanicManager {

    private final Map<Location, BukkitScheduler> breakerPerLocation = new HashMap<>();
    private final ProtocolManager protocolManager;

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

    public BedrockBreakMechanicManager(BedrockBreakMechanicFactory factory) {

        this.protocolManager = ProtocolLibrary.getProtocolManager();
        protocolManager
                .addPacketListener(
                        new PacketAdapter(OraxenPlugin.get(), ListenerPriority.LOW, PacketType.Play.Client.BLOCK_DIG) {
                            @Override
                            public void onPacketReceiving(PacketEvent event) {
                                PacketContainer packet = event.getPacket();
                                Player player = event.getPlayer();
                                ItemStack item = player.getInventory().getItemInMainHand();
                                String itemID = OraxenItems.getIdByItem(item);
                                if (factory.isNotImplementedIn(itemID))
                                    return;

                                StructureModifier<BlockPosition> dataTemp = packet.getBlockPositionModifier();
                                StructureModifier<EnumWrappers.PlayerDigType> data = packet
                                        .getEnumModifier(EnumWrappers.PlayerDigType.class, 2);

                                EnumWrappers.PlayerDigType type = data.getValues().get(0);

                                BlockPosition pos = dataTemp.getValues().get(0);
                                World world = player.getWorld();
                                Block block = world.getBlockAt(pos.getX(), pos.getY(), pos.getZ());
                                if (!block.getType().equals(Material.BEDROCK))
                                    return;

                                BedrockBreakMechanic mechanic = (BedrockBreakMechanic) factory.getMechanic(itemID);

                                Location location = block.getLocation();
                                BedrockBreakMechanicFactory factory =
                                        (BedrockBreakMechanicFactory) mechanic.getFactory();
                                if (factory.isDisabledOnFirstLayer() && location.getBlockY() == 0)
                                    return;
                                if (type == EnumWrappers.PlayerDigType.START_DESTROY_BLOCK) {

                                    if (breakerPerLocation.containsKey(location))
                                        breakerPerLocation.get(location).cancelTasks(OraxenPlugin.get());
                                    BukkitScheduler scheduler = Bukkit.getScheduler();
                                    breakerPerLocation.put(location, scheduler);

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
                                            if (!blockBreakEvent.isCancelled()) {
                                                if (mechanic.bernouilliTest())
                                                    world.dropItemNaturally(location, new ItemStack(Material.BEDROCK));
                                                world.playSound(location, Sound.ENTITY_WITHER_BREAK_BLOCK, 1F, 0.05F);
                                                world
                                                        .spawnParticle(Particle.BLOCK_CRACK,
                                                                location, 25, 0.5D, 0.5D, 0.5D,
                                                                block.getBlockData());
                                                block.breakNaturally();
                                            }

                                            PlayerItemDamageEvent playerItemDamageEvent = new PlayerItemDamageEvent(
                                                    player,
                                                    item,
                                                    factory.getDurabilityCost());
                                            Bukkit.getPluginManager().callEvent(playerItemDamageEvent);

                                            bukkitTask.cancel();
                                        }
                                    }, mechanic.delay, mechanic.period);

                                } else {
                                    breakerPerLocation.remove(location);
                                    sendBlockBreak(player, location, 10);
                                }
                            }
                        });
    }

}
