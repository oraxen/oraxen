package io.th0rgal.oraxen.mechanics.provided.farming.watering;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.mechanics.Mechanic;
import io.th0rgal.oraxen.mechanics.MechanicConfigProperty;
import io.th0rgal.oraxen.mechanics.MechanicFactory;
import io.th0rgal.oraxen.mechanics.MechanicsManager;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class WateringMechanicFactory extends MechanicFactory {

    private static WateringMechanicFactory instance;

    public WateringMechanicFactory(ConfigurationSection section) {
        super(section);
        MechanicsManager.registerListeners(OraxenPlugin.get(), getMechanicID(), new WateringMechanicListener(this));
        instance = this;
    }

    @Override
    public Mechanic parse(ConfigurationSection itemMechanicConfiguration) {
        Mechanic mechanic = new WateringMechanic(this, itemMechanicConfiguration);
        addToImplemented(mechanic);
        return mechanic;
    }

    public static WateringMechanicFactory get() { return instance; }

    @Override
    public @Nullable String getMechanicCategory() {
        return "farming";
    }

    @Override
    public @Nullable String getMechanicDescription() {
        return "Waters farmland blocks to hydrate them";
    }

    @Override
    public @NotNull List<MechanicConfigProperty> getConfigSchema() {
        return List.of();
    }
}
