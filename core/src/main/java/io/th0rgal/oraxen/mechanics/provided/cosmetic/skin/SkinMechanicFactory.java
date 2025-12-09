package io.th0rgal.oraxen.mechanics.provided.cosmetic.skin;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.mechanics.ConfigProperty;
import io.th0rgal.oraxen.mechanics.Mechanic;
import io.th0rgal.oraxen.mechanics.MechanicFactory;
import io.th0rgal.oraxen.mechanics.MechanicInfo;
import io.th0rgal.oraxen.mechanics.MechanicsManager;
import io.th0rgal.oraxen.mechanics.PropertyType;
import org.bukkit.configuration.ConfigurationSection;

@MechanicInfo(
        category = "cosmetic",
        description = "Defines an item as a skin that can be applied to skinnable items"
)
public class SkinMechanicFactory extends MechanicFactory {

    @ConfigProperty(type = PropertyType.BOOLEAN, description = "Whether the skin is consumed when applied", defaultValue = "true")
    public static final String PROP_CONSUME = "consume";

    public SkinMechanicFactory(ConfigurationSection section) {
        super(section);
        MechanicsManager.registerListeners(OraxenPlugin.get(), getMechanicID(), new SkinMechanicListener(this));
    }

    @Override
    public Mechanic parse(ConfigurationSection itemMechanicConfiguration) {
        Mechanic mechanic = new SkinMechanic(this, itemMechanicConfiguration);
        addToImplemented(mechanic);
        return mechanic;
    }
}
