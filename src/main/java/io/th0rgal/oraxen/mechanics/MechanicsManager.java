package io.th0rgal.oraxen.mechanics;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.mechanics.provided.bedrockbreak.BedrockbreakMechanicFactory;
import io.th0rgal.oraxen.mechanics.provided.durability.DurabilityMechanicFactory;
import io.th0rgal.oraxen.settings.ResourcesManager;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.lang.reflect.InvocationTargetException;
import java.util.*;

public class MechanicsManager {

    private static Map<String, MechanicFactory> factoriesByMechanicID = new HashMap<>();

    public static void registerNativeMechanics() {
        registerMechanicFactory("durability", DurabilityMechanicFactory.class);
        registerMechanicFactory("bedrockbreak", BedrockbreakMechanicFactory.class);
    }

    public static void registerMechanicFactory(String mechanicID, Class<? extends MechanicFactory> mechanicFactoryClass) {
        YamlConfiguration mechanicsConfig = new ResourcesManager(OraxenPlugin.get()).getMechanics();
        if (mechanicsConfig.getKeys(false).contains(mechanicID)) {
            ConfigurationSection factorySection = mechanicsConfig.getConfigurationSection(mechanicID);
            if (mechanicsConfig.getBoolean("durability.enabled"))
                try {
                    MechanicFactory factory = mechanicFactoryClass.getConstructor(ConfigurationSection.class).newInstance(factorySection);
                    factoriesByMechanicID.put(mechanicID, factory);
                } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                    e.printStackTrace();
                }
        }
    }

    public static MechanicFactory getMechanicFactory(String mechanicID) {
        return factoriesByMechanicID.get(mechanicID);
    }

}
