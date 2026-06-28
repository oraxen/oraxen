package io.th0rgal.oraxen.mechanics.provided.farming.smelting;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.mechanics.ConfigProperty;
import io.th0rgal.oraxen.mechanics.Mechanic;
import io.th0rgal.oraxen.mechanics.MechanicFactory;
import io.th0rgal.oraxen.mechanics.MechanicInfo;
import io.th0rgal.oraxen.mechanics.MechanicsManager;
import io.th0rgal.oraxen.mechanics.PropertyType;
import org.bukkit.configuration.ConfigurationSection;

@MechanicInfo(
        category = "farming",
        description = "Automatically smelts mined blocks into their smelted form"
)
public class SmeltingMechanicFactory extends MechanicFactory {

    @ConfigProperty(type = PropertyType.BOOLEAN, description = "Play smelting sound effect", defaultValue = "true")
    public static final String PROP_PLAY_SOUND = "play_sound";

    private static SmeltingMechanicFactory instance;

    public SmeltingMechanicFactory(ConfigurationSection section) {
        super(section);
        instance = this;
        MechanicsManager.registerListeners(OraxenPlugin.get(), getMechanicID(), new SmeltingMechanicListener(this));
    }

    @Override
    public Mechanic parse(ConfigurationSection itemMechanicConfiguration) {
        Mechanic mechanic = new SmeltingMechanic(this, itemMechanicConfiguration);
        addToImplemented(mechanic);
        return mechanic;
    }

    public static SmeltingMechanicFactory getInstance() {
        return instance;
    }
}
