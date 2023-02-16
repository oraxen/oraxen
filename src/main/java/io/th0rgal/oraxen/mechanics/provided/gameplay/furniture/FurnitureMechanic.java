package io.th0rgal.oraxen.mechanics.provided.gameplay.furniture;

import com.jeff_media.customblockdata.CustomBlockData;
import com.jeff_media.morepersistentdatatypes.DataType;
import com.ticxo.modelengine.api.ModelEngineAPI;
import com.ticxo.modelengine.api.model.ActiveModel;
import com.ticxo.modelengine.api.model.ModeledEntity;
import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.api.OraxenFurniture;
import io.th0rgal.oraxen.api.OraxenItems;
import io.th0rgal.oraxen.compatibilities.CompatibilitiesManager;
import io.th0rgal.oraxen.compatibilities.provided.lightapi.WrappedLightAPI;
import io.th0rgal.oraxen.mechanics.Mechanic;
import io.th0rgal.oraxen.mechanics.MechanicFactory;
import io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.evolution.EvolvingFurniture;
import io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.jukebox.JukeboxBlock;
import io.th0rgal.oraxen.utils.BlockHelpers;
import io.th0rgal.oraxen.utils.actions.ClickAction;
import io.th0rgal.oraxen.utils.blocksounds.BlockSounds;
import io.th0rgal.oraxen.utils.drops.Drop;
import io.th0rgal.oraxen.utils.drops.Loot;
import io.th0rgal.oraxen.utils.limitedplacing.LimitedPlacing;
import io.th0rgal.oraxen.utils.storage.StorageMechanic;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.*;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;

public class FurnitureMechanic extends Mechanic {

    public static final NamespacedKey FURNITURE_KEY = new NamespacedKey(OraxenPlugin.get(), "furniture");
    public static final NamespacedKey MODELENGINE_KEY = new NamespacedKey(OraxenPlugin.get(), "modelengine");
    public static final NamespacedKey SEAT_KEY = new NamespacedKey(OraxenPlugin.get(), "seat");
    public static final NamespacedKey ROOT_KEY = new NamespacedKey(OraxenPlugin.get(), "root");
    public static final NamespacedKey ORIENTATION_KEY = new NamespacedKey(OraxenPlugin.get(), "orientation");
    public static final NamespacedKey ROTATION_KEY = new NamespacedKey(OraxenPlugin.get(), "rotation");
    public static final NamespacedKey EVOLUTION_KEY = new NamespacedKey(OraxenPlugin.get(), "evolution");
    private final LimitedPlacing limitedPlacing;
    private final StorageMechanic storage;
    private final BlockSounds blockSounds;
    private final JukeboxBlock jukebox;
    public final boolean farmlandRequired;
    public final boolean farmblockRequired;
    private final List<BlockLocation> barriers;
    private final boolean hasSeat;
    private boolean hasSeatYaw;
    private final Drop drop;
    private final EvolvingFurniture evolvingFurniture;
    private final int light;
    private final boolean glowing;
    private final String modelEngineID;
    private final String placedItemId;
    private ItemStack placedItem;
    private float seatHeight;
    private float seatYaw;
    private final List<ClickAction> clickActions;

