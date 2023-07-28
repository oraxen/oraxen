package io.th0rgal.oraxen.mechanics.provided.gameplay.furniture;

import com.jeff_media.customblockdata.CustomBlockData;
import com.jeff_media.morepersistentdatatypes.DataType;
import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.api.OraxenFurniture;
import io.th0rgal.oraxen.config.Settings;
import io.th0rgal.oraxen.utils.BlockHelpers;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.EntitiesLoadEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashSet;

import static io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.FurnitureMechanic.ORIENTATION_KEY;
import static io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.FurnitureMechanic.ROOT_KEY;

public class FurnitureUpdater implements Listener {

    public FurnitureUpdater() {
        if (furnitureUpdateTask != null) furnitureUpdateTask.cancel();
        furnitureUpdateTask = new FurnitureUpdateTask();
        int delay = (Settings.FURNITURE_UPDATE_DELAY.getValue() instanceof Integer integer) ? integer : 5;
        if (delay <= 0) return;
        furnitureUpdateTask.runTaskTimer(OraxenPlugin.get(), 0, delay * 20L);
    }

    public static HashSet<Entity> furnitureToUpdate = new HashSet<>();
    public static FurnitureUpdateTask furnitureUpdateTask;
    public static class FurnitureUpdateTask extends BukkitRunnable {

        @Override
        public void run() {
            for (Entity entity : new HashSet<>(furnitureToUpdate)) {
                OraxenFurniture.updateFurniture(entity);
                furnitureToUpdate.remove(entity);
            }
        }
    }

    @EventHandler
    public void onEntityLoad(EntitiesLoadEvent event) {
        if (!Settings.UPDATE_FURNITURE.toBool() || !Settings.UPDATE_FURNITURE_ON_LOAD.toBool()) return;

        for (Entity entity : event.getEntities())
            if (OraxenFurniture.isFurniture(entity))
                furnitureToUpdate.add(entity);
    }

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        if (!Settings.EXPERIMENTAL_FIX_BROKEN_FURNITURE.toBool()) return;

        for (Block block : CustomBlockData.getBlocksWithCustomData(OraxenPlugin.get(), event.getChunk())) {
            FurnitureMechanic mechanic = OraxenFurniture.getFurnitureMechanic(block);
            if (mechanic == null) return;
            Entity baseEntity = mechanic.getBaseEntity(block);
            // Return if there is a baseEntity
            if (baseEntity != null) return;

            Location rootLoc = new BlockLocation(BlockHelpers.getPDC(block).getOrDefault(ROOT_KEY, DataType.STRING, "")).toLocation(block.getWorld());
            float yaw = BlockHelpers.getPDC(block).getOrDefault(ORIENTATION_KEY, PersistentDataType.FLOAT, 0f);
            if (rootLoc == null) return;

            //OraxenFurniture.remove(block.getLocation(), null);
            mechanic.getLocations(yaw, rootLoc, mechanic.getBarriers()).forEach(loc -> {
                loc.getBlock().setType(Material.AIR);
                new CustomBlockData(loc.getBlock(), OraxenPlugin.get()).clear();
            });
            mechanic.place(rootLoc, yaw, BlockFace.UP);
        }
    }


}
