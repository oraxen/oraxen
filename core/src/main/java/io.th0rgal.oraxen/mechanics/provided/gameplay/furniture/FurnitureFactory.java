package io.th0rgal.oraxen.mechanics.provided.gameplay.furniture;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.api.OraxenFurniture;
import io.th0rgal.oraxen.mechanics.Mechanic;
import io.th0rgal.oraxen.mechanics.MechanicFactory;
import io.th0rgal.oraxen.mechanics.MechanicsManager;
import io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.evolution.EvolutionListener;
import io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.evolution.EvolutionTask;
import io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.jukebox.JukeboxListener;
import io.th0rgal.oraxen.utils.VersionUtil;
import io.th0rgal.oraxen.utils.logs.Logs;
import org.bukkit.configuration.ConfigurationSection;

import java.util.List;
import java.util.Objects;

public class FurnitureFactory extends MechanicFactory {

    public static FurnitureMechanic.FurnitureType defaultFurnitureType;
    public static FurnitureFactory instance;
    public final List<String> toolTypes;
    public final int evolutionCheckDelay;
    private boolean evolvingFurnitures;
    private static EvolutionTask evolutionTask;
    public final boolean customSounds;
    public final boolean detectViabackwards;

    public FurnitureFactory(ConfigurationSection section) {
        super(section);
        if (OraxenPlugin.supportsDisplayEntities)
            defaultFurnitureType = FurnitureMechanic.FurnitureType.getType(section.getString("default_furniture_type", "DISPLAY_ENTITY"));
        else defaultFurnitureType = FurnitureMechanic.FurnitureType.ITEM_FRAME;
        toolTypes = section.getStringList("tool_types");
        evolutionCheckDelay = section.getInt("evolution_check_delay");
        MechanicsManager.registerListeners(OraxenPlugin.get(), getMechanicID(),
                new FurnitureListener(this),
                new FurnitureUpdater(),
                new EvolutionListener(),
                new JukeboxListener()
        );
        evolvingFurnitures = false;
        instance = this;
        customSounds = OraxenPlugin.get().getConfigsManager().getMechanics().getConfigurationSection("custom_block_sounds").getBoolean("stringblock_and_furniture", true);

        if (customSounds) MechanicsManager.registerListeners(OraxenPlugin.get(), getMechanicID(), new FurnitureSoundListener());
        detectViabackwards = OraxenPlugin.get().getConfigsManager().getMechanics().getConfigurationSection("furniture").getBoolean("detect_viabackwards", true);
    }

    public static boolean setDefaultType(ConfigurationSection mechanicSection) {
        if (mechanicSection.isSet("type")) return true;
        mechanicSection.set("type", defaultFurnitureType.toString());
        return false;
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

    public static EvolutionTask getEvolutionTask() {
        return evolutionTask;
    }

    public void registerEvolution() {
        if (evolvingFurnitures)
            return;
        if (evolutionTask != null)
            evolutionTask.cancel();
        evolutionTask = new EvolutionTask(this, evolutionCheckDelay);
        evolutionTask.runTaskTimer(OraxenPlugin.get(), 0, evolutionCheckDelay);
        evolvingFurnitures = true;
    }

    public static void unregisterEvolution() {
        if (evolutionTask != null)
            evolutionTask.cancel();
    }

}
