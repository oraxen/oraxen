package io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.evolution;

import io.th0rgal.oraxen.api.OraxenBlocks;
import io.th0rgal.oraxen.api.OraxenFurniture;
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
                    OraxenFurniture.remove(frameLoc, null);
                    continue;
                }

                if (furnitureMechanic.farmblockRequired) {
                    if (blockBelow.getType() != Material.NOTE_BLOCK) {
                        OraxenFurniture.remove(frameLoc, null);
                        continue;
                    }

                    NoteBlockMechanic noteMechanic = OraxenBlocks.getNoteBlockMechanic(blockBelow);
                    if (noteMechanic == null || !noteMechanic.hasDryout()) {
                        OraxenFurniture.remove(frameLoc, null);
                        continue;
                    }
                    FarmBlockDryout dryoutMechanic = noteMechanic.getDryout();
                    if (noteMechanic.hasDryout()) {
                        if (!dryoutMechanic.isFarmBlock()) {
                            OraxenFurniture.remove(frameLoc, null);
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

                    OraxenFurniture.remove(frameLoc, null);
                    FurnitureMechanic nextMechanic = (FurnitureMechanic) furnitureFactory.getMechanic(evolution.getNextStage());
                    nextMechanic.place(frameLoc, FurnitureMechanic.rotationToYaw(frame.getRotation()), frame.getRotation(), frame.getFacing()
                    );
                } else framePDC.set(FurnitureMechanic.EVOLUTION_KEY, PersistentDataType.INTEGER, evolutionStep);
            }
    }
}
