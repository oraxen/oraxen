package io.th0rgal.oraxen.mechanics.provided.misc.misc;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.mechanics.ConfigProperty;
import io.th0rgal.oraxen.mechanics.Mechanic;
import io.th0rgal.oraxen.mechanics.MechanicFactory;
import io.th0rgal.oraxen.mechanics.MechanicInfo;
import io.th0rgal.oraxen.mechanics.MechanicsManager;
import io.th0rgal.oraxen.mechanics.PropertyType;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;

@MechanicInfo(
        category = "misc",
        description = "Miscellaneous item properties"
)
public class MiscMechanicFactory extends MechanicFactory {

    @ConfigProperty(type = PropertyType.BOOLEAN, description = "Deny vanilla right-click, consume, and bow-shoot behavior", defaultValue = "false")
    public static final String PROP_DISABLE_VANILLA_INTERACTIONS = "disable_vanilla_interactions";

    @ConfigProperty(type = PropertyType.BOOLEAN, description = "Let this item strip logs", defaultValue = "false")
    public static final String PROP_CAN_STRIP_LOGS = "can_strip_logs";

    @ConfigProperty(type = PropertyType.BOOLEAN, description = "Piglins ignore a player who has this item equipped", defaultValue = "false")
    public static final String PROP_PIGLINS_IGNORE_WHEN_EQUIPPED = "piglins_ignore_when_equipped";

    @ConfigProperty(type = PropertyType.BOOLEAN, description = "This item can be composted", defaultValue = "false")
    public static final String PROP_COMPOSTABLE = "compostable";

    @ConfigProperty(type = PropertyType.BOOLEAN, description = "Whether item renaming in anvils is prevented", defaultValue = "false")
    public static final String PROP_PREVENT_RENAMING = "prevent_renaming";

    @ConfigProperty(type = PropertyType.BOOLEAN, description = "Allow this item in vanilla recipes", defaultValue = "false")
    public static final String PROP_ALLOW_IN_VANILLA_RECIPES = "allow_in_vanilla_recipes";

    private static MiscMechanicFactory instance;

    public MiscMechanicFactory(ConfigurationSection section) {
        super(section);
        MechanicsManager.registerListeners(OraxenPlugin.get(), getMechanicID(), new MiscListener(this));
        instance = this;
    }

    @Override
    public Mechanic parse(ConfigurationSection section) {
        MiscMechanic mechanic = new MiscMechanic(this, section);

        addToImplemented(mechanic);
        return mechanic;
    }

    public static MiscMechanicFactory get() {
        return instance;
    }

    @Override
    public MiscMechanic getMechanic(String itemID) {
        return (MiscMechanic) super.getMechanic(itemID);
    }

    @Override
    public MiscMechanic getMechanic(ItemStack itemStack) {
        return (MiscMechanic) super.getMechanic(itemStack);
    }
}
