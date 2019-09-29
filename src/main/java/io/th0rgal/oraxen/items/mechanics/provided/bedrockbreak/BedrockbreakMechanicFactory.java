package io.th0rgal.oraxen.items.mechanics.provided.bedrockbreak;

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
import io.th0rgal.oraxen.items.mechanics.Mechanic;
import io.th0rgal.oraxen.items.mechanics.MechanicFactory;
import io.th0rgal.oraxen.listeners.EventsManager;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;

import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.function.Consumer;

public class BedrockbreakMechanicFactory extends MechanicFactory {

    public BedrockbreakMechanicFactory(ConfigurationSection section) {
        super(section);
        new EventsManager(OraxenPlugin.get()).addEvents(new BedrockbreakMechanicsManager(this));
    }

    @Override
    public Mechanic parse(ConfigurationSection itemMechanicConfiguration) {
        Mechanic mechanic = new BedrockbreakMechanic(this, itemMechanicConfiguration);
        addToImplemented(mechanic);
        return mechanic;
    }

}

class BedrockbreakMechanicsManager implements Listener {

    private Set<Location> locations = new HashSet<>();
    private ProtocolManager protocolManager;


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

    public BedrockbreakMechanicsManager(BedrockbreakMechanicFactory factory) {

        this.protocolManager = ProtocolLibrary.getProtocolManager();
        protocolManager.addPacketListener(new PacketAdapter(OraxenPlugin.get(), ListenerPriority.HIGH, PacketType.Play.Client.BLOCK_DIG) {
            @Override
            public void onPacketReceiving(PacketEvent event) {
                PacketContainer packet = event.getPacket();
                Player player = event.getPlayer();
                ItemStack item = player.getInventory().getItemInMainHand();
                String itemID = OraxenItems.getIdByItem(item);
                if (factory.isNotImplementedIn(itemID))
                    return;

                StructureModifier<BlockPosition> dataTemp = packet.getBlockPositionModifier();
                StructureModifier<EnumWrappers.PlayerDigType> data = packet.getEnumModifier(EnumWrappers.PlayerDigType.class, 2);

                EnumWrappers.PlayerDigType type = data.getValues().get(0);

                BlockPosition pos = dataTemp.getValues().get(0);
                Block block = player.getWorld().getBlockAt(pos.getX(), pos.getY(), pos.getZ());
                if (!block.getType().equals(Material.BEDROCK))
                    return;

                BedrockbreakMechanic mechanic = (BedrockbreakMechanic) factory.getMechanic(itemID);
                Location location = block.getLocation();

                if (type == EnumWrappers.PlayerDigType.START_DESTROY_BLOCK) {
                    locations.add(location);

                    BukkitScheduler scheduler = Bukkit.getScheduler();
                    scheduler.runTaskTimer(OraxenPlugin.get(), new Consumer<BukkitTask>() {
                        int value = 0;

                        @Override
                        public void accept(BukkitTask bukkitTask) {
                            if (!locations.contains(location)) {
                                bukkitTask.cancel();
                                return;
                            }
                            value += 1;

                            sendBlockBreak(player, location, value);

                            if (value >= 10) {
                                if (mechanic.bernouilliTest())
                                    player.getWorld().dropItemNaturally(location, new ItemStack(Material.BEDROCK));
                                block.breakNaturally();

                                bukkitTask.cancel();
                            }
                        }
                    }, 20L, 20L);

                } else {
                    locations.remove(location);
                    sendBlockBreak(player, location, 10);
                }
            }
        });
    }

}

class BedrockbreakMechanic extends Mechanic {

    long delay;
    long period;
    int probability;

    public BedrockbreakMechanic(MechanicFactory mechanicFactory, ConfigurationSection section) {
        /* We give:
        - an instance of the Factory which created the mechanic
        - the section used to configure the mechanic
         */
        super(mechanicFactory, section);
        this.delay = section.getLong("delay");
        this.period = section.getLong("period");
        this.probability = (int) (1D / section.getDouble("probability"));
    }

    public long getPeriod() {
        return period;
    }

    public boolean bernouilliTest() {
        return new Random().nextInt(probability) == 0;
    }
}