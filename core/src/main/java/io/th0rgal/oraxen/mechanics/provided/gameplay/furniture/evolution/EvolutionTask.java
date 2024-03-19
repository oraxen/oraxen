package io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.evolution;

import fr.euphyllia.energie.Energie;
import fr.euphyllia.energie.model.MultipleRecords;
import fr.euphyllia.energie.model.SchedulerType;
import fr.euphyllia.energie.utils.SchedulerTaskRunnable;
import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.api.OraxenBlocks;
import io.th0rgal.oraxen.api.OraxenFurniture;
import io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.FurnitureFactory;
import io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.FurnitureMechanic;
import io.th0rgal.oraxen.mechanics.provided.gameplay.noteblock.NoteBlockMechanic;
import io.th0rgal.oraxen.mechanics.provided.gameplay.noteblock.farmblock.FarmBlockDryout;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import static io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.FurnitureMechanic.EVOLUTION_KEY;

public class EvolutionTask extends SchedulerTaskRunnable {

    private final FurnitureFactory furnitureFactory;
    private final int delay;

    public EvolutionTask(FurnitureFactory furnitureFactory, int delay) {
        this.furnitureFactory = furnitureFactory;
        this.delay = delay;
    }

    @Override
    public void run() {
        for (World world : Bukkit.getWorlds()) {
            for (Chunk chunk : world.getLoadedChunks()) {
                OraxenPlugin.getScheduler().runTask(SchedulerType.SYNC, new MultipleRecords.WorldChunk(world, chunk.getX(), chunk.getZ()), schedulerTaskInter -> {
                    if (Energie.isFolia()) {
                        for (Entity entity : chunk.getEntities()) {
                            if (!FurnitureMechanic.FurnitureType.furnitureEntity().contains(entity.getType())) continue;
                            this.evolutionEntity(world, entity);
                        }
                    } else {
                        for (Class<? extends Entity> entityClass : FurnitureMechanic.FurnitureType.furnitureEntityClasses())
                            for (Entity entity : world.getEntitiesByClass(entityClass)) {
                                this.evolutionEntity(world, entity);
                            }
                    }
                });
            }
        }
    }

    private void evolutionEntity(World world, Entity entity) {
        OraxenPlugin.getScheduler().runTask(SchedulerType.SYNC, entity, schedulerTaskInter1 -> {
            Location entityLoc = entity.getLocation();
            PersistentDataContainer pdc = entity.getPersistentDataContainer();
            if (!pdc.has(EVOLUTION_KEY, PersistentDataType.INTEGER)) return;

            Block blockBelow = entityLoc.getBlock().getRelative(BlockFace.DOWN);
            FurnitureMechanic mechanic = OraxenFurniture.getFurnitureMechanic(entity);
            if (mechanic == null) return;

            if (mechanic.farmlandRequired && blockBelow.getType() != Material.FARMLAND) {
                OraxenFurniture.remove(entity, null);
                return;
            }

            if (mechanic.farmblockRequired) {
                NoteBlockMechanic noteMechanic = OraxenBlocks.getNoteBlockMechanic(blockBelow);
                if (noteMechanic == null || !noteMechanic.hasDryout()) {
                    OraxenFurniture.remove(entity, null);
                    return;
                }
                FarmBlockDryout dryoutMechanic = noteMechanic.getDryout();
                if (!dryoutMechanic.isFarmBlock()) {
                    OraxenFurniture.remove(entity, null);
                    return;
                } else if (!dryoutMechanic.isMoistFarmBlock()) {
                    pdc.set(FurnitureMechanic.EVOLUTION_KEY, PersistentDataType.INTEGER, 0);
                    return;
                }
            }

            EvolvingFurniture evolution = mechanic.getEvolution();
            if (evolution == null) return;

            int lightBoostTick = 0;
            int rainBoostTick = 0;

            if (evolution.isLightBoosted() && entityLoc.getBlock().getLightLevel() >= evolution.getMinimumLightLevel())
                lightBoostTick = evolution.getLightBoostTick();

            if (evolution.isRainBoosted() && world.hasStorm() && world.getHighestBlockAt(entityLoc).getY() > entityLoc.getY())
                rainBoostTick = evolution.getRainBoostTick();

            int evolutionStep = pdc.get(EVOLUTION_KEY, PersistentDataType.INTEGER) + delay + lightBoostTick + rainBoostTick;

            if (evolutionStep > evolution.getDelay()) {
                if (evolution.getNextStage() == null) return;
                if (!evolution.bernoulliTest()) return;

                FurnitureMechanic nextMechanic = (FurnitureMechanic) furnitureFactory.getMechanic(evolution.getNextStage());
                if (nextMechanic == null) return;

                OraxenFurniture.remove(entity, null);
                nextMechanic.place(entity.getLocation(), entity.getLocation().getYaw(), entity.getFacing());
                //OraxenFurniture.place(entity.getLocation(), evolution.getNextStage(), FurnitureMechanic.yawToRotation(entity.getLocation().getYaw()), entity.getFacing());
                //nextMechanic.place(entityLoc, entityLoc.getYaw(), FurnitureMechanic.yawToRotation(entityLoc.getYaw()), entity.getFacing());
            } else pdc.set(FurnitureMechanic.EVOLUTION_KEY, PersistentDataType.INTEGER, evolutionStep);
        }, null);
    }
}
