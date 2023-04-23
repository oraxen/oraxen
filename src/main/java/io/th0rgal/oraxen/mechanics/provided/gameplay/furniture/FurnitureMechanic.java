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
import io.th0rgal.oraxen.utils.logs.Logs;
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
import org.bukkit.util.Transformation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

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
    private final String modelEngineID;
    private final String placedItemId;
    private ItemStack placedItem;
    private float seatHeight;
    private float seatYaw;
    private final List<ClickAction> clickActions;
    private FurnitureType furnitureType;
    private final DisplayEntityProperties displayEntityProperties;
    private final FurnitureHitbox hitbox;

    public record FurnitureHitbox(float width, float height) {
    }

    public enum FurnitureType {
        ITEM_FRAME, GLOW_ITEM_FRAME, DISPLAY_ENTITY;//, ARMOR_STAND;

        public static List<Class<? extends Entity>> furnitureEntityClasses() {
            List<Class<? extends Entity>> list = new ArrayList<>(List.of(ItemFrame.class, GlowItemFrame.class, ArmorStand.class));
            if (OraxenPlugin.supportsDisplayEntities) list.add(ItemDisplay.class);
            return list;
        }
    }

    @SuppressWarnings("unchecked")
    public FurnitureMechanic(MechanicFactory mechanicFactory, ConfigurationSection section) {
        super(mechanicFactory, section, itemBuilder -> itemBuilder.setCustomTag(FURNITURE_KEY,
                PersistentDataType.BYTE, (byte) 1));

        placedItemId = section.getString("item", null);

        modelEngineID = section.getString("modelengine_id", null);

        try {
            furnitureType = FurnitureType.valueOf(section.getString("type", FurnitureType.ITEM_FRAME.name()));
            if (furnitureType == FurnitureType.DISPLAY_ENTITY && !OraxenPlugin.supportsDisplayEntities) {
                Logs.logError("Use of Display Entity on unsupported server version.");
                Logs.logError("This EntityType is only supported on 1.19.4 and above.");
                Logs.logError("Setting type to ITEM_FRAME for <i>" + getItemID() + "</i>.");
                furnitureType = FurnitureType.ITEM_FRAME;
            }
        } catch (IllegalArgumentException e) {
            Logs.logError("Use of illegal EntityType in " + getItemID() + " furniture.");
            Logs.logError("Allowed ones are: " + Arrays.stream(FurnitureType.values()).toList().stream().map(Enum::name));
            Logs.logError("Setting type to ITEM_FRAME.");
            furnitureType = FurnitureType.ITEM_FRAME;
        }
        ConfigurationSection displayEntitySection = section.getConfigurationSection("display_entity_properties");
        displayEntityProperties = OraxenPlugin.supportsDisplayEntities
                ? displayEntitySection != null
                ? new DisplayEntityProperties(displayEntitySection) : new DisplayEntityProperties()
                : null;

        barriers = new ArrayList<>();
        if (CompatibilitiesManager.hasPlugin("ProtocolLib")) {
            if (section.getBoolean("barrier", false))
                barriers.add(new BlockLocation(0, 0, 0));
            if (section.isList("barriers"))
                for (Object barrierObject : section.getList("barriers", new ArrayList<>()))
                    barriers.add(new BlockLocation((Map<String, Object>) barrierObject));
        }

        ConfigurationSection hitboxSection = section.getConfigurationSection("hitbox");
        hitbox = !hasBarriers() && hitboxSection != null
                ? new FurnitureHitbox((float) hitboxSection.getDouble("width", 1.0), (float) hitboxSection.getDouble("height", 1.0))
                : null;

        ConfigurationSection seatSection = section.getConfigurationSection("seat");
        if (seatSection != null) {
            hasSeat = true;
            seatHeight = (float) seatSection.getDouble("height");
            hasSeatYaw = seatSection.contains("yaw");
            if (hasSeatYaw) seatYaw = (float) seatSection.getDouble("yaw");
        } else hasSeat = false;

        ConfigurationSection evoSection = section.getConfigurationSection("evolution");
        if (evoSection != null) {
            evolvingFurniture = new EvolvingFurniture(getItemID(), evoSection);
            ((FurnitureFactory) getFactory()).registerEvolution();
        } else evolvingFurniture = null;

        light = section.getInt("light", -1);

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
        Location seatLoc = BlockHelpers.toCenterBlockLocation(location);
        if (location.getWorld() == null) return null;
        for (Entity entity : location.getWorld().getNearbyEntities(seatLoc, 0.1, 4, 0.1)) {
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

    public boolean hasHitbox() {
        return !hasBarriers() && hitbox != null;
    }

    public FurnitureHitbox getHitbox() {
        return hitbox;
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

    @Deprecated(forRemoval = true, since = "1.154.0")
    public ItemFrame place(Rotation rotation, float yaw, BlockFace facing, Location location) {
        setPlacedItem();
        return place(rotation, yaw, facing, location, placedItem);
    }

    /**
     * @param rotation
     * @param yaw
     * @param facing
     * @param location
     * @param item
     * @return
     */
    @Deprecated(forRemoval = true, since = "1.154.0")
    public ItemFrame place(Rotation rotation, float yaw, BlockFace facing, Location location, ItemStack item) {
        if (!location.isWorldLoaded()) return null;
        if (this.notEnoughSpace(yaw, location)) return null;
        assert location.getWorld() != null;
        setPlacedItem();
        assert location.getWorld() != null;
        ItemFrame itemFrame = getFurnitureEntityType() == EntityType.GLOW_ITEM_FRAME
                ? location.getWorld().spawn(location, GlowItemFrame.class, (GlowItemFrame frame) ->
                setFrameData(frame, item, rotation, facing))
                : location.getWorld().spawn(location, ItemFrame.class, (ItemFrame frame) ->
                setFrameData(frame, item, rotation, facing));

        if (this.isModelEngine() && Bukkit.getPluginManager().isPluginEnabled("ModelEngine")) {
            spawnModelEngineFurniture(itemFrame, yaw);
        }

        if (hasBarriers())
            setBarrierHitbox(location, yaw, rotation, true);
        else if (light != -1)
            WrappedLightAPI.createBlockLight(location, light);

        return itemFrame;
    }

    public Entity place(Location location) {
        setPlacedItem();
        return place(location, placedItem, Rotation.NONE, rotationToYaw(Rotation.NONE), BlockFace.NORTH);
    }

    public Entity place(Location location, float yaw, Rotation rotation, BlockFace facing) {
        setPlacedItem();
        return place(location, placedItem, rotation, yaw, facing);
    }

    public Entity place(Location location, ItemStack originalItem, Rotation rotation, float yaw, BlockFace facing) {
        if (!location.isWorldLoaded()) return null;
        if (this.notEnoughSpace(yaw, location)) return null;
        assert location.getWorld() != null;
        setPlacedItem();
        assert location.getWorld() != null;

        Class<? extends Entity> entityClass = getFurnitureEntityType().getEntityClass();
        if (entityClass == null) entityClass = ItemFrame.class;

        ItemStack item;
        if (evolvingFurniture == null) {
            ItemStack clone = originalItem.clone();
            ItemMeta meta = clone.getItemMeta();
            if (meta != null) meta.setDisplayName("");
            clone.setItemMeta(meta);
            item = clone;
        } else item = placedItem;

        Entity baseEntity = location.getWorld().spawn(BlockHelpers.toCenterBlockLocation(location), entityClass, (entity) ->
                setEntityData(entity, yaw, item, rotation, facing));

        if (this.isModelEngine() && Bukkit.getPluginManager().isPluginEnabled("ModelEngine")) {
            spawnModelEngineFurniture(baseEntity, yaw);
        }

        return baseEntity;
    }

    private void setEntityData(Entity entity, float yaw, ItemStack item, Rotation rotation, BlockFace facing) {
        setBaseFurnitureData(entity);
        if (entity instanceof ItemFrame frame) {
            setFrameData(frame, item, facing, rotation);
            Location location = entity.getLocation();

            if (hasBarriers()) setBarrierHitbox(location, yaw, rotation, true);
            else {
                float width = hasHitbox() ? hitbox.width : 1f;
                float height = hasHitbox() ? hitbox.height : 1f;
                spawnInteractionEntity(entity, location, width, height, true);

                Block block = location.getBlock();
                if (hasSeat()) {
                    UUID entityId = spawnSeat(this, block, hasSeatYaw ? seatYaw : location.getYaw());
                    if (entityId != null) BlockHelpers.getPDC(block).set(SEAT_KEY, DataType.UUID, entityId);
                }
                if (light != -1) {
                    WrappedLightAPI.createBlockLight(location, light);
                }
            }
        } else if (entity instanceof ItemDisplay itemDisplay) {
            setItemDisplayData(itemDisplay, item, rotation, displayEntityProperties);
            Location location = itemDisplay.getLocation();
            float width = hasHitbox() ? hitbox.width : displayEntityProperties.getWidth();
            float height = hasHitbox() ? hitbox.height : displayEntityProperties.getHeight();
            Interaction interaction = spawnInteractionEntity(itemDisplay, location, width, height, displayEntityProperties.isInteractable());

            if (hasBarriers()) setBarrierHitbox(location, yaw, rotation, false);
            else if (hasSeat()) {
                UUID entityId = spawnSeat(this, location.getBlock(), hasSeatYaw ? seatYaw : location.getYaw());
                if (entityId != null && interaction != null)
                    interaction.getPersistentDataContainer().set(SEAT_KEY, DataType.UUID, entityId);
            }
        }
    }

    private Interaction spawnInteractionEntity(Entity entity, Location location, float width, float height, boolean responsive) {
        if (!OraxenPlugin.supportsDisplayEntities) return null;
        return entity.getWorld().spawn(BlockHelpers.toCenterBlockLocation(location), Interaction.class, (Interaction interaction) -> {
            interaction.setInteractionWidth(width);
            interaction.setInteractionHeight(height);
            interaction.setResponsive(responsive);
            interaction.getPersistentDataContainer().set(FURNITURE_KEY, DataType.STRING, getItemID());
            interaction.getPersistentDataContainer().set(ROOT_KEY, DataType.LOCATION, location);
        });
    }

    private void setBaseFurnitureData(Entity entity) {
        entity.setPersistent(true);
        PersistentDataContainer pdc = entity.getPersistentDataContainer();
        pdc.set(FURNITURE_KEY, PersistentDataType.STRING, getItemID());
        if (hasEvolution()) pdc.set(EVOLUTION_KEY, PersistentDataType.INTEGER, 0);
        if (isStorage() && getStorage().getStorageType() == StorageMechanic.StorageType.STORAGE) {
            pdc.set(StorageMechanic.STORAGE_KEY, DataType.ITEM_STACK_ARRAY, new ItemStack[]{});
        }
    }

    private void setItemDisplayData(ItemDisplay itemDisplay, ItemStack item, Rotation rotation, DisplayEntityProperties properties) {
        itemDisplay.setItemDisplayTransform(properties.getDisplayTransform());
        if (properties.hasSpecifiedViewRange()) itemDisplay.setViewRange(properties.getViewRange());
        if (properties.hasInterpolationDuration())
            itemDisplay.setInterpolationDuration(properties.getInterpolationDuration());
        if (properties.hasInterpolationDelay()) itemDisplay.setInterpolationDelay(properties.getInterpolationDelay());
        if (properties.hasTrackingRotation()) itemDisplay.setBillboard(properties.getTrackingRotation());
        if (properties.hasShadowRadius()) itemDisplay.setShadowRadius(properties.getShadowRadius());
        if (properties.hasShadowStrength()) itemDisplay.setShadowStrength(properties.getShadowStrength());
        //if (properties.hasGlowColor()) itemDisplay.setGlowColorOverride(properties.getGlowColor());
        if (properties.hasBrightness()) itemDisplay.setBrightness(displayEntityProperties.getBrightness());
        else if (light != -1) itemDisplay.setBrightness(new Display.Brightness(light, 0));

        itemDisplay.setDisplayWidth(properties.getWidth());
        itemDisplay.setDisplayHeight(properties.getHeight());
        itemDisplay.setItemStack(item);

        // Set scale to .5 if FIXED aka ItemFrame to fix size. Also flip it 90 degrees on pitch
        boolean isFixed = properties.getDisplayTransform().equals(ItemDisplay.ItemDisplayTransform.FIXED);
        Transformation transform = itemDisplay.getTransformation();
        if (properties.hasScale()) {
            transform.getScale().set(properties.getScale());
        } else if (isFixed) transform.getScale().set(new Vector3f(0.5f, 0.5f, 0.5f));

        itemDisplay.setTransformation(transform);
        itemDisplay.setRotation(rotationToYaw(rotation.rotateClockwise().rotateClockwise().rotateClockwise().rotateClockwise()), isFixed ? 90f : 0f);
        if (displayEntityProperties.getDisplayTransform() == ItemDisplay.ItemDisplayTransform.NONE)
            itemDisplay.teleport(BlockHelpers.toCenterLocation(itemDisplay.getLocation()));
    }

    private void setFrameData(ItemFrame frame, ItemStack item, BlockFace facing, Rotation rotation) {
        frame.setVisible(false);
        frame.setItemDropChance(0);
        frame.setFacingDirection(facing, true);
        frame.setItem(item);
        frame.setRotation(rotation);

        if (hasLimitedPlacing()) {
            if (limitedPlacing.isFloor() && !limitedPlacing.isWall() && frame.getLocation().getBlock().getRelative(BlockFace.DOWN).getType().isSolid()) {
                frame.setFacingDirection(BlockFace.UP, true);
            } else if (limitedPlacing.isWall()) {
                frame.setRotation(Rotation.NONE);
            }
        }
    }

    @Deprecated(forRemoval = true, since = "1.154.0")
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
            if (mechanic != null && mechanic.hasLimitedPlacing() && mechanic.limitedPlacing.isFloor() && !mechanic.limitedPlacing.isWall()) {
                frame.setFacingDirection(BlockFace.UP, true);
            }

            // If placed on the side of a block
            if (Set.of(BlockFace.NORTH, BlockFace.WEST, BlockFace.SOUTH, BlockFace.EAST).contains(facing)) {
                frame.setRotation(Rotation.NONE);
            }
        }
    }

    private void setBarrierHitbox(Location location, float yaw, Rotation rotation, boolean handleLight) {
        for (Location barrierLocation : getLocations(yaw, BlockHelpers.toCenterBlockLocation(location), getBarriers())) {
            Block block = barrierLocation.getBlock();
            PersistentDataContainer data = BlockHelpers.getPDC(block);
            data.set(FURNITURE_KEY, PersistentDataType.STRING, getItemID());
            if (hasSeat()) {
                UUID entityId = spawnSeat(this, block, hasSeatYaw ? seatYaw : yaw);
                if (entityId != null) data.set(SEAT_KEY, DataType.UUID, entityId);
            }
            data.set(ROOT_KEY, PersistentDataType.STRING, new BlockLocation(location.clone()).toString());
            data.set(ORIENTATION_KEY, PersistentDataType.FLOAT, yaw);
            data.set(ROTATION_KEY, DataType.asEnum(Rotation.class), rotation);
            block.setType(Material.BARRIER);
            if (handleLight && light != -1)
                WrappedLightAPI.createBlockLight(barrierLocation, light);
        }
    }

    private void spawnModelEngineFurniture(Entity entity, float yaw) {
        ArmorStand megEntity = entity.getWorld().spawn(entity.getLocation(), ArmorStand.class, (ArmorStand stand) -> {
            stand.setVisible(false);
            stand.setInvulnerable(true);
            stand.setCustomNameVisible(false);
            stand.setMarker(true);
            stand.setGravity(false);
            stand.setPersistent(true);
            stand.setAI(false);
            stand.setRotation(yaw, 0);
        });
        ModeledEntity modelEntity = ModelEngineAPI.getOrCreateModeledEntity(entity);
        ActiveModel activeModel = ModelEngineAPI.createActiveModel(ModelEngineAPI.getBlueprint(getModelEngineID()));

        modelEntity.addModel(activeModel, false);
        modelEntity.setBaseEntityVisible(false);
        modelEntity.setModelRotationLock(true);

        entity.getPersistentDataContainer().set(MODELENGINE_KEY, DataType.UUID, megEntity.getUniqueId());
        megEntity.getPersistentDataContainer().set(MODELENGINE_KEY, DataType.ITEM_STACK, getFurnitureItem(entity));

        megEntity.setRotation(yaw, 0);
        if (entity instanceof ItemDisplay itemDisplay)
            itemDisplay.setItemStack(new ItemStack(Material.AIR));
        else if (entity instanceof ItemFrame itemFrame)
            itemFrame.setItem(new ItemStack(Material.AIR), false);
    }

    @Nullable
    public static ItemStack getFurnitureItem(Entity entity) {
        return switch (entity.getType()) {
            case ITEM_FRAME, GLOW_ITEM_FRAME -> ((ItemFrame) entity).getItem();
            case ARMOR_STAND -> ((ArmorStand) entity).getEquipment().getHelmet();
            case ITEM_DISPLAY -> OraxenPlugin.supportsDisplayEntities ? ((ItemDisplay) entity).getItemStack() : null;
            default -> null;
        };
    }

    public boolean removeSolid(Block block) {
        PersistentDataContainer pdc = BlockHelpers.getPDC(block);
        Float orientation = pdc.getOrDefault(ORIENTATION_KEY, PersistentDataType.FLOAT, 0f);
        final BlockLocation rootBlock = new BlockLocation(Objects.requireNonNull(pdc.get(ROOT_KEY, PersistentDataType.STRING)));

        return removeSolid(block.getWorld(), rootBlock, orientation);
    }

    //TODO Should be able to simplify this aton
    public boolean removeSolid(World world, BlockLocation rootBlockLocation, float orientation) {
        Location rootLocation = rootBlockLocation.toLocation(world);

        for (Location location : getLocations(orientation, rootLocation, getBarriers())) {
            if (light != -1)
                WrappedLightAPI.removeBlockLight(location);
            if (hasSeat) {
                ArmorStand seat = getSeat(location);
                if (seat != null && seat.getPersistentDataContainer().has(SEAT_KEY, PersistentDataType.STRING)) {
                    seat.getPassengers().forEach(seat::removePassenger);
                    seat.remove();
                }
            }
            Entity baseEntity = getBaseEntity(rootLocation.getBlock());
            StorageMechanic storageMechanic = getStorage();

            if (baseEntity != null) {
                if (storageMechanic != null && (storageMechanic.isStorage() || storageMechanic.isShulker()))
                    storageMechanic.dropStorageContent(this, baseEntity);
                removeSubEntitiesOfFurniture(baseEntity);
            }
            location.getBlock().setType(Material.AIR);
            new CustomBlockData(location.getBlock(), OraxenPlugin.get()).clear();
        }

        boolean removed = false;
        for (Entity entity : world.getNearbyEntities(rootLocation, 1, 1, 1)) {
            PersistentDataContainer pdc = entity.getPersistentDataContainer();
            if (entity.getType() == getFurnitureEntityType()
                    && entity.getLocation().getBlockX() == rootLocation.getX()
                    && entity.getLocation().getBlockY() == rootLocation.getY()
                    && entity.getLocation().getBlockZ() == rootLocation.getZ()
                    && pdc.has(FURNITURE_KEY, PersistentDataType.STRING)) {
                removeSubEntitiesOfFurniture(entity);
                entity.remove();
                if (light != -1)
                    WrappedLightAPI.removeBlockLight(rootLocation);
                rootLocation.getBlock().setType(Material.AIR);
                removed = true;
                break;
            }
        }

        return removed;
    }

    public void removeAirFurniture(Entity baseEntity) {
        if (light != -1)
            WrappedLightAPI.removeBlockLight(baseEntity.getLocation().getBlock().getLocation());

        removeSubEntitiesOfFurniture(baseEntity);
        baseEntity.remove();
    }

    public void removeSubEntitiesOfFurniture(Entity baseEntity) {
        PersistentDataContainer entityPDC = baseEntity.getPersistentDataContainer();
        if (hasSeat) {
            Entity stand = getSeat(baseEntity.getLocation());
            if (stand != null) {
                stand.getPassengers().forEach(stand::removePassenger);
                stand.remove();
            }
        }
        if (isModelEngine()) {
            UUID uuid = entityPDC.get(MODELENGINE_KEY, DataType.UUID);
            if (uuid != null) {
                ArmorStand stand = (ArmorStand) Bukkit.getEntity(uuid);
                if (stand != null) {
                    stand.getPassengers().forEach(stand::removePassenger);
                    stand.remove();
                }
            }
        }

        if (OraxenPlugin.supportsDisplayEntities) {
            for (Entity entity : baseEntity.getNearbyEntities(0.1, 0.1, 0.1)) {
                if (!(entity instanceof Interaction interaction)) continue;
                PersistentDataContainer pdc = interaction.getPersistentDataContainer();
                if (pdc.has(FURNITURE_KEY, DataType.STRING) && pdc.getOrDefault(FURNITURE_KEY, DataType.STRING, "").equals(getItemID())) {
                    if (pdc.has(ROOT_KEY, DataType.LOCATION) && Objects.equals(pdc.get(ROOT_KEY, DataType.LOCATION), baseEntity.getLocation()))
                        interaction.remove();
                }
            }
        }
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

    public static float getFurnitureYaw(Entity entity) {
        return entity.getLocation().getYaw();
    }

    public static float rotationToYaw(Rotation rotation) {
        return (Arrays.asList(Rotation.values()).indexOf(rotation) * 360f) / 8f;
    }

    public static Rotation yawToRotation(float yaw) {
        return Rotation.values()[Math.round(yaw / 45f) & 0x7];
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

    @Nullable
    private UUID spawnSeat(FurnitureMechanic mechanic, Block target, float yaw) {
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
            return seat.getUniqueId();
        }
        return null;
    }

    @Deprecated(forRemoval = true, since = "1.154.0")
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

    @Nullable
    public Entity getBaseEntity(Block block) {
        PersistentDataContainer pdc = BlockHelpers.getPDC(block);
        if (pdc.isEmpty()) return null;
        final BlockLocation blockLoc = new BlockLocation(Objects.requireNonNull(pdc.get(ROOT_KEY, PersistentDataType.STRING)));
        Location originLoc = blockLoc.toLocation(block.getWorld());

        if (hasBarriers()) for (Entity entity : block.getWorld().getNearbyEntities(originLoc, 1, 1, 1)) {
            if (entity.getType() == getFurnitureEntityType()
                    && entity.getLocation().getBlockX() == originLoc.getBlockX()
                    && entity.getLocation().getBlockY() == originLoc.getBlockY()
                    && entity.getLocation().getBlockZ() == originLoc.getBlockZ()
                    && entity.getPersistentDataContainer().has(FURNITURE_KEY, PersistentDataType.STRING))
                return entity;
        }
        return null;
    }

    @Nullable
    public Entity getBaseEntity(Entity entity) {
        // If the entity is the same type as the base entity, return it
        // Since ItemDisplay entities have no hitbox it will only be for ITEM_FRAME based ones
        if (getFurnitureEntityType() == entity.getType()) return entity;

        PersistentDataContainer pdc = entity.getPersistentDataContainer();
        Location location = pdc.get(ROOT_KEY, DataType.LOCATION);
        if (location == null || !location.isWorldLoaded()) return null;
        assert location.getWorld() != null;
        for (Entity baseEntity : location.getWorld().getNearbyEntities(location, 0.1, 0.1, 0.1)) {
            if (baseEntity.getType() != getFurnitureEntityType()) continue;
            if (!OraxenFurniture.isFurniture(baseEntity)) continue;
            return baseEntity;
        }
        return null;
    }

    @Nullable
    public Interaction getInteractionEntity(@NotNull Entity baseEntity) {
        if (OraxenPlugin.supportsDisplayEntities) {
            for (Entity entity : baseEntity.getNearbyEntities(0.1, 0.1, 0.1)) {
                if (!(entity instanceof Interaction interaction)) continue;
                PersistentDataContainer pdc = interaction.getPersistentDataContainer();
                if (pdc.has(FURNITURE_KEY, DataType.STRING) && pdc.getOrDefault(FURNITURE_KEY, DataType.STRING, "").equals(getItemID())) {
                    if (pdc.has(ROOT_KEY, DataType.LOCATION) && Objects.equals(pdc.get(ROOT_KEY, DataType.LOCATION), baseEntity.getLocation()))
                        return interaction;
                }
            }
        }
        return null;
    }

    public FurnitureType getFurnitureType() {
        return furnitureType;
    }

    public EntityType getFurnitureEntityType() {

        return switch (furnitureType) {
            case ITEM_FRAME -> EntityType.ITEM_FRAME;
            case GLOW_ITEM_FRAME -> EntityType.GLOW_ITEM_FRAME;
            case DISPLAY_ENTITY -> OraxenPlugin.supportsDisplayEntities ? EntityType.ITEM_DISPLAY : EntityType.ITEM_FRAME;
            //case ARMOR_STAND -> EntityType.ARMOR_STAND;
        };
    }

    public Class<? extends Entity> getFurnitureEntityClass() {
        return switch (furnitureType) {
            case ITEM_FRAME -> ItemFrame.class;
            case GLOW_ITEM_FRAME -> GlowItemFrame.class;
            case DISPLAY_ENTITY -> OraxenPlugin.supportsDisplayEntities ? ItemDisplay.class : ItemFrame.class;
            //case ARMOR_STAND -> ArmorStand.class;
        };
    }

    public DisplayEntityProperties getDisplayEntityProperties() {
        return displayEntityProperties;
    }
}
