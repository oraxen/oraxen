package io.th0rgal.oraxen.mechanics.provided.misc.armor_effects;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.api.scheduler.AdaptedTask;
import io.th0rgal.oraxen.mechanics.MechanicFactory;
import io.th0rgal.oraxen.mechanics.MechanicsManager;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
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

    public static ArmorEffectsFactory get() {
        return instance;
    }

    @Override
    public ArmorEffectsMechanic parse(ConfigurationSection section) {
        ArmorEffectsMechanic mechanic = new ArmorEffectsMechanic(this, section);
        addToImplemented(mechanic);
        if (armorEffectTask != null) armorEffectTask.getAdaptedTask().cancel();
        armorEffectTask = new ArmorEffectsTask();
        AdaptedTask task = armorEffectTask.runTaskTimer(0, delay);
        MechanicsManager.registerTask(instance.getMechanicID(), task);
        return mechanic;
    }

    @Override
    public ArmorEffectsMechanic getMechanic(String itemID) {
        return (ArmorEffectsMechanic) super.getMechanic(itemID);
    }

    @Override
    public ArmorEffectsMechanic getMechanic(ItemStack itemStack) {
        return (ArmorEffectsMechanic) super.getMechanic(itemStack);
    }

    public int getDelay() {
        return delay;
    }
}
