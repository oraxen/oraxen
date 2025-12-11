package io.th0rgal.oraxen.mechanics.provided.gameplay.togglelight;

import io.th0rgal.oraxen.api.OraxenFurniture;
import io.th0rgal.oraxen.api.events.OraxenNativeMechanicsRegisteredEvent;
import io.th0rgal.oraxen.mechanics.Mechanic;
import io.th0rgal.oraxen.mechanics.MechanicFactory;
import io.th0rgal.oraxen.mechanics.MechanicsManager;
import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.FurnitureMechanic;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.EventPriority;

public class ToggleLightMechanicFactory extends MechanicFactory {

    private static ToggleLightMechanicFactory instance;

    public ToggleLightMechanicFactory(ConfigurationSection section) {
        super(section);
        instance = this;
        MechanicsManager.registerListeners(OraxenPlugin.get(), getMechanicID(), new ToggleLightMechanicListener());
        MechanicsManager.registerListeners(OraxenPlugin.get(), getMechanicID(), new ToggleLightRefreshListener());
    }

    public static ToggleLightMechanicFactory getInstance() {
        return instance;
    }

    @Override
    public Mechanic parse(ConfigurationSection itemMechanicConfiguration) {
        ToggleLightMechanic mechanic = new ToggleLightMechanic(this, itemMechanicConfiguration);
        addToImplemented(mechanic);
        return mechanic;
    }

    @Override
    public ToggleLightMechanic getMechanic(String itemID) {
        return (ToggleLightMechanic) super.getMechanic(itemID);
    }
}

