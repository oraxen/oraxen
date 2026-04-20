package io.th0rgal.oraxen.mechanics.provided.gameplay.furniture;

import com.jeff_media.customblockdata.CustomBlockData;
import com.jeff_media.morepersistentdatatypes.DataType;
import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.api.OraxenFurniture;
import io.th0rgal.oraxen.config.Settings;
import io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.text.FurnitureTextRegistry;
import io.th0rgal.oraxen.utils.BlockHelpers;
import io.th0rgal.oraxen.utils.SchedulerUtil;
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
import org.bukkit.event.world.EntitiesUnloadEvent;
import org.bukkit.persistence.PersistentDataType;

import java.util.UUID;

import static io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.FurnitureMechanic.ORIENTATION_KEY;
import static io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.FurnitureMechanic.ROOT_KEY;

public class FurnitureUpdater implements Listener {

    public FurnitureUpdater() {
        Bukkit.getPluginManager().registerEvent(EntitiesLoadEvent.class, this, EventPriority.NORMAL, (listener, event) ->
                {
                    if (!FurnitureFactory.isEnabled()) return;
                    ((EntitiesLoadEvent) event).getEntities().stream().filter(OraxenFurniture::isBaseEntity).forEach(entity -> {
                        if (Settings.UPDATE_FURNITURE.toBool() && Settings.UPDATE_FURNITURE_ON_LOAD.toBool()) {
                            OraxenFurniture.updateFurniture(entity);
                        }
                        if (entity.isValid()) registerTextEntity(entity);
                    });
                }
                , OraxenPlugin.get());

        Bukkit.getPluginManager().registerEvent(EntitiesUnloadEvent.class, this, EventPriority.NORMAL, (listener, event) ->
                {
                    if (!FurnitureFactory.isEnabled()) return;
                    ((EntitiesUnloadEvent) event).getEntities().stream()
                            .filter(OraxenFurniture::isBaseEntity)
                            .forEach(entity -> {
                                UUID uuid = entity.getUniqueId();
                                int entityId = entity.getEntityId();
                                SchedulerUtil.runTask(() -> FurnitureTextRegistry.unregister(uuid, entityId));
                            });
                }
                , OraxenPlugin.get());

        if (Settings.UPDATE_FURNITURE.toBool() && Settings.EXPERIMENTAL_FIX_BROKEN_FURNITURE.toBool()) {
            Bukkit.getPluginManager().registerEvent(ChunkLoadEvent.class, this, EventPriority.NORMAL, ((listener, event) -> {
                if (!FurnitureFactory.isEnabled()) return;
                for (Block block : CustomBlockData.getBlocksWithCustomData(OraxenPlugin.get(), ((ChunkLoadEvent) event).getChunk())) {
                    FurnitureMechanic mechanic = OraxenFurniture.getFurnitureMechanic(block);
                    if (mechanic == null) {
                        if (OraxenFurniture.hasFurnitureBlockMarker(block)) OraxenFurniture.remove(block.getLocation(), null);
                        continue;
                    }
                    Entity baseEntity = mechanic.getBaseEntity(block);
                    // Skip if there is a baseEntity
                    if (baseEntity != null) continue;

                    Location rootLoc = new BlockLocation(BlockHelpers.getPDC(block).getOrDefault(ROOT_KEY, DataType.STRING, "")).toLocation(block.getWorld());
                    float yaw = BlockHelpers.getPDC(block).getOrDefault(ORIENTATION_KEY, PersistentDataType.FLOAT, 0f);
                    if (rootLoc == null) continue;

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

    private static void registerTextEntity(Entity entity) {
        FurnitureMechanic mechanic = OraxenFurniture.getFurnitureMechanic(entity);
        if (mechanic != null && mechanic.hasTextDefinitions())
            FurnitureTextRegistry.register(entity, mechanic.getTextDefinitions());
    }
}
