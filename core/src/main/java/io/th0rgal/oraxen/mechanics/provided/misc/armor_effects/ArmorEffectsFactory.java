package io.th0rgal.oraxen.mechanics.provided.misc.armor_effects;

import fr.euphyllia.energie.model.SchedulerTaskInter;
import fr.euphyllia.energie.model.SchedulerType;
import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.mechanics.Mechanic;
import io.th0rgal.oraxen.mechanics.MechanicFactory;
import io.th0rgal.oraxen.mechanics.MechanicsManager;
import org.bukkit.configuration.ConfigurationSection;

public class ArmorEffectsFactory extends MechanicFactory {

    private static ArmorEffectsFactory instance;
    private ArmorEffectsTask armorEffectTask;
    private SchedulerTaskInter armorEffectSchedulerTask;
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
        if (armorEffectSchedulerTask != null) armorEffectSchedulerTask.cancel();
        armorEffectTask = new ArmorEffectsTask();
        armorEffectSchedulerTask = OraxenPlugin.getScheduler().runAtFixedRate(SchedulerType.SYNC, schedulerTaskInter -> {
            armorEffectTask.run();
        }, 0, delay);
        return mechanic;
    }

    public int getDelay() {
        return delay;
    }
}
