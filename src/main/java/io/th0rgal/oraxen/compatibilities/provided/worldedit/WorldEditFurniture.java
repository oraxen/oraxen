package io.th0rgal.oraxen.compatibilities.provided.worldedit;

import com.destroystokyo.paper.event.entity.EntityAddToWorldEvent;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.event.extent.EditSessionEvent;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.extent.AbstractDelegateExtent;
import com.sk89q.worldedit.history.changeset.ChangeSet;
import com.sk89q.worldedit.entity.BaseEntity;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.world.biome.BiomeType;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.util.eventbus.Subscribe;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import com.sk89q.worldedit.world.block.BlockTypes;
import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.api.OraxenFurniture;
import io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.FurnitureMechanic;
import io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.text.FurnitureTextRegistry;
import io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.text.FurnitureTextPacketBridge;
import io.th0rgal.oraxen.utils.BlockHelpers;
import com.jeff_media.morepersistentdatatypes.DataType;
import io.th0rgal.oraxen.utils.PluginUtils;
import io.th0rgal.oraxen.utils.SchedulerUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemFrame;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;
import java.util.UUID;
import java.util.Objects;
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

import static io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.FurnitureMechanic.*;

/** Records furniture removals in the same history as their WorldEdit block changes. */
public final class WorldEditFurniture implements Listener {
    private static final NamespacedKey restoreKey = new NamespacedKey(OraxenPlugin.get(), "worldedit_restore");
    private static WorldEditFurniture listener;

    public static void register() {
        if (listener != null) return;
        listener = new WorldEditFurniture();
        WorldEdit.getInstance().getEventBus().register(listener);
        Bukkit.getPluginManager().registerEvents(listener, OraxenPlugin.get());
    }

    public static void unregister() {
        if (listener == null) return;
        WorldEdit.getInstance().getEventBus().unregister(listener);
        HandlerList.unregisterAll(listener);
        listener = null;
    }

    @Subscribe
    public void onEditSession(EditSessionEvent event) {
        if (event.getStage() != EditSession.Stage.BEFORE_HISTORY || event.getWorld() == null) return;
        event.setExtent(new FurnitureExtent(event.getExtent(), BukkitAdapter.adapt(event.getWorld())));
    }

    @EventHandler
    public void onEntityRestore(EntityAddToWorldEvent event) {
        Entity entity = event.getEntity();
        if (!entity.getPersistentDataContainer().has(restoreKey, PersistentDataType.BYTE)) return;
        SchedulerUtil.runForEntity(entity, () -> {
            FurnitureMechanic mechanic = OraxenFurniture.getFurnitureMechanic(entity);
            if (mechanic == null) return;
            PersistentDataContainer data = entity.getPersistentDataContainer();
            PersistentDataContainer savedData = data.getAdapterContext().newPersistentDataContainer();
            data.copyTo(savedData, true);
            // Recreated hitboxes and seats have new UUIDs. Keep their new links while
            // restoring storage, evolution, and other data from WorldEdit's snapshot.
            for (NamespacedKey key : List.of(restoreKey, BASE_ENTITY_KEY, INTERACTION_KEY,
                    INTERACTIONS_KEY, SEAT_KEY, SEATS_KEY)) {
                savedData.remove(key);
                data.remove(key);
            }
            BlockFace facing = entity instanceof ItemFrame frame ? frame.getFacing() : BlockFace.UP;
            mechanic.setEntityData(entity, FurnitureMechanic.getFurnitureYaw(entity),
                    FurnitureMechanic.getFurnitureItem(entity), facing);
            savedData.copyTo(data, true);
            if (mechanic.hasTextDefinitions()) {
                var entry = FurnitureTextRegistry.register(entity, mechanic.getTextDefinitions());
                FurnitureTextPacketBridge.spawnForTrackedViewers(entry);
            }
        });
    }

    private static <T> T atLocation(Location location, Supplier<T> action) {
        if (Bukkit.isOwnedByCurrentRegion(location)) return action.get();
        CompletableFuture<T> result = new CompletableFuture<>();
        SchedulerUtil.runAtLocation(location, () -> {
            try {
                result.complete(action.get());
            } catch (Throwable error) {
                result.completeExceptionally(error);
            }
        });
        return result.join();
    }

