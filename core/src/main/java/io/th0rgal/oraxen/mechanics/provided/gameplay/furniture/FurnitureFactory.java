package io.th0rgal.oraxen.mechanics.provided.gameplay.furniture;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.mechanics.Mechanic;
import io.th0rgal.oraxen.mechanics.MechanicFactory;
import io.th0rgal.oraxen.mechanics.MechanicsManager;
import io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.evolution.EvolutionListener;
import io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.evolution.EvolutionTask;
import io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.jukebox.JukeboxListener;
import io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.listeners.FurnitureListener;
import io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.listeners.FurnitureSoundListener;
import io.th0rgal.oraxen.nms.EmptyFurniturePacketManager;
import io.th0rgal.oraxen.nms.NMSHandlers;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

import java.util.List;

public class FurnitureFactory extends MechanicFactory {

    public static FurnitureMechanic.FurnitureType defaultFurnitureType;
    public static FurnitureFactory instance;
    public final List<String> toolTypes;
    public final int evolutionCheckDelay;
    private boolean evolvingFurnitures;
    private static EvolutionTask evolutionTask;
    public final boolean customSounds;

    public FurnitureFactory(ConfigurationSection section) {
        super(section);
        instance = this;
        if (OraxenPlugin.supportsDisplayEntities)
            defaultFurnitureType = FurnitureMechanic.FurnitureType.getType(section.getString("default_furniture_type", "DISPLAY_ENTITY"));
        else defaultFurnitureType = FurnitureMechanic.FurnitureType.ITEM_FRAME;
        toolTypes = section.getStringList("tool_types");
        evolutionCheckDelay = section.getInt("evolution_check_delay");
        MechanicsManager.registerListeners(OraxenPlugin.get(), getMechanicID(),
                new FurnitureListener(),
                new EvolutionListener(),
                new JukeboxListener()
        );
        FurnitureUpdater.registerListener();
        evolvingFurnitures = false;
        customSounds = OraxenPlugin.get().configsManager().getMechanics().getBoolean("custom_block_sounds.stringblock_and_furniture", true);

        if (customSounds) MechanicsManager.registerListeners(OraxenPlugin.get(), getMechanicID(), new FurnitureSoundListener());
    }

    public IFurniturePacketManager furniturePacketManager() {
        return NMSHandlers.getHandler() != null ? NMSHandlers.getHandler().furniturePacketManager() : new EmptyFurniturePacketManager();
    }

    @Override
    public Mechanic parse(ConfigurationSection section) {
        Mechanic mechanic = new FurnitureMechanic(this, section);
        addToImplemented(mechanic);
        return mechanic;
    }

    public static boolean isEnabled() {
        return instance != null;
    }

    public static FurnitureFactory get() {
        return instance;
    }

    public void registerEvolution() {
        if (evolvingFurnitures)
            return;
        if (evolutionTask != null)
            evolutionTask.cancel();
        evolutionTask = new EvolutionTask(this, evolutionCheckDelay);
        BukkitTask task = evolutionTask.runTaskTimer(OraxenPlugin.get(), 0, evolutionCheckDelay);
        MechanicsManager.registerTask(getMechanicID(), task);
        evolvingFurnitures = true;
    }

    public static void unregisterEvolution() {
        if (evolutionTask != null)
            evolutionTask.cancel();
    }

    @Override
    public FurnitureMechanic getMechanic(String itemID) {
        return (FurnitureMechanic) super.getMechanic(itemID);
    }

    @Override
    public FurnitureMechanic getMechanic(ItemStack itemStack) {
        return (FurnitureMechanic) super.getMechanic(itemStack);
    }

}
