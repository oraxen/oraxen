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

import java.util.Random;

import static io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.FurnitureMechanic.EVOLUTION_KEY;
import static io.th0rgal.oraxen.mechanics.provided.gameplay.noteblock.NoteBlockMechanicListener.getNoteBlockMechanic;

public class EvolutionTask extends BukkitRunnable {

    private final FurnitureFactory furnitureFactory;

    public EvolutionTask(FurnitureFactory furnitureFactory) {
        this.furnitureFactory = furnitureFactory;
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

                    if (mechanic == null) {
                        continue;
                    }

                    if (mechanic.farmlandRequired && blockBelow.getType() != Material.FARMLAND) {
                        mechanic.remove(frame);
                        continue;
                    }

                    if (mechanic.farmblockRequired) {

                        if (blockBelow.getType() != Material.NOTE_BLOCK) {
                            mechanic.remove(frame);
                            continue;
                        }

                        NoteBlockMechanic noteBlockMechanic = getNoteBlockMechanic(blockBelow);
                        if (noteBlockMechanic.hasDryout()) {
                            if (!noteBlockMechanic.getDryout().isFarmBlock()) {
                                mechanic.remove(frame);
                                continue;
                            } else if (!noteBlockMechanic.getDryout().isMoistFarmBlock()) {
                                frame.getPersistentDataContainer().set(FurnitureMechanic.EVOLUTION_KEY,
                                        PersistentDataType.INTEGER, 0);
                                continue;
                            }
                        }
                    }

                    EvolvingFurniture evolution = mechanic.getEvolution();
                    double growChance = evolution.getGrowChance();

                    if (evolution.isRainBoosted() && world.hasStorm() && world.getHighestBlockAt(frame.getLocation()).getY() > frame.getLocation().getY())
                        growChance += 0.5;
                    if (evolution.isLightBoosted() && frame.getLocation().getBlock().getLightLevel() > 9)
                        growChance += 0.5;

                    double random = new Random().nextDouble();
                    if (growChance >= random) {
                        if (evolution.getNextStage() == null) continue;
                        mechanic.remove(frame);
                        FurnitureMechanic nextMechanic = (FurnitureMechanic)
                                furnitureFactory.getMechanic(evolution.getNextStage());
                        nextMechanic.place(frame.getRotation(),
                                mechanic.getYaw(frame.getRotation()),
                                frame.getFacing(),
                                frame.getLocation(),
                                null
                        );
                    }
                }
    }
}
