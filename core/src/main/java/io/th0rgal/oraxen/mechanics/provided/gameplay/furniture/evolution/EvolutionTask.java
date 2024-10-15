package io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.evolution;

import io.th0rgal.oraxen.api.OraxenFurniture;
import io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.FurnitureFactory;
import io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.FurnitureMechanic;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
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
        for (World world : Bukkit.getWorlds()) for (Entity entity : world.getEntitiesByClass(ItemDisplay.class)) {
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

            EvolvingFurniture evolution = mechanic.evolution();
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

                FurnitureMechanic nextMechanic = furnitureFactory.getMechanic(evolution.getNextStage());
                if (nextMechanic == null) continue;

                OraxenFurniture.remove(entity, null);
                nextMechanic.place(entity.getLocation());
            } else pdc.set(FurnitureMechanic.EVOLUTION_KEY, PersistentDataType.INTEGER, evolutionStep);
        }
    }
}
