package io.th0rgal.oraxen.mechanics.provided.gameplay.furniture;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.mechanics.Mechanic;
import io.th0rgal.oraxen.mechanics.MechanicFactory;
import io.th0rgal.oraxen.mechanics.MechanicsManager;
import io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.evolution.EvolutionTask;
import io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.evolution.EvolutionListener;
import org.bukkit.configuration.ConfigurationSection;

import java.util.List;

public class FurnitureFactory extends MechanicFactory {

    public static FurnitureFactory instance;
    public final List<String> toolTypes;
    private final int evolutionTickInterval;
    private boolean evolvingFurnitures;
    private static EvolutionTask evolutionTask;

    public FurnitureFactory(ConfigurationSection section) {
        super(section);
        toolTypes = section.getStringList("tool_types");
        evolutionTickInterval = section.getInt("evolution_tick_interval");
        MechanicsManager.registerListeners(OraxenPlugin.get(), new FurnitureListener(this));
        MechanicsManager.registerListeners(OraxenPlugin.get(), new EvolutionListener(this));
        evolvingFurnitures = false;
        instance = this;
    }

    @Override
    public Mechanic parse(ConfigurationSection itemMechanicConfiguration) {
        Mechanic mechanic = new FurnitureMechanic(this, itemMechanicConfiguration);
        addToImplemented(mechanic);
        return mechanic;
    }

    public static FurnitureFactory getInstance() {
        return instance;
    }

    public void registerEvolution() {
        if (evolvingFurnitures)
            return;
        if (evolutionTask != null)
            evolutionTask.cancel();
        evolutionTask = new EvolutionTask(this);
        evolutionTask.runTaskTimer(OraxenPlugin.get(), 20L * evolutionTickInterval, 20L * evolutionTickInterval);
        evolvingFurnitures = true;
    }

}
