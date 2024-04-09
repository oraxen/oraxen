package io.th0rgal.oraxen.compatibilities.provided.worldedit;

import com.sk89q.jnbt.CompoundTag;
import com.sk89q.jnbt.Tag;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.entity.BaseEntity;
import com.sk89q.worldedit.entity.Entity;
import com.sk89q.worldedit.event.extent.EditSessionEvent;
import com.sk89q.worldedit.extent.AbstractDelegateExtent;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.util.eventbus.Subscribe;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.api.OraxenBlocks;
import io.th0rgal.oraxen.api.OraxenFurniture;
import io.th0rgal.oraxen.config.Settings;
import io.th0rgal.oraxen.mechanics.Mechanic;
import io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.FurnitureMechanic;
import io.th0rgal.oraxen.mechanics.provided.gameplay.noteblock.NoteBlockMechanic;
import io.th0rgal.oraxen.mechanics.provided.gameplay.stringblock.StringBlockMechanic;
import io.th0rgal.oraxen.utils.VersionUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.EntityType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class WorldEditHandlers {


    public WorldEditHandlers(boolean register) {
        if (register) {
            WorldEdit.getInstance().getEventBus().register(this);
        } else {
            WorldEdit.getInstance().getEventBus().unregister(this);
        }
    }

    private static final List<com.sk89q.worldedit.world.entity.EntityType> furnitureTypes = new ArrayList<>();

    static {
        furnitureTypes.add(BukkitAdapter.adapt(EntityType.ITEM_FRAME));
        if (VersionUtil.atOrAbove("1.19.4")){
            furnitureTypes.add(BukkitAdapter.adapt(EntityType.ITEM_DISPLAY));
            furnitureTypes.add(BukkitAdapter.adapt(EntityType.INTERACTION));
        }
    }


    @Subscribe
    public void onEditSession(EditSessionEvent event) {
        if (event.getWorld() == null) return;

        event.setExtent(new AbstractDelegateExtent(event.getExtent()) {

            @Override
            public Entity createEntity(com.sk89q.worldedit.util.Location location, BaseEntity baseEntity) {
                if (!Settings.WORLDEDIT_FURNITURE.toBool()) return super.createEntity(location, baseEntity);
                if (baseEntity == null || baseEntity.getType() == BukkitAdapter.adapt(EntityType.INTERACTION)) return null;
                if (!baseEntity.hasNbtData() || !furnitureTypes.contains(baseEntity.getType()))
                    return super.createEntity(location, baseEntity);

                Location bukkitLocation = BukkitAdapter.adapt(BukkitAdapter.adapt(event.getWorld()), location);
                FurnitureMechanic mechanic = getFurnitureMechanic(baseEntity);
                if (mechanic == null) return super.createEntity(location, baseEntity);

                // Remove interaction-tag from baseEntity-nbt
                CompoundTag compoundTag = baseEntity.getNbtData();
                if (compoundTag == null) return super.createEntity(location, baseEntity);
                Map<String, Tag> compoundTagMap = new HashMap<>(compoundTag.getValue());
                Map<String, Tag> bukkitValues = new HashMap<>((Map<String, Tag>) compoundTagMap.get("BukkitValues").getValue());
                bukkitValues.remove("oraxen:interaction");
                compoundTagMap.put("BukkitValues", new CompoundTag(bukkitValues));
                baseEntity.setNbtData(new CompoundTag(compoundTagMap));

                Bukkit.getScheduler().scheduleSyncDelayedTask(OraxenPlugin.get(), () -> {
                    List<org.bukkit.entity.Entity> entities = bukkitLocation.getNearbyEntities(0.5, 0.5, 0.5).stream().toList();
                    List<org.bukkit.entity.Entity> nearbyEntities = entities.stream().sorted(Comparator.comparingDouble(entity -> entity.getLocation().distance(bukkitLocation))).toList();
                    nearbyEntities.stream().findFirst().ifPresent(e ->
                            mechanic.setEntityData(e, e.getLocation().getYaw(), BlockFace.NORTH));
                }, 1L);

                return super.createEntity(location, baseEntity);
            }

            @Override
            public <T extends BlockStateHolder<T>> boolean setBlock(BlockVector3 pos, T block) throws WorldEditException {
                BlockData blockData = BukkitAdapter.adapt(block);
                World world = Bukkit.getWorld(event.getWorld().getName());
                Location loc = new Location(world, pos.getX(), pos.getY(), pos.getZ());
                Mechanic mechanic = OraxenBlocks.getOraxenBlock(blockData);
                if (blockData.getMaterial() == Material.NOTE_BLOCK) {
                    if (mechanic != null && Settings.WORLDEDIT_NOTEBLOCKS.toBool()) {
                        Bukkit.getScheduler().scheduleSyncDelayedTask(OraxenPlugin.get(), () -> OraxenBlocks.place(mechanic.getItemID(), loc), 1L);
                    }
                } else if (blockData.getMaterial() == Material.TRIPWIRE) {
                    if (mechanic != null && Settings.WORLDEDIT_STRINGBLOCKS.toBool()) {
                        Bukkit.getScheduler().scheduleSyncDelayedTask(OraxenPlugin.get(), () -> OraxenBlocks.place(mechanic.getItemID(), loc), 1L);
                    }
                } else {
                    if (world == null) return super.setBlock(pos, block);
                    Mechanic replacingMechanic = OraxenBlocks.getOraxenBlock(loc);
                    if (replacingMechanic == null) return super.setBlock(pos, block);
                    if (replacingMechanic instanceof StringBlockMechanic && !Settings.WORLDEDIT_STRINGBLOCKS.toBool())
                        return super.setBlock(pos, block);
                    if (replacingMechanic instanceof NoteBlockMechanic && !Settings.WORLDEDIT_NOTEBLOCKS.toBool())
                        return super.setBlock(pos, block);

                    Bukkit.getScheduler().scheduleSyncDelayedTask(OraxenPlugin.get(), () -> OraxenBlocks.remove(loc, null), 1L);
                }

                return super.setBlock(pos, block);
            }

            @Nullable
            private FurnitureMechanic getFurnitureMechanic(@NotNull BaseEntity entity) {
                if (!entity.hasNbtData() || !furnitureTypes.contains(entity.getType())) return null;
                CompoundTag tag = entity.getNbtData();
                Map<String, Tag> bukkitValues = null;
                try {
                    bukkitValues = (Map<String, Tag>) tag.getValue().get("BukkitValues").getValue();
                } catch (Exception ignored) {
                }
                if (bukkitValues == null) return null;
                Tag furnitureTag = bukkitValues.get("oraxen:furniture");
                if (furnitureTag == null) return null;

                String furnitureId = furnitureTag.getValue().toString();
                return OraxenFurniture.getFurnitureMechanic(furnitureId);
            }
        });
    }
}
