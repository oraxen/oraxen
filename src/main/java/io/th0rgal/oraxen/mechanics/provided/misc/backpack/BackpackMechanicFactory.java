package io.th0rgal.oraxen.mechanics.provided.misc.backpack;

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
        description = "Provides portable storage that persists with the item"
)
public class BackpackMechanicFactory extends MechanicFactory {

    @ConfigProperty(type = PropertyType.INTEGER, description = "Number of inventory rows (1-6)", defaultValue = "6", min = 1, max = 6)
    public static final String PROP_ROWS = "rows";

    @ConfigProperty(type = PropertyType.STRING, description = "Inventory title", defaultValue = "Backpack")
    public static final String PROP_TITLE = "title";

    @ConfigProperty(type = PropertyType.STRING, description = "Sound played when opening")
    public static final String PROP_OPEN_SOUND = "open_sound";

    @ConfigProperty(type = PropertyType.STRING, description = "Sound played when closing")
    public static final String PROP_CLOSE_SOUND = "close_sound";

    @ConfigProperty(type = PropertyType.DOUBLE, description = "Sound volume", defaultValue = "1.0", min = 0.0, max = 2.0)
    public static final String PROP_VOLUME = "volume";

    @ConfigProperty(type = PropertyType.DOUBLE, description = "Sound pitch", defaultValue = "1.0", min = 0.0, max = 2.0)
    public static final String PROP_PITCH = "pitch";

    public BackpackMechanicFactory(ConfigurationSection section) {
        super(section);
        MechanicsManager.registerListeners(OraxenPlugin.get(), getMechanicID(), new BackpackListener(this));
    }

    @Override
    public Mechanic parse(ConfigurationSection itemMechanicConfiguration) {
        Mechanic mechanic = new BackpackMechanic(this, itemMechanicConfiguration);
        addToImplemented(mechanic);
        return mechanic;
    }
}