    @SuppressWarnings("unchecked")
    public FurnitureMechanic(MechanicFactory mechanicFactory, ConfigurationSection section) {
        super(mechanicFactory, section, itemBuilder -> itemBuilder.setCustomTag(FURNITURE_KEY,
                PersistentDataType.BYTE, (byte) 1));

        placedItemId = section.getString("item", null);

        modelEngineID = section.getString("modelengine_id", null);

        barriers = new ArrayList<>();
        if (CompatibilitiesManager.hasPlugin("ProtocolLib")) {
            if (section.getBoolean("barrier", false))
                barriers.add(new BlockLocation(0, 0, 0));
            if (section.isList("barriers"))
                for (Object barrierObject : section.getList("barriers", new ArrayList<>()))
                    barriers.add(new BlockLocation((Map<String, Object>) barrierObject));
        }

        ConfigurationSection seatSection = section.getConfigurationSection("seat");
        if (seatSection != null) {
            hasSeat = true;
            seatHeight = (float) seatSection.getDouble("height");
            if (seatSection.contains("yaw")) {
                hasSeatYaw = true;
                seatYaw = (float) seatSection.getDouble("yaw");
            } else {
                hasSeatYaw = false;
            }
        } else
            hasSeat = false;

        ConfigurationSection evoSection = section.getConfigurationSection("evolution");
        if (evoSection != null) {
            evolvingFurniture = new EvolvingFurniture(getItemID(), evoSection);
            ((FurnitureFactory) getFactory()).registerEvolution();
        } else evolvingFurniture = null;

        light = section.getInt("light", -1);
        glowing = section.getBoolean("glowing", false);

        farmlandRequired = section.getBoolean("farmland_required", false);
        farmblockRequired = section.getBoolean("farmblock_required", false);

        List<Loot> loots = new ArrayList<>();
        if (section.isConfigurationSection("drop")) {
            ConfigurationSection drop = section.getConfigurationSection("drop");
            if (drop != null) {
                for (LinkedHashMap<String, Object> lootConfig : (List<LinkedHashMap<String, Object>>) drop.getList("loots", new ArrayList<>()))
                    loots.add(new Loot(lootConfig));

                if (drop.isString("minimal_type")) {
                    FurnitureFactory mechanic = (FurnitureFactory) mechanicFactory;
                    List<String> bestTools = drop.isList("best_tools") ? drop.getStringList("best_tools") : new ArrayList<>();
                    this.drop = new Drop(mechanic.toolTypes, loots, drop.getBoolean("silktouch"),
                            drop.getBoolean("fortune"), getItemID(),
                            drop.getString("minimal_type"),
                            bestTools);
                } else this.drop =
                        new Drop(loots, drop.getBoolean("silktouch", false), drop.getBoolean("fortune", false), getItemID());
            } else this.drop = new Drop(loots, false, false, getItemID());
        } else drop = new Drop(loots, false, false, getItemID());

        if (section.isConfigurationSection("limited_placing")) {
            limitedPlacing = new LimitedPlacing(Objects.requireNonNull(section.getConfigurationSection("limited_placing")));
        } else limitedPlacing = null;

        if (section.isConfigurationSection("storage")) {
            storage = new StorageMechanic(Objects.requireNonNull(section.getConfigurationSection("storage")));
        } else storage = null;

        if (section.isConfigurationSection("block_sounds")) {
            blockSounds = new BlockSounds(Objects.requireNonNull(section.getConfigurationSection("block_sounds")));
        } else blockSounds = null;

        if (section.isConfigurationSection("jukebox")) {
            jukebox = new JukeboxBlock(mechanicFactory, Objects.requireNonNull(section.getConfigurationSection("jukebox")));
        } else jukebox = null;

        clickActions = ClickAction.parseList(section);
    }

    public boolean isModelEngine() {
        return modelEngineID != null;
    }

    public String getModelEngineID() {
        return modelEngineID;
    }

    public static ArmorStand getSeat(Location location) {
        Location seatLoc = location.clone().add(0.5, 0.0, 0.5);
        if (location.getWorld() == null) return null;
        for (Entity entity : location.getWorld().getNearbyEntities(seatLoc, 0.1, 10, 0.1)) {
            if (entity instanceof ArmorStand seat
                    && entity.getLocation().getX() == seatLoc.getX()
                    && entity.getLocation().getZ() == seatLoc.getZ()
                    && entity.getPersistentDataContainer().has(SEAT_KEY, PersistentDataType.STRING)) {
                return seat;
            }
        }
        return null;
    }

    public boolean hasLimitedPlacing() {
        return limitedPlacing != null;
    }

    public LimitedPlacing getLimitedPlacing() {
        return limitedPlacing;
    }

    public boolean isStorage() {
        return storage != null;
    }

    public StorageMechanic getStorage() {
        return storage;
    }

    public boolean hasBlockSounds() {
        return blockSounds != null;
    }

    public BlockSounds getBlockSounds() {
        return blockSounds;
    }

