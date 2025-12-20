package io.th0rgal.oraxen.mechanics.provided.cosmetic.aura;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.mechanics.ConfigProperty;
import io.th0rgal.oraxen.mechanics.Mechanic;
import io.th0rgal.oraxen.mechanics.MechanicFactory;
import io.th0rgal.oraxen.mechanics.MechanicInfo;
import io.th0rgal.oraxen.mechanics.MechanicsManager;
import io.th0rgal.oraxen.mechanics.PropertyType;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.List;

@MechanicInfo(
        category = "cosmetic",
        description = "Creates a particle aura effect around the player when holding the item"
)
public class AuraMechanicFactory extends MechanicFactory {

    @ConfigProperty(type = PropertyType.ENUM, description = "Aura type/pattern", enumValues = {"simple", "ring", "helix"})
    public static final String PROP_TYPE = "type";

    @ConfigProperty(type = PropertyType.ENUM, description = "Particle type to display", enumRef = "Particle")
    public static final String PROP_PARTICLE = "particle";

    private static AuraMechanicFactory instance;
    private final List<AuraMechanic> registeredMechanics = new ArrayList<>();

    public AuraMechanicFactory(ConfigurationSection section) {
        super(section);
        instance = this;
        MechanicsManager.registerListeners(OraxenPlugin.get(), getMechanicID(), new AuraMechanicListener(this));
    }

    public static AuraMechanicFactory getInstance() {
        return instance;
    }

    /**
     * Stops all aura tasks. Called during mechanic reload/unload.
     */
    public static void stopAllAuras() {
        if (instance != null) {
            for (AuraMechanic mechanic : instance.registeredMechanics) {
                mechanic.stopAura();
            }
            instance.registeredMechanics.clear();
        }
    }

    @Override
    public Mechanic parse(ConfigurationSection itemMechanicConfiguration) {
        AuraMechanic mechanic = new AuraMechanic(this, itemMechanicConfiguration);
        registeredMechanics.add(mechanic);
        addToImplemented(mechanic);
        return mechanic;
    }
}
