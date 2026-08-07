package io.th0rgal.oraxen.mechanics.provided.misc.food;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.mechanics.ConfigProperty;
import io.th0rgal.oraxen.mechanics.Mechanic;
import io.th0rgal.oraxen.mechanics.MechanicFactory;
import io.th0rgal.oraxen.mechanics.MechanicInfo;
import io.th0rgal.oraxen.mechanics.MechanicsManager;
import io.th0rgal.oraxen.mechanics.PropertyType;
import io.th0rgal.oraxen.utils.VersionUtil;
import io.th0rgal.oraxen.utils.logs.Logs;
import org.bukkit.configuration.ConfigurationSection;

@Deprecated(forRemoval = true, since = "1.20.6")
@MechanicInfo(
        category = "misc",
        description = "Legacy food behavior (deprecated on 1.20.5+, use food component instead)"
)
public class FoodMechanicFactory extends MechanicFactory {

    @ConfigProperty(type = PropertyType.INTEGER, description = "Hunger points restored", defaultValue = "1", min = 0)
    public static final String PROP_HUNGER = "hunger";

    @ConfigProperty(type = PropertyType.DOUBLE, description = "Saturation restored", defaultValue = "1.0", min = 0.0)
    public static final String PROP_SATURATION = "saturation";

    @ConfigProperty(type = PropertyType.STRING, description = "Item to give after consuming")
    public static final String PROP_REPLACEMENT = "replacement";

    @ConfigProperty(type = PropertyType.LIST, description = "Potion effects to apply on consume")
    public static final String PROP_EFFECTS = "effects";

    private static FoodMechanicFactory instance;

    public FoodMechanicFactory(ConfigurationSection section) {
        super(section);
        instance = this;
        MechanicsManager.registerListeners(OraxenPlugin.get(), getMechanicID(), new FoodMechanicListener(this));
    }

    public static FoodMechanicFactory getInstance() {
        return instance;
    }

    @Override
    public Mechanic parse(ConfigurationSection itemMechanicConfiguration) {
        Mechanic mechanic = new FoodMechanic(this, itemMechanicConfiguration);

        if (VersionUtil.atOrAbove("1.20.5")) {
            Logs.logWarning(mechanic.getItemID() + " is using deprecated Food-Mechanic...");
            Logs.logWarning("It is heavily advised to swap to the new `food`-property on 1.20.5+ servers...");
        }

        addToImplemented(mechanic);
        return mechanic;
    }
}
