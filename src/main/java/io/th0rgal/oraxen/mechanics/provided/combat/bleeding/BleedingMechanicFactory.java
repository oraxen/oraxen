package io.th0rgal.oraxen.mechanics.provided.combat.bleeding;

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
        description = "Causes targets to bleed over time, dealing damage at intervals"
)
public class BleedingMechanicFactory extends MechanicFactory {

    @ConfigProperty(type = PropertyType.DOUBLE, description = "Probability of causing bleeding (0.0-1.0)", defaultValue = "0.3", min = 0.0, max = 1.0)
    public static final String PROP_CHANCE = "chance";

    @ConfigProperty(type = PropertyType.INTEGER, description = "Total duration of bleeding effect in ticks", defaultValue = "100", min = 1)
    public static final String PROP_DURATION = "duration";

    @ConfigProperty(type = PropertyType.DOUBLE, description = "Damage dealt each interval (in half-hearts)", defaultValue = "0.5", min = 0.0, max = 20.0)
    public static final String PROP_DAMAGE_PER_INTERVAL = "damage_per_interval";

    @ConfigProperty(type = PropertyType.INTEGER, description = "Ticks between each damage tick", defaultValue = "20", min = 1)
    public static final String PROP_INTERVAL = "interval";

    public BleedingMechanicFactory(ConfigurationSection section) {
        super(section);
        MechanicsManager.registerListeners(OraxenPlugin.get(), getMechanicID(), new BleedingMechanicListener(this));
    }

    @Override
    public Mechanic parse(ConfigurationSection itemMechanicConfiguration) {
        Mechanic mechanic = new BleedingMechanic(this, itemMechanicConfiguration);
        addToImplemented(mechanic);
        return mechanic;
    }
}