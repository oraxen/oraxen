package io.th0rgal.oraxen.mechanics.provided.combat.spell.fireball;

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
        description = "Launches a fireball projectile that explodes on impact"
)
public class FireballMechanicFactory extends MechanicFactory {

    @ConfigProperty(type = PropertyType.INTEGER, description = "Cooldown between uses in milliseconds", defaultValue = "0", min = 0)
    public static final String PROP_DELAY = "delay";

    @ConfigProperty(type = PropertyType.DOUBLE, description = "Explosion power/radius")
    public static final String PROP_YIELD = "yield";

    @ConfigProperty(type = PropertyType.DOUBLE, description = "Fireball travel speed")
    public static final String PROP_SPEED = "speed";

    public FireballMechanicFactory(ConfigurationSection section) {
        super(section);
        MechanicsManager.registerListeners(OraxenPlugin.get(), getMechanicID(), new FireballMechanicManager(this));
    }

    @Override
    public Mechanic parse(ConfigurationSection itemMechanicConfiguration) {
        Mechanic mechanic = new FireballMechanic(this, itemMechanicConfiguration);
        addToImplemented(mechanic);
        return mechanic;
    }
}
