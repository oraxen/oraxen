package io.th0rgal.oraxen.mechanics.provided.gameplay.efficiency;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.mechanics.Mechanic;
import io.th0rgal.oraxen.mechanics.MechanicConfigProperty;
import io.th0rgal.oraxen.mechanics.MechanicFactory;
import io.th0rgal.oraxen.mechanics.MechanicsManager;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class EfficiencyMechanicFactory extends MechanicFactory {

    private final EfficiencyMechanicListener listener;

    public EfficiencyMechanicFactory(ConfigurationSection section) {
        super(section);
        listener = new EfficiencyMechanicListener(this);
        MechanicsManager.registerListeners(OraxenPlugin.get(), getMechanicID(), listener);
    }

    @Override
    public void onUnregister() {
        listener.clearAll();
    }

    @Override
    public Mechanic parse(ConfigurationSection itemMechanicConfiguration) {
        Mechanic mechanic = new EfficiencyMechanic(this, itemMechanicConfiguration);
        addToImplemented(mechanic);
        return mechanic;
    }

    public EfficiencyMechanicFactory getInstance() {
        return this;
    }

    @Override
    public @Nullable String getMechanicCategory() {
        return "gameplay";
    }

    @Override
    public @Nullable String getMechanicDescription() {
        return "Modifies mining speed for tools beyond vanilla efficiency";
    }

    @Override
    public @NotNull List<MechanicConfigProperty> getConfigSchema() {
        return List.of(
                MechanicConfigProperty.decimal("amount", "Efficiency modifier amount")
        );
    }
}