    // FAWE's AbstractDelegateExtent forwards bulk edits directly to its delegate,
    // bypassing setBlock overrides. Extent's defaults dispatch through this hook.
    private static final class FurnitureExtent implements Extent {
        private final Extent extent;
        private final World world;

        private static ChangeSet findHistory(Object object, Set<Object> visited) {
            if (object == null || !visited.add(object)) return null;
            if (object instanceof ChangeSet history) return history;
            if (object instanceof AbstractDelegateExtent delegate) {
                ChangeSet history = findHistory(delegate.getExtent(), visited);
                if (history != null) return history;
            }
            for (String method : List.of("getChangeSet", "getProcessor", "getPostProcessor", "getBatchProcessors")) {
                try {
                    Object value = object.getClass().getMethod(method).invoke(object);
                    if (value instanceof Iterable<?> values) {
                        for (Object child : values) {
                            ChangeSet history = findHistory(child, visited);
                            if (history != null) return history;
                        }
                    } else {
                        ChangeSet history = findHistory(value, visited);
                        if (history != null) return history;
                    }
                } catch (NoSuchMethodException ignored) {
                } catch (ReflectiveOperationException error) {
                    throw new IllegalStateException("Could not access WorldEdit history", error);
                }
            }
            return null;
        }

        private FurnitureExtent(Extent extent, World world) {
            this.extent = extent;
            this.world = world;
        }

        @Override
        public BlockState getBlock(BlockVector3 position) { return extent.getBlock(position); }

        @Override
        public BaseBlock getFullBlock(BlockVector3 position) { return extent.getFullBlock(position); }

        @Override
        public BlockVector3 getMinimumPoint() { return extent.getMinimumPoint(); }

        @Override
        public BlockVector3 getMaximumPoint() { return extent.getMaximumPoint(); }

        @Override
        public BiomeType getBiome(BlockVector3 position) { return extent.getBiome(position); }

        @Override
        public boolean setBiome(BlockVector3 position, BiomeType biome) { return extent.setBiome(position, biome); }

        @Override
        public List<? extends com.sk89q.worldedit.entity.Entity> getEntities() { return extent.getEntities(); }

        @Override
        public List<? extends com.sk89q.worldedit.entity.Entity> getEntities(Region region) { return extent.getEntities(region); }

        @Override
        public com.sk89q.worldedit.entity.Entity createEntity(com.sk89q.worldedit.util.Location location, BaseEntity entity) {
            return extent.createEntity(location, entity);
        }

        @Override
        public Operation commit() { return extent.commit(); }

        // These methods belong to FAWE's Extent API. Forward queue controls while
        // retaining compatibility with the regular WorldEdit compile-time API.
        public boolean isQueueEnabled() { return (boolean) queueControl("isQueueEnabled"); }

        public void enableQueue() { queueControl("enableQueue"); }

        public void disableQueue() { queueControl("disableQueue"); }

        public void removeEntity(int x, int y, int z, UUID uuid) {
            invokeExtent("removeEntity", new Class<?>[]{int.class, int.class, int.class, UUID.class}, x, y, z, uuid);
        }

        public com.sk89q.worldedit.entity.Entity createEntity(com.sk89q.worldedit.util.Location location,
                                                            BaseEntity entity, UUID uuid) {
            return (com.sk89q.worldedit.entity.Entity) invokeExtent("createEntity",
                    new Class<?>[]{com.sk89q.worldedit.util.Location.class, BaseEntity.class, UUID.class}, location, entity, uuid);
        }

        private Object queueControl(String method) {
            return invokeExtent(method, new Class<?>[0]);
        }

        private Object invokeExtent(String method, Class<?>[] parameters, Object... arguments) {
            try {
                return Extent.class.getMethod(method, parameters).invoke(extent, arguments);
            } catch (InvocationTargetException error) {
                if (error.getCause() instanceof RuntimeException cause) throw cause;
                throw new IllegalStateException("Could not forward FAWE extent operation", error.getCause());
            } catch (ReflectiveOperationException error) {
                throw new IllegalStateException("Could not forward FAWE extent operation", error);
            }
        }

        // FAWE also dispatches edits through its coordinate overload.
        public <T extends BlockStateHolder<T>> boolean setBlock(int x, int y, int z, T block)
                throws WorldEditException {
            return setBlock(BlockVector3.at(x, y, z), block);
        }

