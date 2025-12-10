package io.th0rgal.oraxen.mechanics.provided.misc.misc;

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
import org.bukkit.inventory.ItemStack;

@MechanicInfo(
        category = "misc",
        description = "Miscellaneous item properties like fire/lava damage resistance"
)
public class MiscMechanicFactory extends MechanicFactory {

    @ConfigProperty(type = PropertyType.BOOLEAN, description = "Whether item burns in fire (deprecated, use damage_resistant)", defaultValue = "true")
    public static final String PROP_BURNS_IN_FIRE = "burns_in_fire";

    @ConfigProperty(type = PropertyType.BOOLEAN, description = "Whether item burns in lava (deprecated, use damage_resistant)", defaultValue = "true")
    public static final String PROP_BURNS_IN_LAVA = "burns_in_lava";

    @ConfigProperty(type = PropertyType.BOOLEAN, description = "Whether item breaks from cactus (deprecated, use damage_resistant)", defaultValue = "true")
    public static final String PROP_BREAKS_FROM_CACTUS = "breaks_from_cactus";

    @ConfigProperty(type = PropertyType.BOOLEAN, description = "Whether item can break music discs", defaultValue = "false")
    public static final String PROP_BREAK_MUSIC_DISCS = "break_music_discs";

    private static MiscMechanicFactory instance;

    public MiscMechanicFactory(ConfigurationSection section) {
        super(section);
        MechanicsManager.registerListeners(OraxenPlugin.get(), getMechanicID(), new MiscListener(this));
        instance = this;
    }

    @Override
    public Mechanic parse(ConfigurationSection section) {
        MiscMechanic mechanic = new MiscMechanic(this, section);

        if (VersionUtil.atOrAbove("1.21.2")) {
            if ((!mechanic.burnsInFire() || !mechanic.burnsInLava()) &&
                    (section.contains(PROP_BURNS_IN_FIRE) || section.contains(PROP_BURNS_IN_LAVA))) {
                Logs.logWarning(mechanic.getItemID() + " is using deprecated Misc-Mechanic burns_in_fire/lava...");
                Logs.logWarning("It is heavily advised to swap to the new `damage_resistant`-component on 1.21.2+ servers...");
            } else if (!mechanic.breaksFromCactus() && section.contains(PROP_BREAKS_FROM_CACTUS)) {
                Logs.logWarning(mechanic.getItemID() + " is using deprecated Misc-Mechanic breaks_from_cactus...");
                Logs.logWarning("It is heavily advised to swap to the new `damage_resistant`-component on 1.21.2+ servers...");
            }
        }

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
