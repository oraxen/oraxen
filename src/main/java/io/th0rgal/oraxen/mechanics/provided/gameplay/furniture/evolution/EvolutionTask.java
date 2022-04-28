package io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.evolution;

import io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.FurnitureFactory;
import io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.FurnitureMechanic;
import io.th0rgal.oraxen.mechanics.provided.gameplay.noteblock.NoteBlockMechanic;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.ItemFrame;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;

import static io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.FurnitureMechanic.EVOLUTION_KEY;
import static io.th0rgal.oraxen.mechanics.provided.gameplay.noteblock.NoteBlockMechanicListener.getNoteBlockMechanic;

public class EvolutionTask extends BukkitRunnable {

    private final FurnitureFactory furnitureFactory;
    private final int delay;

    public EvolutionTask(FurnitureFactory furnitureFactory, int delay) {
        this.furnitureFactory = furnitureFactory;
        this.delay = delay;
    }

    @Override
    public void run() {
        for (World world : Bukkit.getWorlds())
            for (ItemFrame frame : world.getEntitiesByClass(ItemFrame.class))
                if (frame.getPersistentDataContainer().has(EVOLUTION_KEY,
                        PersistentDataType.INTEGER)) {

                    String itemID = frame.getPersistentDataContainer()
                            .get(FurnitureMechanic.FURNITURE_KEY, PersistentDataType.STRING);
                    Block blockBelow = frame.getLocation().clone().subtract(0, 1, 0).getBlock();
                    FurnitureMechanic mechanic = (FurnitureMechanic) furnitureFactory.getMechanic(itemID);
                    NoteBlockMechanic noteBlockMechanic = getNoteBlockMechanic(blockBelow);

                    if (mechanic.farmlandRequired && blockBelow.getType() != Material.FARMLAND) {
                        mechanic.remove(frame);
                        continue;
                    }

                    if (mechanic.farmblockRequired && !noteBlockMechanic.getDryout().isFarmBlock()) {
                        mechanic.remove(frame);
                        continue;
                    }

                    if (mechanic.farmblockRequired && !noteBlockMechanic.getDryout().isMoistFarmBlock()) {
                        frame.getPersistentDataContainer().set(FurnitureMechanic.EVOLUTION_KEY,
                                PersistentDataType.INTEGER, 0);
                        continue;
                    }

                    EvolvingFurniture evolution = mechanic.getEvolution();
                    int evolutionStep = frame.getPersistentDataContainer()
                            .get(EVOLUTION_KEY, PersistentDataType.INTEGER)
                            + delay * frame.getLocation().getBlock().getLightLevel();

                    if (evolutionStep > evolution.getDelay()) {
                        if (evolution.getNextStage() == null) continue;
                        if (!evolution.bernoulliTest()) continue;
                        mechanic.remove(frame);
                        FurnitureMechanic nextMechanic = (FurnitureMechanic)
                                furnitureFactory.getMechanic(evolution.getNextStage());
                        nextMechanic.place(frame.getRotation(),
                                mechanic.getYaw(frame.getRotation()),
                                frame.getFacing(),
                                frame.getLocation(),
                                null
                        );
                    } else frame.getPersistentDataContainer().set(FurnitureMechanic.EVOLUTION_KEY,
                            PersistentDataType.INTEGER, evolutionStep);
                }
    }
}