    public boolean isJukebox() {
        return jukebox != null;
    }

    public JukeboxBlock getJukebox() {
        return jukebox;
    }

    public boolean hasBarriers() {
        return !barriers.isEmpty();
    }

    public List<BlockLocation> getBarriers() {
        return barriers;
    }

    public boolean hasSeat() {
        return hasSeat;
    }

    public float getSeatHeight() {
        return seatHeight;
    }

    public Drop getDrop() {
        return drop;
    }

    public boolean hasEvolution() {
        return evolvingFurniture != null;
    }

    public EvolvingFurniture getEvolution() {
        return evolvingFurniture;
    }

    public void setPlacedItem() {
        if (placedItem == null) {
            placedItem = OraxenItems.getItemById(placedItemId != null ? placedItemId : getItemID()).build();
            ItemMeta meta = placedItem.getItemMeta();
            if (meta != null) meta.setDisplayName("");
            placedItem.setItemMeta(meta);
        }
    }

    public ItemFrame place(Location location) {
        setPlacedItem();
        return place(Rotation.NONE, getYaw(Rotation.NONE), BlockFace.NORTH, location, placedItem);
    }

    public ItemFrame place(Rotation rotation, float yaw, BlockFace facing, Location location) {
        setPlacedItem();
        return place(rotation, yaw, facing, location, placedItem);
    }

    public ItemFrame place(Rotation rotation, float yaw, BlockFace facing, Location location, ItemStack item) {
        if (!location.isWorldLoaded()) return null;
        if (this.notEnoughSpace(yaw, location)) return null;
        assert location.getWorld() != null;
        setPlacedItem();
        assert location.getWorld() != null;
        ItemFrame itemFrame = glowing
                ? location.getWorld().spawn(location, GlowItemFrame.class, (GlowItemFrame frame) ->
                setFrameData(frame, item, rotation, facing))
                : location.getWorld().spawn(location, ItemFrame.class, (ItemFrame frame) ->
                setFrameData(frame, item, rotation, facing));

        if (this.isModelEngine() && Bukkit.getPluginManager().isPluginEnabled("ModelEngine")) {
            spawnModelEngineFurniture(itemFrame, yaw);
        }

        if (hasBarriers())
            setBarrierHitbox(location, yaw, rotation);
        else if (light != -1)
            WrappedLightAPI.createBlockLight(location, light);

        return itemFrame;
    }

    private void setFrameData(ItemFrame frame, ItemStack item, Rotation rotation, BlockFace facing) {
        frame.setVisible(false);
        frame.setFixed(false);
        frame.setPersistent(true);
        frame.setItemDropChance(0);
        if (evolvingFurniture == null) {
            ItemStack clone = item.clone();
            ItemMeta meta = clone.getItemMeta();
            if (meta != null) meta.setDisplayName("");
            clone.setItemMeta(meta);
            frame.setItem(clone, false);
        } else frame.setItem(placedItem, false);
        frame.setRotation(rotation);
        frame.setFacingDirection(facing, true);

        PersistentDataContainer pdc = frame.getPersistentDataContainer();
        pdc.set(FURNITURE_KEY, PersistentDataType.STRING, getItemID());
        if (hasEvolution()) pdc.set(EVOLUTION_KEY, PersistentDataType.INTEGER, 0);
        if (isStorage()) if (getStorage().getStorageType() == StorageMechanic.StorageType.STORAGE) {
            pdc.set(StorageMechanic.STORAGE_KEY, DataType.ITEM_STACK_ARRAY, new ItemStack[]{});
        }

        if (frame.getLocation().getBlock().getRelative(BlockFace.DOWN).getType().isSolid()) {
            FurnitureMechanic mechanic = OraxenFurniture.getFurnitureMechanic(frame);

            // Make sure that if a floor-only furniture is placed on the side of a wall block, it is facing correctly
            if (mechanic.hasLimitedPlacing() && mechanic.limitedPlacing.isFloor() && !mechanic.limitedPlacing.isWall()) {
                frame.setFacingDirection(BlockFace.UP, true);
            }

            // If placed on the side of a block
            if (Set.of(BlockFace.NORTH, BlockFace.WEST, BlockFace.SOUTH, BlockFace.EAST).contains(facing)) {
                frame.setRotation(Rotation.NONE);
            }
        }
    }

