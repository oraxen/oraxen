package io.th0rgal.oraxen.compatibilities.provided.worldedit;

import com.sk89q.jnbt.ByteArrayTag;
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
import io.th0rgal.oraxen.utils.UUIDUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
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
        furnitureTypes.add(BukkitAdapter.adapt(EntityType.ITEM_DISPLAY));
        furnitureTypes.add(BukkitAdapter.adapt(EntityType.INTERACTION));
    }


    @Subscribe
    public void onEditSession(EditSessionEvent event) {
        if (event.getWorld() == null) return;

        event.setExtent(new AbstractDelegateExtent(event.getExtent()) {
            private final Map<UUID, UUID> oldToNewUUIDs = new HashMap<>();

            @Override
            public Entity createEntity(com.sk89q.worldedit.util.Location location, BaseEntity baseEntity) {
                // If entity isnt a furniture, we return
                Entity worldEditEntity = getExtent().createEntity(location, baseEntity);
                if (worldEditEntity == null) return null;
                if (baseEntity == null || !baseEntity.hasNbtData() || !furnitureTypes.contains(baseEntity.getType()))
                    return worldEditEntity;
                FurnitureMechanic mechanic = getFurnitureMechanic(baseEntity);
                if (mechanic == null) return worldEditEntity;

                Location bukkitLocation = BukkitAdapter.adapt(BukkitAdapter.adapt(event.getWorld()), location);
                // UUID of the original entity
                UUID previousUUID = UUID.nameUUIDFromBytes(baseEntity.getNbtData().getByteArray("UUID"));
                BaseEntity worldEditState = worldEditEntity.getState();
                UUID newUUID = UUID.nameUUIDFromBytes(worldEditEntity.getState().getNbtData().getByteArray("UUID"));

                // We store the old UUID and the new UUID in a map
                oldToNewUUIDs.put(previousUUID, newUUID);

                CompoundTag compoundTag = worldEditState.getNbtData();
                Map<String, Tag> bukkitValues = new HashMap<>((Map<String, Tag>) compoundTag.getValue().get("BukkitValues").getValue());
                // If the entity-nbt has "oraxen:interaction", we know it is the BaseEntity
                // And update the interaction-uuid link

                // If the entity-nbt has "oraxen:base_entity", we know it is the hitbox
                // and we update the base_entity-uuid link
                // Otherwise we know it is the base-entity and get the old interaction tag
                String linkedEntityTagID = baseEntity.getType() == BukkitAdapter.adapt(EntityType.INTERACTION)
                        ? "oraxen:base_entity" : "oraxen:interaction";
                Tag linkedEntityTag = bukkitValues.get(linkedEntityTagID);

                UUID oldInteractionUUID = UUID.nameUUIDFromBytes((byte[]) linkedEntityTag.getValue());
                UUID newInteractionUUID = oldToNewUUIDs.get(oldInteractionUUID);

                // Set the hitbox/base-entity uuid link
                // TODO handle incase oldToNewUUIDs returns null
                if (newInteractionUUID != null)
                    bukkitValues.put(linkedEntityTagID, new ByteArrayTag(UUIDUtils.uuidToByteArray(newInteractionUUID)));
                bukkitValues.remove(linkedEntityTagID);

                worldEditState.setNbtData(compoundTag.setValue(bukkitValues));

                return worldEditEntity;
            }

            @Override
            public <T extends BlockStateHolder<T>> boolean setBlock(BlockVector3 pos, T block) throws WorldEditException {
                BlockData blockData = BukkitAdapter.adapt(block);
                World world = Bukkit.getWorld(event.getWorld().getName());
                Location loc = new Location(world, pos.getX(), pos.getY(), pos.getZ());
                Mechanic mechanic = OraxenBlocks.getOraxenBlock(blockData);
                if (blockData.getMaterial() == Material.NOTE_BLOCK) {
                    if (mechanic != null && Settings.WORLDEDIT_NOTEBLOCKS.toBool()) {
                        Bukkit.getScheduler().scheduleSyncDelayedTask(OraxenPlugin.get(), () -> OraxenBlocks.place(mechanic.getItemID(), loc));
                    }
                } else if (blockData.getMaterial() == Material.TRIPWIRE) {
                    if (mechanic != null && Settings.WORLDEDIT_STRINGBLOCKS.toBool()) {
                        Bukkit.getScheduler().scheduleSyncDelayedTask(OraxenPlugin.get(), () -> OraxenBlocks.place(mechanic.getItemID(), loc));
                    }
                } else {
                    if (world == null) return super.setBlock(pos, block);
                    Mechanic replacingMechanic = OraxenBlocks.getOraxenBlock(loc);
                    if (replacingMechanic == null) return super.setBlock(pos, block);
                    if (replacingMechanic instanceof StringBlockMechanic && !Settings.WORLDEDIT_STRINGBLOCKS.toBool())
                        return super.setBlock(pos, block);
                    if (replacingMechanic instanceof NoteBlockMechanic && !Settings.WORLDEDIT_NOTEBLOCKS.toBool())
                        return super.setBlock(pos, block);

                    Bukkit.getScheduler().scheduleSyncDelayedTask(OraxenPlugin.get(), () -> OraxenBlocks.remove(loc, null));
                }

                return getExtent().setBlock(pos, block);
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
                String furnitureId = bukkitValues.get("oraxen:furniture").getValue().toString();
                return OraxenFurniture.getFurnitureMechanic(furnitureId);
            }
        });
    }
}
