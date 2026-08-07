package io.th0rgal.oraxen.mechanics.provided.misc.consumablepotioneffects;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.mechanics.ConfigProperty;
import io.th0rgal.oraxen.mechanics.Mechanic;
import io.th0rgal.oraxen.mechanics.MechanicFactory;
import io.th0rgal.oraxen.mechanics.MechanicInfo;
import io.th0rgal.oraxen.mechanics.MechanicsManager;
import io.th0rgal.oraxen.mechanics.NestedProperty;
import io.th0rgal.oraxen.mechanics.PropertyType;
import org.bukkit.configuration.ConfigurationSection;

@MechanicInfo(
        category = "misc",
        description = "Applies potion effects when the item is consumed"
)
public class ConsumablePotionEffectsFactory extends MechanicFactory {

    @ConfigProperty(type = PropertyType.LIST, description = "List of potion effects to apply on consumption")
    public static final String PROP_EFFECTS = "effects";

    public ConsumablePotionEffectsFactory(ConfigurationSection section) {
        super(section);
        MechanicsManager.registerListeners(OraxenPlugin.get(), getMechanicID(), new ConsumablePotionEffectsListener(this));
    }

    @Override
    public Mechanic parse(ConfigurationSection itemMechanicConfiguration) {
        Mechanic mechanic = new ConsumablePotionEffectsMechanic(this, itemMechanicConfiguration);
        addToImplemented(mechanic);
        return mechanic;
    }
}