    private void setBarrierHitbox(Location location, float yaw, Rotation rotation) {
        for (Location barrierLocation : getLocations(yaw, location.clone(), getBarriers())) {
            Block block = barrierLocation.getBlock();
            PersistentDataContainer data = BlockHelpers.getPDC(block);
            data.set(FURNITURE_KEY, PersistentDataType.STRING, getItemID());
            if (hasSeat()) {
                String entityId = spawnSeat(this, block, hasSeatYaw ? seatYaw : yaw);
                if (entityId != null) data.set(SEAT_KEY, PersistentDataType.STRING, entityId);
            }
            data.set(ROOT_KEY, PersistentDataType.STRING, new BlockLocation(location.clone()).toString());
            data.set(ORIENTATION_KEY, PersistentDataType.FLOAT, yaw);
            data.set(ROTATION_KEY, DataType.asEnum(Rotation.class), rotation);
            block.setType(Material.BARRIER);
            if (light != -1)
                WrappedLightAPI.createBlockLight(barrierLocation, light);
        }
    }

    private void spawnModelEngineFurniture(ItemFrame itemFrame, float yaw) {
        ArmorStand baseEntity = itemFrame.getWorld().spawn(itemFrame.getLocation(), ArmorStand.class, (ArmorStand stand) -> {
            stand.setVisible(false);
            stand.setInvulnerable(true);
            stand.setCustomNameVisible(false);
            stand.setMarker(true);
            stand.setGravity(false);
            stand.setPersistent(true);
            stand.setAI(false);
            stand.setRotation(yaw, 0);
        });
        ModeledEntity modelEntity = ModelEngineAPI.getOrCreateModeledEntity(baseEntity);
        ActiveModel activeModel = ModelEngineAPI.createActiveModel(ModelEngineAPI.getBlueprint(getModelEngineID()));

        modelEntity.addModel(activeModel, false);
        modelEntity.setBaseEntityVisible(false);
        modelEntity.setModelRotationLock(true);

        itemFrame.getPersistentDataContainer().set(MODELENGINE_KEY, DataType.UUID, baseEntity.getUniqueId());
        baseEntity.getPersistentDataContainer().set(MODELENGINE_KEY, DataType.ITEM_STACK, itemFrame.getItem());

        baseEntity.setRotation(yaw, 0);
        itemFrame.setItem(new ItemStack(Material.AIR), false);
    }

    public boolean removeSolid(Block block) {
        PersistentDataContainer pdc = BlockHelpers.getPDC(block);
        Float orientation = pdc.getOrDefault(ORIENTATION_KEY, PersistentDataType.FLOAT, 0f);
        final BlockLocation rootBlock = new BlockLocation(Objects.requireNonNull(pdc.get(ROOT_KEY, PersistentDataType.STRING)));

        return removeSolid(block.getWorld(), rootBlock, orientation);
    }

