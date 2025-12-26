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
import org.bukkit.block.data.type.Light;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import static io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.FurnitureMechanic.EVOLUTION_KEY;
import static io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.FurnitureMechanic.STAGE_INDEX_KEY;

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
        for (World world : Bukkit.getWorlds()) {
            for (Class<? extends Entity> entityClass : FurnitureMechanic.FurnitureType.furnitureEntityClasses()) {
                for (Entity entity : world.getEntitiesByClass(entityClass)) {
                    // Run entity operations on the entity's region thread for Folia compatibility
                    SchedulerUtil.runForEntity(entity, () -> processEvolution(entity, world));
                }
            }
        }
    }

    private void processEvolution(Entity entity, World world) {
        Location entityLoc = entity.getLocation();
        PersistentDataContainer pdc = entity.getPersistentDataContainer();
        if (!pdc.has(EVOLUTION_KEY, PersistentDataType.INTEGER)) return;

        Block blockBelow = entityLoc.getBlock().getRelative(BlockFace.DOWN);
        FurnitureMechanic mechanic = OraxenFurniture.getFurnitureMechanic(entity);
        if (mechanic == null) return;

        // Check farmland/farmblock requirements
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
                pdc.set(EVOLUTION_KEY, PersistentDataType.INTEGER, 0);
                return;
            }
        }

        // NEW: Handle staged evolution (model swapping, no entity recreation)
        if (mechanic.hasGrowthStages()) {
            processStageEvolution(entity, mechanic, pdc, entityLoc, world);
            return;
        }

        // Legacy: Handle single-stage evolution (entity recreation)
        processLegacyEvolution(entity, mechanic, pdc, entityLoc, world);
    }

    /**
     * Processes evolution for furniture using the new inline stages system.
     * Only swaps the model, preserves the entity.
     */
    private void processStageEvolution(Entity entity, FurnitureMechanic mechanic, 
                                        PersistentDataContainer pdc, Location entityLoc, World world) {
        int currentStageIndex = pdc.getOrDefault(STAGE_INDEX_KEY, PersistentDataType.INTEGER, 0);
        
        // Check if already at final stage
        if (mechanic.isFinalStage(currentStageIndex)) return;
        
        GrowthStage currentStage = mechanic.getGrowthStage(currentStageIndex);
        if (currentStage == null || !currentStage.hasEvolution()) return;
        
        // Calculate boost ticks
        int lightBoostTick = 0;
        int rainBoostTick = 0;
        
        if (currentStage.isLightBoosted() && 
            entityLoc.getBlock().getLightLevel() >= currentStage.getMinimumLightLevel()) {
            lightBoostTick = currentStage.getLightBoostTick();
        }
        
        if (currentStage.isRainBoosted() && world.hasStorm() && 
            world.getHighestBlockAt(entityLoc).getY() > entityLoc.getY()) {
            rainBoostTick = currentStage.getRainBoostTick();
        }
        
        // Update evolution progress
        int evolutionStep = pdc.getOrDefault(EVOLUTION_KEY, PersistentDataType.INTEGER, 0) 
                           + delay + lightBoostTick + rainBoostTick;
        
        if (evolutionStep > currentStage.getDelay()) {
            // Ready to evolve - check probability
            if (!currentStage.bernoulliTest()) {
                // Failed probability check, reset timer but stay at current stage
                pdc.set(EVOLUTION_KEY, PersistentDataType.INTEGER, 0);
                return;
            }
            
            // Advance to next stage
            int nextStageIndex = currentStageIndex + 1;
            GrowthStage nextStage = mechanic.getGrowthStage(nextStageIndex);
            if (nextStage == null) return;

            // Update stage index
            pdc.set(STAGE_INDEX_KEY, PersistentDataType.INTEGER, nextStageIndex);
            // Reset evolution timer
            pdc.set(EVOLUTION_KEY, PersistentDataType.INTEGER, 0);

            // Swap the model (no entity recreation!)
            String modelKey = nextStage.getModelKey();
            FurnitureMechanic.setFurnitureItemModel(entity, mechanic.getItemID(), modelKey);

            // Update light level if stage has per-stage light
            updateStageLight(entityLoc.getBlock(), currentStage, nextStage, mechanic);
        } else {
            // Not ready yet, update progress
            pdc.set(EVOLUTION_KEY, PersistentDataType.INTEGER, evolutionStep);
        }
    }

    /**
     * Processes evolution using the legacy system (separate items, entity recreation).
     */
    private void processLegacyEvolution(Entity entity, FurnitureMechanic mechanic, 
                                         PersistentDataContainer pdc, Location entityLoc, World world) {
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
        } else {
            pdc.set(EVOLUTION_KEY, PersistentDataType.INTEGER, evolutionStep);
        }
    }

    /**
     * Updates the light level when transitioning between growth stages.
     * Removes old light and creates new light if the stage light levels differ.
     */
    static void updateStageLight(Block block, GrowthStage oldStage, GrowthStage newStage, FurnitureMechanic mechanic) {
        int oldLight = oldStage != null ? oldStage.getLight() : -1;
        int newLight = newStage.getLight();

        // If new stage inherits from mechanic (-1), use mechanic's light level
        int effectiveNewLight = newLight == -1 && mechanic.hasLight() ? mechanic.getLight().getLightLevel() : newLight;
        int effectiveOldLight = oldLight == -1 && mechanic.hasLight() ? mechanic.getLight().getLightLevel() : oldLight;

        // No change needed if light levels are the same
        if (effectiveOldLight == effectiveNewLight) return;

        // Remove old light if it existed
        if (effectiveOldLight > 0) {
            removeLight(block, effectiveOldLight);
        }

        // Create new light if needed
        if (effectiveNewLight > 0) {
            createLight(block, effectiveNewLight);
        }
    }

    private static final BlockFace[] LIGHT_FACES = new BlockFace[]{
            BlockFace.UP, BlockFace.DOWN, BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST, BlockFace.SELF
    };

    private static void createLight(Block block, int lightLevel) {
        Light lightData = (Light) Material.LIGHT.createBlockData();
        lightData.setLevel(Math.min(15, lightLevel));

        if (block.getType().isAir()) {
            block.setBlockData(lightData);
        } else {
            for (BlockFace face : LIGHT_FACES) {
                Block relative = block.getRelative(face);
                if (!relative.getType().isAir() && relative.getType() != Material.LIGHT) continue;
                if (relative.getBlockData() instanceof Light relativeLight && relativeLight.getLevel() > lightLevel) continue;
                relative.setBlockData(lightData);
            }
        }
    }

    private static void removeLight(Block block, int lightLevel) {
        for (BlockFace face : LIGHT_FACES) {
            Block relative = block.getRelative(face);
            if (relative.getType() == Material.LIGHT) {
                relative.setType(Material.AIR);
            }
        }
    }
}
