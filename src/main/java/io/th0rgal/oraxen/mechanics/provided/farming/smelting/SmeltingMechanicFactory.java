package io.th0rgal.oraxen.mechanics.provided.farming.smelting;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.mechanics.ConfigProperty;
import io.th0rgal.oraxen.mechanics.Mechanic;
import io.th0rgal.oraxen.mechanics.MechanicFactory;
import io.th0rgal.oraxen.mechanics.MechanicInfo;
import io.th0rgal.oraxen.mechanics.MechanicsManager;
import io.th0rgal.oraxen.mechanics.PropertyType;
import io.th0rgal.oraxen.utils.OraxenYaml;
import io.th0rgal.oraxen.utils.logs.Logs;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;

import java.util.EnumSet;
import java.util.Set;

@MechanicInfo(
        category = "farming",
        description = "Automatically smelts mined blocks into their smelted form"
)
public class SmeltingMechanicFactory extends MechanicFactory {

    @ConfigProperty(type = PropertyType.BOOLEAN, description = "Play smelting sound effect", defaultValue = "true")
    public static final String PROP_PLAY_SOUND = "play_sound";

    private static SmeltingMechanicFactory instance;
    private final Set<Material> blacklistedDrops = EnumSet.noneOf(Material.class);

    public SmeltingMechanicFactory(ConfigurationSection section) {
        super(section);
        instance = this;
        for (String name : section.getStringList("blacklist_cooked")) {
            Material material = OraxenYaml.getMaterial(name);
            if (material != null) blacklistedDrops.add(material);
            else Logs.logWarning("Unknown material " + name + " in smelting blacklist_cooked, skipping it.");
        }
        MechanicsManager.registerListeners(OraxenPlugin.get(), getMechanicID(), new SmeltingMechanicListener(this));
    }

    @Override
    public Mechanic parse(ConfigurationSection itemMechanicConfiguration) {
        Mechanic mechanic = new SmeltingMechanic(this, itemMechanicConfiguration);
        addToImplemented(mechanic);
        return mechanic;
    }

    /** Drops listed in blacklist_cooked keep their normal form. */
    public boolean isCookingBlacklisted(Material material) {
        return blacklistedDrops.contains(material);
    }

    public static SmeltingMechanicFactory getInstance() {
        return instance;
    }
}
