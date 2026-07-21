package io.th0rgal.oraxen.mechanics.provided.gameplay.repair;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.mechanics.ConfigProperty;
import io.th0rgal.oraxen.mechanics.Mechanic;
import io.th0rgal.oraxen.mechanics.MechanicFactory;
import io.th0rgal.oraxen.mechanics.MechanicInfo;
import io.th0rgal.oraxen.mechanics.MechanicsManager;
import io.th0rgal.oraxen.mechanics.PropertyType;
import org.bukkit.configuration.ConfigurationSection;

@Deprecated(forRemoval = true, since = "1.21")
@MechanicInfo(
        category = "gameplay",
        description = "Allows items to be repaired with custom materials (deprecated on 1.21+, use repaired_by component)"
)
public class RepairMechanicFactory extends MechanicFactory {

    @ConfigProperty(type = PropertyType.DOUBLE, description = "Repair ratio per material consumed", defaultValue = "0.25", min = 0.0, max = 1.0)
    public static final String PROP_RATIO = "ratio";

    @ConfigProperty(type = PropertyType.STRING, description = "Oraxen item ID to use as repair material")
    public static final String PROP_ORAXEN_ITEM = "oraxen_item";

    @ConfigProperty(type = PropertyType.LIST, description = "Fixed repair amounts per material")
    public static final String PROP_FIXED_REPAIR_AMOUNT = "fixed_repair_amount";

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
