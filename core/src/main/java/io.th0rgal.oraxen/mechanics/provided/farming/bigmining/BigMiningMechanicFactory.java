package io.th0rgal.oraxen.mechanics.provided.farming.bigmining;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.mechanics.Mechanic;
import io.th0rgal.oraxen.mechanics.MechanicFactory;
import io.th0rgal.oraxen.mechanics.MechanicsManager;
import org.bukkit.configuration.ConfigurationSection;

public class BigMiningMechanicFactory extends MechanicFactory {

    private final boolean callEvents;

    public BigMiningMechanicFactory(ConfigurationSection section) {
        super(section);
        this.callEvents = section.getBoolean("call_events", true);
        MechanicsManager.registerListeners(OraxenPlugin.get(), getMechanicID(), new BigMiningMechanicListener(this));
    }

    @Override
    public Mechanic parse(ConfigurationSection itemMechanicConfiguration) {
        Mechanic mechanic = new BigMiningMechanic(this, itemMechanicConfiguration);
        addToImplemented(mechanic);
        return mechanic;
    }

    public boolean callEvents() {
        return callEvents;
    }

}