    public boolean removeSolid(World world, BlockLocation rootBlockLocation, float orientation) {
        Location rootLocation = rootBlockLocation.toLocation(world);

        for (Location location : getLocations(orientation, rootLocation, getBarriers())) {
            if (light != -1)
                WrappedLightAPI.removeBlockLight(location);
            if (hasSeat()) {
                ArmorStand seat = getSeat(location);
                if (seat != null && seat.getPersistentDataContainer().has(SEAT_KEY, PersistentDataType.STRING)) {
                    seat.getPassengers().forEach(seat::removePassenger);
                    seat.remove();
                }
            }
            StorageMechanic storageMechanic = getStorage();
            if (storageMechanic != null && (storageMechanic.isStorage() || storageMechanic.isShulker())) {
                ItemFrame itemFrame = getItemFrame(rootLocation.getBlock());
                if (itemFrame != null) {
                    storageMechanic.dropStorageContent(this, itemFrame);
                }
            }
            location.getBlock().setType(Material.AIR);
            new CustomBlockData(location.getBlock(), OraxenPlugin.get()).clear();
        }

        boolean removed = false;
        for (Entity entity : world.getNearbyEntities(rootLocation, 1, 1, 1)) {
            PersistentDataContainer pdc = entity.getPersistentDataContainer();
            if (entity instanceof ItemFrame frame
                    && entity.getLocation().getBlockX() == rootLocation.getX()
                    && entity.getLocation().getBlockY() == rootLocation.getY()
                    && entity.getLocation().getBlockZ() == rootLocation.getZ()
                    && pdc.has(FURNITURE_KEY, PersistentDataType.STRING)) {
                if (pdc.has(SEAT_KEY, PersistentDataType.STRING)) {
                    String uuid = pdc.get(SEAT_KEY, PersistentDataType.STRING);
                    Entity seat = uuid != null ? Bukkit.getEntity(UUID.fromString(uuid)) : null;
                    if (seat != null) {
                        seat.getPassengers().clear();
                        seat.remove();
                    }
                }
                if (pdc.has(MODELENGINE_KEY, DataType.UUID)) {
                    UUID uuid = pdc.get(MODELENGINE_KEY, DataType.UUID);
                    if (uuid != null) {
                        ArmorStand stand = (ArmorStand) Bukkit.getEntity(uuid);
                        if (stand != null) {
                            stand.remove();
                        }
                    }
                }
                frame.remove();
                if (light != -1)
                    WrappedLightAPI.removeBlockLight(rootLocation);
                rootLocation.getBlock().setType(Material.AIR);
                removed = true;
                break;
            }
        }

        return removed;
    }

    public void removeAirFurniture(Entity frame) {
        PersistentDataContainer framePDC = frame.getPersistentDataContainer();
        if (framePDC.has(SEAT_KEY, PersistentDataType.STRING)) {
            String uuid = framePDC.get(SEAT_KEY, PersistentDataType.STRING);
            Entity stand = uuid != null ? Bukkit.getEntity(UUID.fromString(uuid)) : null;
            if (stand != null) {
                stand.getPassengers().forEach(stand::removePassenger);
                stand.remove();
            }
        }
        if (framePDC.has(MODELENGINE_KEY, DataType.UUID)) {
            UUID uuid = framePDC.get(MODELENGINE_KEY, DataType.UUID);
            if (uuid != null) {
                ArmorStand stand = (ArmorStand) Bukkit.getEntity(uuid);
                if (stand != null) {
                    stand.remove();
                }
            }
        }
        Location location = frame.getLocation().getBlock().getLocation();
        if (light != -1) {
            WrappedLightAPI.removeBlockLight(location);
        }
        frame.remove();
    }

    /**
     * Scheduled for removal in a future update. As of 1.147.0 API has been entirely redone.<br>
     * See {@link io.th0rgal.oraxen.api.OraxenFurniture#remove(Location, Player)} for the new method
     */
    @Deprecated(forRemoval = true, since = "1.147.0")
    public void remove(ItemFrame frame) {
        if (this.hasBarriers())
            this.removeSolid(frame.getWorld(), new BlockLocation(frame.getLocation()),
                    this.getYaw(frame.getRotation()));
        else
            this.removeAirFurniture(frame);
    }

    public List<Location> getLocations(float rotation, Location center, List<BlockLocation> relativeCoordinates) {
        List<Location> output = new ArrayList<>();
        for (BlockLocation modifier : relativeCoordinates)
            output.add(modifier.groundRotate(rotation).add(center));
        return output;
    }

    public boolean notEnoughSpace(float yaw, Location rootLocation) {
        if (!hasBarriers()) return false;
        return !getLocations(yaw, rootLocation, getBarriers()).stream().map(l -> l.getBlock().getType())
                .allMatch(BlockHelpers.REPLACEABLE_BLOCKS::contains);
    }

    public float getYaw(Rotation rotation) {
        return (Arrays.asList(Rotation.values()).indexOf(rotation) * 360f) / 8f;
    }

    public boolean hasClickActions() {
        return !clickActions.isEmpty();
    }

