package io.th0rgal.oraxen.mechanics.provided.gameplay.furniture;

import com.jeff_media.customblockdata.CustomBlockData;
import com.jeff_media.morepersistentdatatypes.DataType;
import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.api.OraxenFurniture;
import io.th0rgal.oraxen.config.Settings;
import io.th0rgal.oraxen.utils.BlockHelpers;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.EntitiesLoadEvent;
import org.bukkit.persistence.PersistentDataType;

import static io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.FurnitureMechanic.ORIENTATION_KEY;
import static io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.FurnitureMechanic.ROOT_KEY;

public class FurnitureUpdater implements Listener {

    public FurnitureUpdater() {
        if (!Settings.UPDATE_FURNITURE.toBool()) return;
        if (Settings.UPDATE_FURNITURE_ON_LOAD.toBool()) {
            Bukkit.getPluginManager().registerEvent(EntitiesLoadEvent.class, this, EventPriority.NORMAL, (listener, event) ->
                    ((EntitiesLoadEvent) event).getEntities().stream().filter(OraxenFurniture::isBaseEntity).forEach(OraxenFurniture::updateFurniture)
                    , OraxenPlugin.get());
        }

        if (Settings.EXPERIMENTAL_FIX_BROKEN_FURNITURE.toBool()) {
            Bukkit.getPluginManager().registerEvent(ChunkLoadEvent.class, this, EventPriority.NORMAL, ((listener, event) -> {
                for (Block block : CustomBlockData.getBlocksWithCustomData(OraxenPlugin.get(), ((ChunkLoadEvent) event).getChunk())) {
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
            }), OraxenPlugin.get());
        }
    }
}
