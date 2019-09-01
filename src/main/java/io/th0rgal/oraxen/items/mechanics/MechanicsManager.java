package io.th0rgal.oraxen.items.mechanics;

import io.th0rgal.oraxen.items.mechanics.provided.durability.DurabilityMechanic;
import io.th0rgal.oraxen.settings.Message;
import io.th0rgal.oraxen.utils.Logs;
import org.bukkit.configuration.ConfigurationSection;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MechanicsManager {

    private static Map<String, Class<?>> mechanicsClassByID = new HashMap<>();

    public static void register(String id, Class<?> clazz) {
        mechanicsClassByID.put(id, clazz);
    }

    public static void registerNativeMechanics() {
        register("Durability", DurabilityMechanic.class);
    }

    private static Map<String, List<Mechanic>> mechanicsByItemID = new HashMap<>();

    public static void addItemMechanic(String itemID, ConfigurationSection mechanicSection) {

        String mechanicID = mechanicSection.getName();

        if (!mechanicsClassByID.containsKey(mechanicID)) {
            Logs.logInfo(Message.MECHANIC_DOESNT_EXIST.toString().replace("{mechanic}", mechanicID));
            return;
        }

        Class<?>  mechanicClass = mechanicsClassByID.get(mechanicID);
        try {
            //Mechanic constructor will automatically call addItemMechanic with Mechanic object
            Mechanic mechanic = (Mechanic)mechanicClass.getConstructor(ConfigurationSection.class).newInstance(mechanicSection);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            e.printStackTrace();
        }

    }

    //will get called automatically
    public static void addItemMechanic(String itemID, Mechanic mechanic) {
        List<Mechanic> itemMechanics = mechanicsByItemID.getOrDefault(itemID, new ArrayList<>());
        itemMechanics.add(mechanic);
        mechanicsByItemID.put(itemID, itemMechanics);
    }

    public List<Mechanic> getMechanicsByItemID(String itemID) {
        return mechanicsByItemID.getOrDefault(itemID, new ArrayList<>());
    }


}