        @Override
        public <T extends BlockStateHolder<T>> boolean setBlock(BlockVector3 position, T block)
                throws WorldEditException {
            if (block.getBlockType() == BlockTypes.BARRIER
                    || getBlock(position).getBlockType() != BlockTypes.BARRIER)
                return extent.setBlock(position, block);

            Location location = new Location(world, position.x(), position.y(), position.z());
            Entity baseEntity = atLocation(location, () -> {
                Block barrier = location.getBlock();
                FurnitureMechanic mechanic = OraxenFurniture.getFurnitureMechanic(barrier);
                return mechanic == null ? null : mechanic.getBaseEntity(barrier);
            });
            boolean changed = extent.setBlock(position, block);
            if (!changed || baseEntity == null) return changed;

            atLocation(baseEntity.getLocation(), () -> {
                if (!baseEntity.isValid()) return null;
                FurnitureMechanic mechanic = OraxenFurniture.getFurnitureMechanic(baseEntity);
                if (mechanic == null) return null;
                Location root = BlockHelpers.toCenterBlockLocation(baseEntity.getLocation());
                float yaw = FurnitureMechanic.getFurnitureYaw(baseEntity);
                if (mechanic.hasLimitedPlacing() && mechanic.getLimitedPlacing().isRoof()
                        && baseEntity instanceof org.bukkit.entity.ItemDisplay) yaw -= 180;
                List<Location> barriers = mechanic.getLocations(yaw, root, mechanic.getBarriers(baseEntity)).stream()
                        .filter(barrier -> baseEntity.getUniqueId().equals(BlockHelpers.getPDC(barrier.getBlock())
                                .get(BASE_ENTITY_KEY, DataType.UUID))).toList();

                // Store the model and PDC in the same history as its barriers.
                baseEntity.getPersistentDataContainer().set(restoreKey, PersistentDataType.BYTE, (byte) 1);
                var entityState = BukkitAdapter.adapt(baseEntity).getState();
                var snapshot = entityState.getNbt();
                Location entityLocation = baseEntity.getLocation();
                BlockVector3 entityPosition = BlockVector3.at(entityLocation.getX(), entityLocation.getY(), entityLocation.getZ());
                // FAWE stores history in batch processors, whose entity lookups
                // do not include the world's live furniture entities.
                ChangeSet history = PluginUtils.isEnabled("FastAsyncWorldEdit")
                        ? findHistory(extent, Collections.newSetFromMap(new IdentityHashMap<>())) : null;
                var entities = history == null ? extent.getEntities(new CuboidRegion(entityPosition, entityPosition)) : List.<com.sk89q.worldedit.entity.Entity>of();
                var tracked = entities.stream()
                        .filter(entity -> entity.getState() != null && entity.getState().getNbt() != null
                                && snapshot != null && snapshot.value().get("UUID") != null
                                && Objects.equals(snapshot.value().get("UUID"), entity.getState().getNbt().value().get("UUID")))
                        .findFirst().orElse(null);

                try {
                    // Record every barrier before the furniture removal clears it.
                    for (Location barrier : barriers) {
                        BlockVector3 barrierPosition = BlockVector3.at(barrier.getX(), barrier.getY(), barrier.getZ());
                        if (!barrierPosition.equals(position))
                            extent.setBlock(barrierPosition, BlockTypes.AIR.getDefaultState());
                    }
                } catch (WorldEditException error) {
                    throw new IllegalStateException("Could not record furniture barriers", error);
                }
                if (history != null) {
                    try {
                        history.getClass().getMethod("addEntityRemove", com.sk89q.jnbt.CompoundTag.class)
                                .invoke(history, entityState.getNbtData());
                    } catch (ReflectiveOperationException error) {
                        throw new IllegalStateException("Could not record FAWE furniture removal", error);
                    }
                }
                else if (tracked != null) tracked.remove();
                baseEntity.getPersistentDataContainer().remove(restoreKey);
                OraxenFurniture.remove(baseEntity, null);
                // Synchronous WorldEdit may have replaced the blocks already,
                // so normal furniture removal cannot identify their barrier PDC.
                barriers.forEach(barrier -> BlockHelpers.removePDC(barrier.getBlock()));
                return null;
            });
            atLocation(location, () -> {
                BlockHelpers.removePDC(location.getBlock());
                return null;
            });
            return true;
        }
    }
}
