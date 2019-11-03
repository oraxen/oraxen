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
import io.th0rgal.oraxen.mechanics.Mechanic;
import io.th0rgal.oraxen.mechanics.MechanicFactory;

import io.th0rgal.oraxen.mechanics.MechanicsManager;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;

import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.function.Consumer;

public class BedrockbreakMechanicFactory extends MechanicFactory {

    private boolean disabledOnFirstLayer;
    private int durabilityCost;

    public BedrockbreakMechanicFactory(ConfigurationSection section) {
        super(section);
        disabledOnFirstLayer = section.getBoolean("disable_on_first_layer");
        durabilityCost = section.getInt("durability_cost");
        MechanicsManager.registerListeners(OraxenPlugin.get(), new BedrockbreakMechanicsManager(this));
    }

    @Override
    public Mechanic parse(ConfigurationSection itemMechanicConfiguration) {
        Mechanic mechanic = new BedrockbreakMechanic(this, itemMechanicConfiguration);
        addToImplemented(mechanic);
        return mechanic;
    }

    public boolean isDisabledOnFirstLayer() {
        return disabledOnFirstLayer;
    }

    public int getDurabilityCost() {
        return durabilityCost;
    }
}
