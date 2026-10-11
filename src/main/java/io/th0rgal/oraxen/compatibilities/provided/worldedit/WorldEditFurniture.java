package io.th0rgal.oraxen.compatibilities.provided.worldedit;

import com.destroystokyo.paper.event.entity.EntityAddToWorldEvent;
import com.destroystokyo.paper.event.entity.EntityRemoveFromWorldEvent;
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
import org.bukkit.entity.ItemDisplay;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;
import java.util.UUID;
import java.util.Objects;
import java.util.Map;
import java.util.HashSet;
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import static io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.FurnitureMechanic.*;

/** Records furniture removals in the same history as their WorldEdit block changes. */
public final class WorldEditFurniture implements Listener {
    private static final NamespacedKey restoreKey = new NamespacedKey(OraxenPlugin.get(), "worldedit_restore");
    private static final NamespacedKey restoredKey = new NamespacedKey(OraxenPlugin.get(), "worldedit_restored");
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
            for (NamespacedKey key : List.of(restoreKey, restoredKey, BASE_ENTITY_KEY, INTERACTION_KEY,
                    INTERACTIONS_KEY, SEAT_KEY, SEATS_KEY)) {
                savedData.remove(key);
                data.remove(key);
            }
            Location savedLocation = entity.getLocation();
            ItemStack savedItem = FurnitureMechanic.getFurnitureItem(entity);
            if (savedItem != null) savedItem = savedItem.clone();
            float placementYaw = FurnitureMechanic.getFurnitureYaw(entity);
            BlockFace facing = entity instanceof ItemFrame frame ? frame.getFacing() : BlockFace.UP;
            if (entity instanceof ItemDisplay display
                    && display.getItemDisplayTransform() == ItemDisplay.ItemDisplayTransform.FIXED
                    && mechanic.hasLimitedPlacing() && mechanic.getLimitedPlacing().isRoof()
                    && savedLocation.getPitch() == 90f) {
                facing = BlockFace.DOWN;
                // Placement subtracts 180 degrees from the display yaw, but keeps
                // the original yaw for barriers, interactions, and seats.
                placementYaw += 180f;
            }
            mechanic.setEntityData(entity, placementYaw, savedItem != null ? savedItem.clone() : null, facing);
            savedData.copyTo(data, true);
            data.set(restoredKey, PersistentDataType.BYTE, (byte) 1);
            // Initialisation applies the initial growth model. Undo must retain
            // the snapshot's item, including its current growth stage model.
            FurnitureMechanic.setFurnitureItem(entity, savedItem);
            if (entity instanceof ItemDisplay)
                entity.setRotation(savedLocation.getYaw(), savedLocation.getPitch());
            if (mechanic.hasTextDefinitions()) {
                var entry = FurnitureTextRegistry.register(entity, mechanic.getTextDefinitions());
                FurnitureTextPacketBridge.spawnForTrackedViewers(entry);
            }
        });
    }

    @EventHandler
    public void onRestoredEntityRemoval(EntityRemoveFromWorldEvent event) {
        Entity entity = event.getEntity();
        PersistentDataContainer data = entity.getPersistentDataContainer();
        if (!entity.isDead() || !data.has(restoredKey, PersistentDataType.BYTE)) return;
        // History only records the base. Children recreated by undo need their own cleanup.
        Set<UUID> children = new HashSet<>();
        children.addAll(data.getOrDefault(SEATS_KEY, DataType.asList(DataType.UUID), List.of()));
        children.addAll(data.getOrDefault(INTERACTIONS_KEY, DataType.asList(DataType.UUID), List.of()));
        for (NamespacedKey key : List.of(SEAT_KEY, INTERACTION_KEY)) {
            UUID uuid = data.get(key, DataType.UUID);
            if (uuid != null) children.add(uuid);
        }
        for (UUID uuid : children) {
            Entity child = Bukkit.getEntity(uuid);
            if (child != null) SchedulerUtil.runOnOwningThread(child, () -> {
                child.getPassengers().forEach(child::removePassenger);
                if (!child.isDead()) child.remove();
            });
        }
        UUID uuid = entity.getUniqueId();
        int id = entity.getEntityId();
        SchedulerUtil.runTask(() -> FurnitureTextRegistry.unregister(uuid, id));
    }

    private static <T> T atEntity(Entity entity, Supplier<T> action) {
        if (Bukkit.isOwnedByCurrentRegion(entity)) return action.get();
        CompletableFuture<T> result = new CompletableFuture<>();
        SchedulerUtil.ScheduledTask task = SchedulerUtil.runForEntity(entity, () -> {
            try {
                result.complete(action.get());
            } catch (Throwable error) {
                result.completeExceptionally(error);
            }
        }, () -> result.complete(null));
        if (task == null) result.complete(null);
        return result.join();
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
        private final Map<Long, Set<BlockVector3>> furnitureBlocks = new ConcurrentHashMap<>();

        private boolean mayContainFurniture(BlockVector3 position, Location location) {
            // Scan block PDC on the owning region once per chunk and keep only
            // immutable coordinates on FAWE workers. Ordinary barriers then
            // avoid a separate scheduler round-trip for every edited block.
            int chunkX = position.x() >> 4;
            int chunkZ = position.z() >> 4;
            long chunkKey = ((long) chunkX << 32) | (chunkZ & 0xffffffffL);
            return furnitureBlocks.computeIfAbsent(chunkKey, key -> atLocation(location, () -> {
                Set<BlockVector3> positions = new HashSet<>();
                for (Block candidate : BlockHelpers.getBlocksWithCustomData(OraxenPlugin.get(),
                        world.getChunkAt(chunkX, chunkZ))) {
                    if (OraxenFurniture.getFurnitureMechanic(candidate) != null)
                        positions.add(BlockVector3.at(candidate.getX(), candidate.getY(), candidate.getZ()));
                }
                return Set.copyOf(positions);
            })).contains(position);
        }

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

        private record FurnitureRemoval(UUID uuid, List<Location> barriers) {}

        private void rollback(List<BlockVector3> changed, Map<BlockVector3, BaseBlock> originals)
                throws WorldEditException {
            for (int index = changed.size() - 1; index >= 0; index--) {
                BlockVector3 position = changed.get(index);
                if (!extent.setBlock(position, originals.get(position)))
                    throw new IllegalStateException("WorldEdit rejected furniture rollback at " + position);
            }
        }

        @Override
        public <T extends BlockStateHolder<T>> boolean setBlock(BlockVector3 position, T block)
                throws WorldEditException {
            if (block.getBlockType() == BlockTypes.BARRIER
                    || getBlock(position).getBlockType() != BlockTypes.BARRIER)
                return extent.setBlock(position, block);

            Location location = new Location(world, position.x(), position.y(), position.z());
            if (!Bukkit.isOwnedByCurrentRegion(location) && !mayContainFurniture(position, location))
                return extent.setBlock(position, block);
            Entity baseEntity = atLocation(location, () -> {
                Block barrier = location.getBlock();
                FurnitureMechanic mechanic = OraxenFurniture.getFurnitureMechanic(barrier);
                return mechanic == null ? null : mechanic.getBaseEntity(barrier);
            });
            if (baseEntity == null) return extent.setBlock(position, block);

            FurnitureRemoval removal = atEntity(baseEntity, () -> {
                if (!baseEntity.isValid()) return null;
                FurnitureMechanic mechanic = OraxenFurniture.getFurnitureMechanic(baseEntity);
                if (mechanic == null) return null;
                Location root = BlockHelpers.toCenterBlockLocation(baseEntity.getLocation());
                float yaw = FurnitureMechanic.getFurnitureYaw(baseEntity);
                if (mechanic.hasLimitedPlacing() && mechanic.getLimitedPlacing().isRoof()
                        && baseEntity instanceof ItemDisplay display
                        && display.getItemDisplayTransform() == ItemDisplay.ItemDisplayTransform.FIXED
                        && baseEntity.getLocation().getPitch() == 90f) yaw += 180;
                return new FurnitureRemoval(baseEntity.getUniqueId(),
                        mechanic.getLocations(yaw, root, mechanic.getBarriers(baseEntity)));
            });
            if (removal == null) return false;
            List<Location> barriers = new ArrayList<>();
            for (Location barrier : removal.barriers()) {
                boolean owned = atLocation(barrier, () -> removal.uuid().equals(
                        BlockHelpers.getPDC(barrier.getBlock()).get(BASE_ENTITY_KEY, DataType.UUID)));
                if (owned) barriers.add(barrier);
            }
            Map<BlockVector3, BaseBlock> originals = new LinkedHashMap<>();
            originals.put(position, extent.getFullBlock(position));
            for (Location barrier : barriers) {
                BlockVector3 barrierPosition = BlockVector3.at(barrier.getX(), barrier.getY(), barrier.getZ());
                originals.putIfAbsent(barrierPosition, extent.getFullBlock(barrierPosition));
            }
            List<BlockVector3> changed = new ArrayList<>();
            boolean complete = true;
            try {
                for (BlockVector3 barrierPosition : originals.keySet()) {
                    boolean accepted = barrierPosition.equals(position)
                            ? extent.setBlock(barrierPosition, block)
                            : extent.setBlock(barrierPosition, BlockTypes.AIR.getDefaultState());
                    if (!accepted) {
                        complete = false;
                        break;
                    }
                    changed.add(barrierPosition);
                }
            } catch (WorldEditException | RuntimeException error) {
                rollback(changed, originals);
                throw error;
            }
            if (!complete) {
                rollback(changed, originals);
                return false;
            }
            // Keep every barrier's ownership data until all writes have succeeded.
            for (Location barrier : barriers) atLocation(barrier, () -> {
                BlockHelpers.removePDC(barrier.getBlock());
                return null;
            });
            atLocation(location, () -> {
                BlockHelpers.removePDC(location.getBlock());
                return null;
            });
            atEntity(baseEntity, () -> {
                if (!baseEntity.isValid()) return null;
                FurnitureMechanic mechanic = OraxenFurniture.getFurnitureMechanic(baseEntity);
                if (mechanic == null) return null;
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
                // WorldEdit alone changes barriers, so denied positions and fallback
                // recovery blocks are never cleared directly through Bukkit.
                mechanic.removeNonSolidFurniture(baseEntity);
                return null;
            });
            return true;
        }
    }
}