    public void runClickActions(final Player player) {
        for (final ClickAction action : clickActions) {
            if (action.canRun(player)) {
                action.performActions(player);
            }
        }
    }

    private String spawnSeat(FurnitureMechanic mechanic, Block target, float yaw) {
        if (mechanic.hasSeat()) {
            final ArmorStand seat = target.getWorld().spawn(target.getLocation()
                    .add(0.5, mechanic.getSeatHeight() - 1, 0.5), ArmorStand.class, (ArmorStand stand) -> {
                stand.setVisible(false);
                stand.setRotation(yaw, 0);
                stand.setInvulnerable(true);
                stand.setPersistent(true);
                stand.setAI(false);
                stand.setCollidable(false);
                stand.setGravity(false);
                stand.setSilent(true);
                stand.setCustomNameVisible(false);
                stand.setCanPickupItems(false);
                //TODO Maybe marker works here? Was removed for rotation issues but should be fixed
                stand.addEquipmentLock(EquipmentSlot.HEAD, ArmorStand.LockType.ADDING_OR_CHANGING);
                stand.addEquipmentLock(EquipmentSlot.HAND, ArmorStand.LockType.ADDING_OR_CHANGING);
                stand.addEquipmentLock(EquipmentSlot.OFF_HAND, ArmorStand.LockType.ADDING_OR_CHANGING);
                stand.addEquipmentLock(EquipmentSlot.CHEST, ArmorStand.LockType.ADDING_OR_CHANGING);
                stand.addEquipmentLock(EquipmentSlot.LEGS, ArmorStand.LockType.ADDING_OR_CHANGING);
                stand.addEquipmentLock(EquipmentSlot.FEET, ArmorStand.LockType.ADDING_OR_CHANGING);
                stand.getPersistentDataContainer().set(FURNITURE_KEY, PersistentDataType.STRING, mechanic.getItemID());
                stand.getPersistentDataContainer().set(SEAT_KEY, PersistentDataType.STRING, stand.getUniqueId().toString());
            });
            return seat.getUniqueId().toString();
        }
        return null;
    }


    /**
     * Scheduled for removal in a future update. As of 1.147.0 API has been entirely redone.<br>
     * This method now only takes the block/barrier, see {@link #getItemFrame(Block)} for the new method
     */
    @Deprecated(forRemoval = true, since = "1.147.0")
    public ItemFrame getItemFrame(Block block, BlockLocation blockLocation, Float orientation) {
        if (hasBarriers()) {
            for (Location sideLocation : getLocations(orientation, blockLocation.toLocation(block.getWorld()), getBarriers())) {
                for (Entity entity : block.getWorld().getNearbyEntities(sideLocation, 1, 1, 1))
                    if (entity instanceof ItemFrame frame
                            && entity.getLocation().getBlockX() == sideLocation.getBlockX()
                            && entity.getLocation().getBlockY() == sideLocation.getBlockY()
                            && entity.getLocation().getBlockZ() == sideLocation.getBlockZ()
                            && entity.getPersistentDataContainer().has(FURNITURE_KEY, PersistentDataType.STRING))
                        return frame;
            }
        }
        return null;
    }

    public ItemFrame getItemFrame(Block block) {
        PersistentDataContainer pdc = BlockHelpers.getPDC(block);
        if (pdc.isEmpty()) return null;
        final BlockLocation blockLoc = new BlockLocation(Objects.requireNonNull(pdc.get(ROOT_KEY, PersistentDataType.STRING)));
        Location originLoc = blockLoc.toLocation(block.getWorld());

        if (hasBarriers()) for (Entity entity : block.getWorld().getNearbyEntities(originLoc, 0.5, 0.5, 0.5)) {
            if (entity instanceof ItemFrame frame
                    && entity.getLocation().getBlockX() == originLoc.getBlockX()
                    && entity.getLocation().getBlockY() == originLoc.getBlockY()
                    && entity.getLocation().getBlockZ() == originLoc.getBlockZ()
                    && entity.getPersistentDataContainer().has(FURNITURE_KEY, PersistentDataType.STRING))
                return frame;
        }
        return null;
    }
}
