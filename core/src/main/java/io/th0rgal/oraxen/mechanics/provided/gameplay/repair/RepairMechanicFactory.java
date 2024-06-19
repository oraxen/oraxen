package io.th0rgal.oraxen.mechanics.provided.gameplay.repair;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.mechanics.Mechanic;
import io.th0rgal.oraxen.mechanics.MechanicFactory;
import io.th0rgal.oraxen.mechanics.MechanicsManager;
import org.bukkit.configuration.ConfigurationSection;

@Deprecated(forRemoval = true, since = "1.21")
public class RepairMechanicFactory extends MechanicFactory {

    private final boolean oraxenDurabilityOnly;

    public RepairMechanicFactory(ConfigurationSection section) {
        super(section);
        oraxenDurabilityOnly = section.getBoolean("oraxen_durability_only");
        MechanicsManager.registerListeners(OraxenPlugin.get(), getMechanicID(), new RepairMechanicListener(this));
    }

    @Override
    public Mechanic parse(ConfigurationSection itemMechanicConfiguration) {
        Mechanic mechanic = new RepairMechanic(this, itemMechanicConfiguration);
        addToImplemented(mechanic);
        return mechanic;
    }

    public boolean isOraxenDurabilityOnly() {
        return oraxenDurabilityOnly;
    }
}
