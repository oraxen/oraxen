package io.th0rgal.oraxen.mechanics.provided.combat.spell.energyblast;

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
        description = "Fires an energy blast projectile that damages entities in its path"
)
public class EnergyBlastMechanicFactory extends MechanicFactory {

    @ConfigProperty(type = PropertyType.INTEGER, description = "Cooldown between uses in milliseconds", defaultValue = "0", min = 0)
    public static final String PROP_DELAY = "delay";

    @ConfigProperty(type = PropertyType.DOUBLE, description = "Damage dealt to entities hit")
    public static final String PROP_DAMAGE = "damage";

    @ConfigProperty(type = PropertyType.INTEGER, description = "Length/range of the energy blast")
    public static final String PROP_LENGTH = "length";

    public EnergyBlastMechanicFactory(ConfigurationSection section) {
        super(section);
        MechanicsManager.registerListeners(OraxenPlugin.get(), getMechanicID(), new EnergyBlastMechanicManager(this));
    }

    @Override
    public Mechanic parse(ConfigurationSection itemMechanicConfiguration) {
        Mechanic mechanic = new EnergyBlastMechanic(this, itemMechanicConfiguration);
        addToImplemented(mechanic);
        return mechanic;
    }
}
