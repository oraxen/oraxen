package io.th0rgal.oraxen.mechanics.provided.gameplay.efficiency;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.mechanics.Mechanic;
import io.th0rgal.oraxen.mechanics.MechanicFactory;
import org.bukkit.configuration.ConfigurationSection;

public class EfficiencyMechanicFactory extends MechanicFactory {

    public EfficiencyMechanicFactory(ConfigurationSection section) {
        super(section);
        OraxenPlugin.get().getPacketAdapter().whenEnabled(adapter -> {
            adapter.reregisterEfficencyMechanicListener(this);
        });
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

}
