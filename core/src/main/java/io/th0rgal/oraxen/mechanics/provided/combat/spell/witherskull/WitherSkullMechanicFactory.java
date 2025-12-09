package io.th0rgal.oraxen.mechanics.provided.combat.spell.witherskull;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.mechanics.ConfigProperty;
import io.th0rgal.oraxen.mechanics.Mechanic;
import io.th0rgal.oraxen.mechanics.MechanicFactory;
import io.th0rgal.oraxen.mechanics.MechanicInfo;
import io.th0rgal.oraxen.mechanics.MechanicsManager;
import io.th0rgal.oraxen.mechanics.PropertyType;
import org.bukkit.configuration.ConfigurationSection;

@MechanicInfo(category = "combat", description = "Launches a wither skull projectile that applies wither effect")
public class WitherSkullMechanicFactory extends MechanicFactory {

    @ConfigProperty(type = PropertyType.INTEGER, description = "Cooldown between uses in milliseconds", defaultValue = "0", min = 0)
    public static final String PROP_DELAY = "delay";

    @ConfigProperty(type = PropertyType.BOOLEAN, description = "Whether the skull is charged (blue, more powerful)", defaultValue = "false")
    public static final String PROP_CHARGED = "charged";

    public WitherSkullMechanicFactory(ConfigurationSection section) {
        super(section);
        MechanicsManager.registerListeners(OraxenPlugin.get(), getMechanicID(), new WitherSkullMechanicListener(this));
    }

    @Override
    public Mechanic parse(ConfigurationSection itemMechanicConfiguration) {
        Mechanic mechanic = new WitherSkullMechanic(this, itemMechanicConfiguration);
        addToImplemented(mechanic);
        return mechanic;
    }
}
