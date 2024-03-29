package io.th0rgal.oraxen.mechanics.provided.misc.armor_effects;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.mechanics.Mechanic;
import io.th0rgal.oraxen.mechanics.MechanicFactory;
import io.th0rgal.oraxen.mechanics.MechanicsManager;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.scheduler.BukkitTask;

public class ArmorEffectsFactory extends MechanicFactory {

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
