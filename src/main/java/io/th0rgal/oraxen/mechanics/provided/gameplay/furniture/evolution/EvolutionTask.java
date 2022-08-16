package io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.evolution;

import io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.FurnitureFactory;
import io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.FurnitureMechanic;
import io.th0rgal.oraxen.mechanics.provided.gameplay.noteblock.NoteBlockMechanic;
import io.th0rgal.oraxen.mechanics.provided.gameplay.noteblock.farmblock.FarmBlockDryout;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.ItemFrame;
import org.bukkit.persistence.PersistentDataContainer;
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
            for (ItemFrame frame : world.getEntitiesByClass(ItemFrame.class)) {
                Location frameLoc = frame.getLocation();
                PersistentDataContainer framePDC = frame.getPersistentDataContainer();
                if (!framePDC.has(EVOLUTION_KEY, PersistentDataType.INTEGER)) continue;

                String itemID = framePDC.get(FurnitureMechanic.FURNITURE_KEY, PersistentDataType.STRING);
                Block blockBelow = frameLoc.getBlock().getRelative(BlockFace.DOWN);
                FurnitureMechanic furnitureMechanic = (FurnitureMechanic) furnitureFactory.getMechanic(itemID);
                if (furnitureMechanic == null) continue;

                if (furnitureMechanic.farmlandRequired && blockBelow.getType() != Material.FARMLAND) {
                    furnitureMechanic.remove(frame);
                    continue;
                }

                if (furnitureMechanic.farmblockRequired) {
                    if (blockBelow.getType() != Material.NOTE_BLOCK) {
                        furnitureMechanic.remove(frame);
                        continue;
                    }

                    NoteBlockMechanic noteMechanic = getNoteBlockMechanic(blockBelow);
                    if (noteMechanic == null || !noteMechanic.hasDryout()) {
                        furnitureMechanic.remove(frame);
                        continue;
                    }
                    FarmBlockDryout dryoutMechanic = noteMechanic.getDryout();
                    if (noteMechanic.hasDryout()) {
                        if (!dryoutMechanic.isFarmBlock()) {
                            furnitureMechanic.remove(frame);
                            continue;
                        } else if (!dryoutMechanic.isMoistFarmBlock()) {
                            framePDC.set(FurnitureMechanic.EVOLUTION_KEY,
                                    PersistentDataType.INTEGER, 0);
                            continue;
                        }
                    }
                }

                EvolvingFurniture evolution = furnitureMechanic.getEvolution();
                if (evolution == null) continue;

                int lightBoostTick = 0;
                int rainBoostTick = 0;

                if (evolution.isLightBoosted() && frameLoc.getBlock().getLightLevel() >= evolution.getMinimumLightLevel())
                    lightBoostTick = evolution.getLightBoostTick();

                if (evolution.isRainBoosted() && world.hasStorm() && world.getHighestBlockAt(frameLoc).getY() > frameLoc.getY())
                    rainBoostTick = evolution.getRainBoostTick();

                int evolutionStep = framePDC.get(EVOLUTION_KEY, PersistentDataType.INTEGER) + delay + lightBoostTick + rainBoostTick;

                if (evolutionStep > evolution.getDelay()) {
                    if (evolution.getNextStage() == null) continue;
                    if (!evolution.bernoulliTest()) continue;

                    furnitureMechanic.remove(frame);
                    FurnitureMechanic nextMechanic = (FurnitureMechanic) furnitureFactory.getMechanic(evolution.getNextStage());
                    nextMechanic.place(frame.getRotation(),
                            furnitureMechanic.getYaw(frame.getRotation()),
                            frame.getFacing(),
                            frameLoc,
                            null
                    );
                } else framePDC.set(FurnitureMechanic.EVOLUTION_KEY, PersistentDataType.INTEGER, evolutionStep);
            }
    }
}
