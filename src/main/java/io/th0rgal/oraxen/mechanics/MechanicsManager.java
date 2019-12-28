package io.th0rgal.oraxen.mechanics;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.mechanics.provided.bedrockbreak.BedrockBreakMechanicFactory;
import io.th0rgal.oraxen.mechanics.provided.bigmining.BigMiningMechanicFactory;
import io.th0rgal.oraxen.mechanics.provided.block.BlockMechanicFactory;
import io.th0rgal.oraxen.mechanics.provided.bottledexp.BottledExpMechanicFactory;
import io.th0rgal.oraxen.mechanics.provided.commands.CommandsMechanicFactory;
import io.th0rgal.oraxen.mechanics.provided.durability.DurabilityMechanicFactory;
import io.th0rgal.oraxen.mechanics.provided.hat.HatMechanicFactory;
import io.th0rgal.oraxen.mechanics.provided.thor.ThorMechanicFactory;
import io.th0rgal.oraxen.settings.ResourcesManager;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.InvocationTargetException;
import java.util.*;

public class MechanicsManager {

    private static final Map<String, MechanicFactory> FACTORIES_BY_MECHANIC_ID = new HashMap<>();
    private static final List<Listener> MECHANICS_LISTENERS = new ArrayList<>();

    public static void registerNativeMechanics() {
        registerMechanicFactory("durability", DurabilityMechanicFactory.class);
        registerMechanicFactory("commands", CommandsMechanicFactory.class);
        registerMechanicFactory("block", BlockMechanicFactory.class);
        registerMechanicFactory("hat", HatMechanicFactory.class);

        registerMechanicFactory("thor", ThorMechanicFactory.class);
        registerMechanicFactory("bigmining", BigMiningMechanicFactory.class);
        registerMechanicFactory("bottledexp", BottledExpMechanicFactory.class);
        registerMechanicFactory("bedrockbreak", BedrockBreakMechanicFactory.class);
    }

    public static void registerMechanicFactory(String mechanicID, Class<? extends MechanicFactory> mechanicFactoryClass) {
        YamlConfiguration mechanicsConfig = new ResourcesManager(OraxenPlugin.get()).getMechanics();
        if (mechanicsConfig.getKeys(false).contains(mechanicID)) {
            ConfigurationSection factorySection = mechanicsConfig.getConfigurationSection(mechanicID);
            if (factorySection.getBoolean("enabled"))
                try {
                    MechanicFactory factory = mechanicFactoryClass.getConstructor(ConfigurationSection.class).newInstance(factorySection);
                    FACTORIES_BY_MECHANIC_ID.put(mechanicID, factory);
                } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                    e.printStackTrace();
                }
        }
    }

    public static void registerListeners(JavaPlugin plugin, Listener... listeners) {
        for (Listener listener : listeners) {
            Bukkit.getPluginManager().registerEvents(listener, plugin);
            MECHANICS_LISTENERS.add(listener);
        }
    }

    public static void unloadListeners() {
        for (Listener listener : MECHANICS_LISTENERS)
            HandlerList.unregisterAll(listener);
    }

    public static MechanicFactory getMechanicFactory(String mechanicID) {
        return FACTORIES_BY_MECHANIC_ID.get(mechanicID);
    }

}
