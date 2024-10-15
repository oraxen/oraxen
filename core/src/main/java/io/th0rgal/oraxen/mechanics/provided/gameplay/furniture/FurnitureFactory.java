package io.th0rgal.oraxen.mechanics.provided.gameplay.furniture;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.mechanics.Mechanic;
import io.th0rgal.oraxen.mechanics.MechanicFactory;
import io.th0rgal.oraxen.mechanics.MechanicsManager;
import io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.evolution.EvolutionListener;
import io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.evolution.EvolutionTask;
import io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.jukebox.JukeboxListener;
import io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.listeners.FurnitureListener;
import io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.listeners.FurniturePacketListener;
import io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.listeners.FurnitureSoundListener;
import io.th0rgal.oraxen.nms.EmptyFurniturePacketManager;
import io.th0rgal.oraxen.nms.NMSHandler;
import io.th0rgal.oraxen.nms.NMSHandlers;
import io.th0rgal.oraxen.utils.PluginUtils;
import io.th0rgal.oraxen.utils.VersionUtil;
import io.th0rgal.oraxen.utils.logs.Logs;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

import java.util.List;
import java.util.Optional;

public class FurnitureFactory extends MechanicFactory {

    public static FurnitureFactory instance;
    public final List<String> toolTypes;
    public final int evolutionCheckDelay;
    private boolean evolvingFurnitures;
    private static EvolutionTask evolutionTask;
    public double simulationRadius = Math.pow((Bukkit.getServer().getSimulationDistance() * 16.0), 2.0);

    public FurnitureFactory(ConfigurationSection section) {
        super(section);
        instance = this;
        toolTypes = section.getStringList("tool_types");
        evolutionCheckDelay = section.getInt("evolution_check_delay");
        MechanicsManager.registerListeners(OraxenPlugin.get(), getMechanicID(),
                new FurnitureListener(),
                new EvolutionListener(),
                new JukeboxListener()
        );

        if (VersionUtil.isPaperServer()) {
            MechanicsManager.registerListeners(OraxenPlugin.get(), "furniture", new FurniturePacketListener());
        } else {
            Logs.logWarning("Seems that your server is a Spigot-server");
            Logs.logWarning("FurnitureHitboxes will not work due to it relying on Paper-only events");
            Logs.logWarning("It is heavily recommended to make the upgrade to Paper");
        }

        evolvingFurnitures = false;

        if (PluginUtils.isEnabled("AxiomPaper"))
            MechanicsManager.registerListeners(OraxenPlugin.get(), getMechanicID(), new AxiomCompatibility());

        MechanicsManager.registerListeners(OraxenPlugin.get(), getMechanicID(), new FurnitureSoundListener());
    }

    public IFurniturePacketManager packetManager() {
        return Optional.of(NMSHandlers.getHandler()).map(NMSHandler::furniturePacketManager).orElse(new EmptyFurniturePacketManager());
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

    public static void removeAllFurniturePackets() {
        if (instance == null) return;
        instance.packetManager().removeAllFurniturePackets();
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
