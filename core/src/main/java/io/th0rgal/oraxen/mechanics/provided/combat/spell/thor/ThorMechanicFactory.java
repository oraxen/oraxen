package io.th0rgal.oraxen.mechanics.provided.combat.spell.thor;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.mechanics.ConfigProperty;
import io.th0rgal.oraxen.mechanics.Mechanic;
import io.th0rgal.oraxen.mechanics.MechanicFactory;
import io.th0rgal.oraxen.mechanics.MechanicInfo;
import io.th0rgal.oraxen.mechanics.MechanicsManager;
import io.th0rgal.oraxen.mechanics.PropertyType;
import org.bukkit.configuration.ConfigurationSection;

@MechanicInfo(
        category = "combat",
        description = "Summons lightning bolts on the target location when attacking"
)
public class ThorMechanicFactory extends MechanicFactory {

    @ConfigProperty(type = PropertyType.INTEGER, description = "Number of lightning bolts to summon", defaultValue = "1", min = 1)
    public static final String PROP_LIGHTNING_BOLTS_AMOUNT = "lightning_bolts_amount";

    @ConfigProperty(type = PropertyType.DOUBLE, description = "Random offset for lightning positions", defaultValue = "1.5", min = 0.0, max = 10.0)
    public static final String PROP_RANDOM_LOCATION_VARIATION = "random_location_variation";

    @ConfigProperty(type = PropertyType.INTEGER, description = "Cooldown between uses in milliseconds", defaultValue = "0", min = 0)
    public static final String PROP_DELAY = "delay";

    public ThorMechanicFactory(ConfigurationSection section) {
        super(section);
        MechanicsManager.registerListeners(OraxenPlugin.get(), getMechanicID(), new ThorMechanicListener(this));
    }

    @Override
    public Mechanic parse(ConfigurationSection itemMechanicConfiguration) {
        Mechanic mechanic = new ThorMechanic(this, itemMechanicConfiguration);
        addToImplemented(mechanic);
        return mechanic;
    }
}
