package io.th0rgal.oraxen.mechanics.provided.misc.armor_effects;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.mechanics.ConfigProperty;
import io.th0rgal.oraxen.mechanics.Mechanic;
import io.th0rgal.oraxen.mechanics.MechanicFactory;
import io.th0rgal.oraxen.mechanics.MechanicInfo;
import io.th0rgal.oraxen.mechanics.MechanicsManager;
import io.th0rgal.oraxen.mechanics.PropertyType;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.scheduler.BukkitTask;

@MechanicInfo(
        category = "misc",
        description = "Applies potion effects while wearing armor pieces"
)
public class ArmorEffectsFactory extends MechanicFactory {

    @ConfigProperty(type = PropertyType.LIST, description = "List of potion effects to apply")
    public static final String PROP_EFFECTS = "effects";

    private static ArmorEffectsFactory instance;
    private ArmorEffectsTask armorEffectTask;
    private final int delay;

    public ArmorEffectsFactory(ConfigurationSection section) {
        super(section);
        this.delay = section.getInt("delay_in_ticks", 20);
        instance = this;
        MechanicsManager.registerListeners(OraxenPlugin.get(), getMechanicID(), new ArmorEffectsListener());
    }

    public static ArmorEffectsFactory getInstance() {
        return instance;
    }

    @Override
    public Mechanic parse(ConfigurationSection configurationSection) {
        Mechanic mechanic = new ArmorEffectsMechanic(this, configurationSection);
        addToImplemented(mechanic);
        if (armorEffectTask != null) armorEffectTask.cancel();
        armorEffectTask = new ArmorEffectsTask();
        BukkitTask task = armorEffectTask.runTaskTimer(OraxenPlugin.get(), 0, delay);
        MechanicsManager.registerTask(instance.getMechanicID(), task);
        return mechanic;
    }

    public int getDelay() {
        return delay;
    }
}
