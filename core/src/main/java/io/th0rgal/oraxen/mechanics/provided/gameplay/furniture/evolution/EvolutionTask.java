package io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.evolution;

import io.th0rgal.oraxen.api.OraxenBlocks;
import io.th0rgal.oraxen.api.OraxenFurniture;
import io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.FurnitureFactory;
import io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.FurnitureMechanic;
import io.th0rgal.oraxen.mechanics.provided.gameplay.noteblock.NoteBlockMechanic;
import io.th0rgal.oraxen.mechanics.provided.gameplay.noteblock.farmblock.FarmBlockDryout;
import io.th0rgal.oraxen.utils.SchedulerUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import static io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.FurnitureMechanic.EVOLUTION_KEY;

public class EvolutionTask implements Runnable {

    private final FurnitureFactory furnitureFactory;
    private final int delay;
    private SchedulerUtil.ScheduledTask scheduledTask;

    public EvolutionTask(FurnitureFactory furnitureFactory, int delay) {
        this.furnitureFactory = furnitureFactory;
        this.delay = delay;
    }

    public SchedulerUtil.ScheduledTask start(long initialDelay, long period) {
        scheduledTask = SchedulerUtil.runTaskTimer(initialDelay, period, this);
        return scheduledTask;
    }

    public void cancel() {
        if (scheduledTask != null) {
            scheduledTask.cancel();
            scheduledTask = null;
        }
    }

    @Override
    public void run() {
        for (World world : Bukkit.getWorlds())
            for (Class<? extends Entity> entityClass : FurnitureMechanic.FurnitureType.furnitureEntityClasses())
                for (Entity entity : world.getEntitiesByClass(entityClass)) {
                    Location entityLoc = entity.getLocation();
                    PersistentDataContainer pdc = entity.getPersistentDataContainer();
                    if (!pdc.has(EVOLUTION_KEY, PersistentDataType.INTEGER)) continue;

                    Block blockBelow = entityLoc.getBlock().getRelative(BlockFace.DOWN);
                    FurnitureMechanic mechanic = OraxenFurniture.getFurnitureMechanic(entity);
                    if (mechanic == null) continue;

                    if (mechanic.farmlandRequired && blockBelow.getType() != Material.FARMLAND) {
                        OraxenFurniture.remove(entity, null);
                        continue;
                    }

                    if (mechanic.farmblockRequired) {
                        NoteBlockMechanic noteMechanic = OraxenBlocks.getNoteBlockMechanic(blockBelow);
                        if (noteMechanic == null || !noteMechanic.hasDryout()) {
                            OraxenFurniture.remove(entity, null);
                            continue;
                        }
                        FarmBlockDryout dryoutMechanic = noteMechanic.getDryout();
                        if (!dryoutMechanic.isFarmBlock()) {
                            OraxenFurniture.remove(entity, null);
                            continue;
                        } else if (!dryoutMechanic.isMoistFarmBlock()) {
                            pdc.set(FurnitureMechanic.EVOLUTION_KEY, PersistentDataType.INTEGER, 0);
                            continue;
                        }
                    }

                    EvolvingFurniture evolution = mechanic.getEvolution();
                    if (evolution == null) continue;

                    int lightBoostTick = 0;
                    int rainBoostTick = 0;

                    if (evolution.isLightBoosted() && entityLoc.getBlock().getLightLevel() >= evolution.getMinimumLightLevel())
                        lightBoostTick = evolution.getLightBoostTick();

                    if (evolution.isRainBoosted() && world.hasStorm() && world.getHighestBlockAt(entityLoc).getY() > entityLoc.getY())
                        rainBoostTick = evolution.getRainBoostTick();

                    int evolutionStep = pdc.get(EVOLUTION_KEY, PersistentDataType.INTEGER) + delay + lightBoostTick + rainBoostTick;

                    if (evolutionStep > evolution.getDelay()) {
                        if (evolution.getNextStage() == null) continue;
                        if (!evolution.bernoulliTest()) continue;

                        FurnitureMechanic nextMechanic = (FurnitureMechanic) furnitureFactory.getMechanic(evolution.getNextStage());
                        if (nextMechanic == null) continue;

                        OraxenFurniture.remove(entity, null);
                        nextMechanic.place(entity.getLocation(), entity.getLocation().getYaw(), entity.getFacing());
                        //OraxenFurniture.place(entity.getLocation(), evolution.getNextStage(), FurnitureMechanic.yawToRotation(entity.getLocation().getYaw()), entity.getFacing());
                        //nextMechanic.place(entityLoc, entityLoc.getYaw(), FurnitureMechanic.yawToRotation(entityLoc.getYaw()), entity.getFacing());
                    } else pdc.set(FurnitureMechanic.EVOLUTION_KEY, PersistentDataType.INTEGER, evolutionStep);
                }
    }
}
