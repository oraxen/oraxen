package io.th0rgal.oraxen.mechanics.provided.misc.soulbound;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.mechanics.ConfigProperty;
import io.th0rgal.oraxen.mechanics.Mechanic;
import io.th0rgal.oraxen.mechanics.MechanicFactory;
import io.th0rgal.oraxen.mechanics.MechanicInfo;
import io.th0rgal.oraxen.mechanics.MechanicsManager;
import io.th0rgal.oraxen.mechanics.PropertyType;
import org.bukkit.configuration.ConfigurationSection;

@MechanicInfo(
        category = "misc",
        description = "Keeps the item in inventory on death instead of dropping"
)
public class SoulBoundMechanicFactory extends MechanicFactory {

    @ConfigProperty(type = PropertyType.DOUBLE, description = "Chance to lose item anyway (0-100%)", defaultValue = "0.0", min = 0.0, max = 100.0)
    public static final String PROP_LOSE_CHANCE = "lose_chance";

    public SoulBoundMechanicFactory(ConfigurationSection section) {
        super(section);
        MechanicsManager.registerListeners(OraxenPlugin.get(), getMechanicID(), new SoulBoundMechanicListener(this));
    }

    @Override
    public Mechanic parse(ConfigurationSection itemMechanicConfiguration) {
        Mechanic mechanic = new SoulBoundMechanic(this, itemMechanicConfiguration);
        addToImplemented(mechanic);
        return mechanic;
    }
}
