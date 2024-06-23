package io.th0rgal.oraxen.mechanics.provided.misc.food;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.mechanics.Mechanic;
import io.th0rgal.oraxen.mechanics.MechanicFactory;
import io.th0rgal.oraxen.mechanics.MechanicsManager;
import io.th0rgal.oraxen.utils.VersionUtil;
import io.th0rgal.oraxen.utils.logs.Logs;
import org.bukkit.configuration.ConfigurationSection;

@Deprecated(forRemoval = true, since = "1.20.6")
public class FoodMechanicFactory extends MechanicFactory {

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
