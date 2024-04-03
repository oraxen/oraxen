package io.th0rgal.oraxen.mechanics.provided.gameplay.furniture;

import com.comphenix.protocol.wrappers.BlockPosition;
import com.jeff_media.morepersistentdatatypes.DataType;
import com.ticxo.modelengine.api.ModelEngineAPI;
import com.ticxo.modelengine.api.model.ActiveModel;
import com.ticxo.modelengine.api.model.ModeledEntity;
import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.api.OraxenItems;
import io.th0rgal.oraxen.compatibilities.provided.blocklocker.BlockLockerMechanic;
import io.th0rgal.oraxen.mechanics.Mechanic;
import io.th0rgal.oraxen.mechanics.MechanicFactory;
import io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.evolution.EvolvingFurniture;
import io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.hitbox.FurnitureHitbox;
import io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.jukebox.JukeboxBlock;
import io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.seats.FurnitureSeat;
import io.th0rgal.oraxen.mechanics.provided.gameplay.light.LightMechanic;
import io.th0rgal.oraxen.mechanics.provided.gameplay.limitedplacing.LimitedPlacing;
import io.th0rgal.oraxen.mechanics.provided.gameplay.storage.StorageMechanic;
import io.th0rgal.oraxen.utils.*;
import io.th0rgal.oraxen.utils.actions.ClickAction;
import io.th0rgal.oraxen.utils.blocksounds.BlockSounds;
import io.th0rgal.oraxen.utils.drops.Drop;
import io.th0rgal.oraxen.utils.logs.Logs;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Rotation;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Transformation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class FurnitureMechanic extends Mechanic {

    public static final NamespacedKey FURNITURE_KEY = new NamespacedKey(OraxenPlugin.get(), "furniture");
    public static final NamespacedKey MODELENGINE_KEY = new NamespacedKey(OraxenPlugin.get(), "modelengine");
    public static final NamespacedKey EVOLUTION_KEY = new NamespacedKey(OraxenPlugin.get(), "evolution");

    private final int hardness;
    private final LimitedPlacing limitedPlacing;
    private final StorageMechanic storage;
    private final BlockSounds blockSounds;
    private final JukeboxBlock jukebox;
    public final boolean farmlandRequired;
    private final Drop drop;
    private final EvolvingFurniture evolvingFurniture;
    private final LightMechanic light;
    private final String modelEngineID;
    private final String placedItemId;
    private ItemStack placedItem;
    private final List<FurnitureSeat> seats = new ArrayList<>();
    private final List<ClickAction> clickActions;
    private FurnitureType furnitureType;
    private final DisplayEntityProperties displayEntityProperties;
    private final boolean isRotatable;
    private final BlockLockerMechanic blockLocker;
    private final RestrictedRotation restrictedRotation;
    @NotNull private final FurnitureHitbox hitbox;

    public enum RestrictedRotation {
        NONE, STRICT, VERY_STRICT;

        public static RestrictedRotation fromString(String string) {
            try {
                return RestrictedRotation.valueOf(string);
            } catch (IllegalArgumentException e) {
                Logs.logError("Invalid restricted rotation: " + string);
                Logs.logError("Allowed ones are: " + Arrays.toString(RestrictedRotation.values()));
                Logs.logWarning("Setting to STRICT");
                return STRICT;
            }
        }
    }

    public enum FurnitureType {
        ITEM_FRAME, GLOW_ITEM_FRAME, DISPLAY_ENTITY;//, ARMOR_STAND;

        public static List<Class<? extends Entity>> furnitureEntityClasses() {
            List<Class<? extends Entity>> list = new ArrayList<>(List.of(ItemFrame.class, GlowItemFrame.class, ArmorStand.class));
            if (OraxenPlugin.supportsDisplayEntities) list.add(ItemDisplay.class);
            return list;
        }

        public static FurnitureType getType(String type) {
            try {
                return FurnitureType.valueOf(type);
            } catch (IllegalArgumentException e) {
                Logs.logError("Invalid furniture type: " + type + ", set in mechanics.yml.");
                Logs.logWarning("Using default " + (OraxenPlugin.supportsDisplayEntities ? "DISPLAY_ENTITY" : "ITEM_FRAME"), true);
                return OraxenPlugin.supportsDisplayEntities ? DISPLAY_ENTITY : ITEM_FRAME;
            }
        }
    }

    @SuppressWarnings("unchecked")
    public FurnitureMechanic(MechanicFactory mechanicFactory, ConfigurationSection section) {
        super(mechanicFactory, section, itemBuilder -> itemBuilder.setCustomTag(FURNITURE_KEY, PersistentDataType.BYTE, (byte) 1));

        hardness = section.getInt("hardness", 1);
        placedItemId = section.getString("item", null);
        modelEngineID = section.getString("modelengine_id", null);
        farmlandRequired = section.getBoolean("farmland_required", false);
        light = new LightMechanic(section);
        restrictedRotation = RestrictedRotation.fromString(section.getString("restricted_rotation", "STRICT"));

        try {
            String defaultEntityType;
            if (OraxenPlugin.supportsDisplayEntities)
                defaultEntityType = Objects.requireNonNullElse(FurnitureFactory.defaultFurnitureType, FurnitureType.DISPLAY_ENTITY).name();
            else defaultEntityType = FurnitureType.ITEM_FRAME.name();
            furnitureType = FurnitureType.valueOf(section.getString("type", defaultEntityType));
            if (furnitureType == FurnitureType.DISPLAY_ENTITY && !OraxenPlugin.supportsDisplayEntities) {
                Logs.logError("Use of Display Entity on unsupported server version.");
                Logs.logError("This EntityType is only supported on 1.19.4 and above.");
                Logs.logWarning("Setting type to ITEM_FRAME for furniture: <i><gold>" + getItemID());
                furnitureType = FurnitureType.ITEM_FRAME;
            }
        } catch (IllegalArgumentException e) {
            Logs.logError("Use of illegal EntityType in furniture: <gold>" + getItemID());
            Logs.logWarning("Allowed ones are: <gold>" + Arrays.stream(FurnitureType.values()).map(Enum::name).toList());
            Logs.logWarning("Setting type to ITEM_FRAME for furniture: <gold>" + getItemID());
            furnitureType = FurnitureType.ITEM_FRAME;
        }

        section.set("type", furnitureType.name());

        ConfigurationSection displayProperties = section.getConfigurationSection("display_entity_properties");
        displayEntityProperties = OraxenPlugin.supportsDisplayEntities
                ? displayProperties != null
                ? new DisplayEntityProperties(displayProperties) : new DisplayEntityProperties()
                : null;

        ConfigurationSection hitboxSection = section.getConfigurationSection("hitbox");
        hitbox = hitboxSection != null ? new FurnitureHitbox(hitboxSection) : FurnitureHitbox.EMPTY;

        for (Object seatEntry : section.getList("seats", new ArrayList<>())) {
            FurnitureSeat seat = FurnitureSeat.getSeat(seatEntry);
            if (seat == null) continue;
            seats.add(seat);
        }

        ConfigurationSection evoSection = section.getConfigurationSection("evolution");
        evolvingFurniture = evoSection != null ? new EvolvingFurniture(getItemID(), evoSection) : null;
        if (evolvingFurniture != null) ((FurnitureFactory) getFactory()).registerEvolution();

        ConfigurationSection dropSection = section.getConfigurationSection("drop");
        drop = dropSection != null ? Drop.createDrop(FurnitureFactory.getInstance().toolTypes, dropSection, getItemID()) : new Drop(new ArrayList<>(), false, false, getItemID());

        ConfigurationSection limitedPlacingSection = section.getConfigurationSection("limited_placing");
        limitedPlacing = limitedPlacingSection != null ? new LimitedPlacing(limitedPlacingSection) : null;

        ConfigurationSection storageSection = section.getConfigurationSection("storage");
        storage = storageSection != null ? new StorageMechanic(storageSection) : null;

        ConfigurationSection blockSoundsSection = section.getConfigurationSection("block_sounds");
        blockSounds = blockSoundsSection != null ? new BlockSounds(blockSoundsSection) : null;

        ConfigurationSection jukeboxSection = section.getConfigurationSection("jukebox");
        jukebox = jukeboxSection != null ? new JukeboxBlock(mechanicFactory, jukeboxSection) : null;

        clickActions = ClickAction.parseList(section);

        if (section.getBoolean("rotatable", false)) {
            if (hitbox.barrierHitboxes().stream().anyMatch(b -> b.getX() != 0 || b.getZ() != 0)) {
                Logs.logWarning("Furniture <gold>" + getItemID() + " </gold>has barriers with non-zero X or Z coordinates.");
                Logs.logWarning("Furniture rotation will be disabled for this furniture.");
                isRotatable = false;
            } else isRotatable = true;
        } else isRotatable = false;

        ConfigurationSection blockLockerSection = section.getConfigurationSection("blocklocker");
        blockLocker = blockLockerSection != null ? new BlockLockerMechanic(blockLockerSection) : null;
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
                    && entity.getPersistentDataContainer().has(FURNITURE_KEY, DataType.STRING)) {
                return seat;
            }
        }

        return null;
    }

    public boolean hasHardness() {
        return hardness != -1;
    }

    public int getHardness() {
        return hardness;
    }

    public boolean hasLimitedPlacing() {
        return limitedPlacing != null;
    }

    public LimitedPlacing limitedPlacing() {
        return limitedPlacing;
    }

    public boolean isStorage() {
        return storage != null;
    }

    public StorageMechanic storage() {
        return storage;
    }

    public boolean hasBlockSounds() {
        return blockSounds != null;
    }

    public BlockSounds blockSounds() {
        return blockSounds;
    }

    public boolean isJukebox() {
        return jukebox != null;
    }

    public JukeboxBlock jukebox() {
        return jukebox;
    }

    public FurnitureHitbox hitbox() {
        return hitbox;
    }

    public boolean hasSeats() {
        return !seats.isEmpty();
    }

    public List<FurnitureSeat> seats() {
        return seats;
    }

    public Drop drop() {
        return drop;
    }

    public boolean hasEvolution() {
        return evolvingFurniture != null;
    }

    public EvolvingFurniture evolution() {
        return evolvingFurniture;
    }

    public boolean isRotatable() {
        return isRotatable;
    }

    public boolean isInteractable() {
        return isRotatable || hasSeats() || isStorage();
    }

    public void setPlacedItem() {
        if (placedItem == null) {
            placedItem = OraxenItems.getItemById(placedItemId != null ? placedItemId : getItemID()).build();
            //Utils.editItemMeta(placedItem, meta -> meta.setDisplayName(""));
        }
    }

    public Entity place(Location location) {
        setPlacedItem();
        return place(location, placedItem, 0f, BlockFace.NORTH, true);
    }

    public Entity place(Location location, Float yaw, BlockFace facing) {
        setPlacedItem();
        return place(location, placedItem, yaw, facing, true);
    }

    public Entity place(Location location, ItemStack originalItem, Float yaw, BlockFace facing, boolean checkSpace) {
        if (!location.isWorldLoaded()) return null;
        if (checkSpace && !this.hasEnoughSpace(location, yaw)) return null;
        assert location.getWorld() != null;
        setPlacedItem();
        assert location.getWorld() != null;

        Class<? extends Entity> entityClass = furnitureEntityType().getEntityClass();
        if (entityClass == null) entityClass = ItemFrame.class;

        ItemStack item;
        if (evolvingFurniture == null) {
            item = ItemUtils.editItemMeta(originalItem.clone(), meta -> meta.setDisplayName(""));
        } else item = placedItem;
        item.setAmount(1);

        Entity baseEntity = EntityUtils.spawnEntity(correctedSpawnLocation(location, facing), entityClass, (e) -> setEntityData(e, yaw, item, facing));
        if (baseEntity == null) return null;
        if (this.isModelEngine() && PluginUtils.isEnabled("ModelEngine")) {
            spawnModelEngineFurniture(baseEntity);
        }
        FurnitureSeat.spawnSeats(baseEntity, this);

        //hitbox.handleHitboxes(baseEntity, this);

        return baseEntity;
    }

    private Location correctedSpawnLocation(Location baseLocation, BlockFace facing) {
        Location correctedLocation = BlockHelpers.toCenterBlockLocation(baseLocation);
        boolean isWall = hasLimitedPlacing() && limitedPlacing.isWall();
        boolean isRoof = hasLimitedPlacing() && limitedPlacing.isRoof();
        boolean isFixed = hasDisplayEntityProperties() && displayEntityProperties.getDisplayTransform() == ItemDisplay.ItemDisplayTransform.FIXED;
        if (furnitureType != FurnitureType.DISPLAY_ENTITY || !hasDisplayEntityProperties()) return correctedLocation;
        if (displayEntityProperties.getDisplayTransform() != ItemDisplay.ItemDisplayTransform.NONE && !isWall && !isRoof) return correctedLocation;
        float scale = displayEntityProperties.hasScale() ? displayEntityProperties.getScale().y() : 1;
        // Since roof-furniture need to be more or less flipped, we have to add 0.5 (0.49 or it is "inside" the block above) to the Y coordinate
        if (isFixed && isWall) correctedLocation.add(-facing.getModX() * (0.49 * scale), 0, -facing.getModZ() * (0.49 * scale));
        return correctedLocation.add(0, (0.5 * scale) + (isRoof ? isFixed ? 0.49 : -1 * 1 : 0), 0);
    }

    public void setEntityData(Entity entity, float yaw, BlockFace facing) {
        setEntityData(entity, yaw, FurnitureHelpers.furnitureItem(entity), facing);
    }

    public void setEntityData(Entity entity, float yaw, ItemStack item, BlockFace facing) {
        setBaseFurnitureData(entity);
        Location location = entity.getLocation();
        if (entity instanceof ItemFrame frame) {
            setFrameData(frame, item, yaw, facing);

            if (false);//(hasBarriers()) setBarrierHitbox(entity, location, yaw);
            else {
                //float width = hasHitbox() ? hitbox.width : 1f;
                //float height = hasHitbox() ? hitbox.height : 1f;
                //spawnInteractionEntity(frame, location, width, height);
                Block block = location.getBlock();
                if (light.hasLightLevel()) light.createBlockLight(block);
            }
        } else if (entity instanceof ItemDisplay itemDisplay) {
            setItemDisplayData(itemDisplay, item, yaw, displayEntityProperties);
            //float width = hasHitbox() ? hitbox.width : displayEntityProperties.getDisplayWidth();
            //float height = hasHitbox() ? hitbox.height : displayEntityProperties.getDisplayHeight();
            boolean isFixed = displayEntityProperties.getDisplayTransform() == ItemDisplay.ItemDisplayTransform.FIXED;
            Location interactionLoc = location.clone().subtract(0, (hasLimitedPlacing() && limitedPlacing.isRoof() && isFixed) ? 1.5 * (1.0 - 1) : 0, 0);
            //spawnInteractionEntity(itemDisplay, interactionLoc, width, height);
            Location barrierLoc = EntityUtils.isNone(itemDisplay) && displayEntityProperties.hasScale()
                            ? location.clone().subtract(0, 0.5 * displayEntityProperties.getScale().y(), 0) : location;

            //if (hasBarriers()) setBarrierHitbox(entity, barrierLoc, yaw);
            if (light.hasLightLevel()) light.createBlockLight(location.getBlock());
        }
    }

    private void setBaseFurnitureData(Entity entity) {
        entity.setPersistent(true);
        entity.setCustomNameVisible(false);
        PersistentDataContainer pdc = entity.getPersistentDataContainer();
        pdc.set(FURNITURE_KEY, PersistentDataType.STRING, getItemID());
        if (hasEvolution()) pdc.set(EVOLUTION_KEY, PersistentDataType.INTEGER, 0);
        if (isStorage() && storage().getStorageType() == StorageMechanic.StorageType.STORAGE) {
            pdc.set(StorageMechanic.STORAGE_KEY, DataType.ITEM_STACK_ARRAY, new ItemStack[]{});
        }
    }

    private void setItemDisplayData(ItemDisplay itemDisplay, ItemStack item, Float yaw, DisplayEntityProperties properties) {
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

        itemDisplay.setDisplayWidth(properties.getDisplayWidth());
        itemDisplay.setDisplayHeight(properties.getDisplayHeight());
        itemDisplay.setItemStack(item);

        // Set scale to .5 if FIXED aka ItemFrame to fix size. Also flip it 90 degrees on pitch
        boolean isFixed = properties.getDisplayTransform().equals(ItemDisplay.ItemDisplayTransform.FIXED);
        Transformation transform = itemDisplay.getTransformation();
        if (properties.hasScale()) {
            transform.getScale().set(properties.getScale());
        } else transform.getScale().set(isFixed ? new Vector3f(0.5f, 0.5f, 0.5f) : new Vector3f(1f, 1f, 1f));

        // since FIXED is meant to mimic ItemFrames, we rotate it to match the ItemFrame's rotation
        // 1.20 Fixes this, will break for 1.19.4 but added disclaimer in console
        float pitch;
        yaw = (VersionUtil.atOrAbove("1.20.1") && (!hasLimitedPlacing() || !limitedPlacing.isRoof() || !isFixed)) ? yaw : yaw - 180;
        if (VersionUtil.atOrAbove("1.20.1")) {
            if (hasLimitedPlacing() && isFixed)
                if (limitedPlacing.isFloor()) pitch = -90;
                else if (limitedPlacing.isRoof()) pitch = 90;
                else pitch = 0;
            else pitch = 0;
        }
        else pitch = isFixed && hasLimitedPlacing() ? limitedPlacing.isFloor() ? 90 : limitedPlacing.isWall() ? 0 : limitedPlacing.isRoof() ? -90 : 0 : 0;

        itemDisplay.setTransformation(transform);
        itemDisplay.setRotation(yaw, pitch);
    }

    private void setFrameData(ItemFrame frame, ItemStack item, float yaw, BlockFace facing) {
        frame.setVisible(false);
        frame.setItemDropChance(0);
        frame.setFacingDirection(facing, true);
        frame.setItem(item, false);
        frame.setRotation(FurnitureHelpers.yawToRotation(yaw));

        if (hasLimitedPlacing()) {
            if (limitedPlacing.isFloor() && !limitedPlacing.isWall() && frame.getLocation().getBlock().getRelative(BlockFace.DOWN).getType().isSolid())
                frame.setFacingDirection(BlockFace.UP, true);
            else if (limitedPlacing.isWall() && facing.getModY() == 0)
                frame.setRotation(Rotation.NONE);
            else if (limitedPlacing.isRoof() && facing == BlockFace.DOWN)
                frame.setFacingDirection(BlockFace.DOWN, true);
        }
    }

    private void spawnModelEngineFurniture(@NotNull Entity entity) {
        ModeledEntity modelEntity = ModelEngineAPI.getOrCreateModeledEntity(entity);
        ActiveModel activeModel = ModelEngineAPI.createActiveModel(getModelEngineID());
        ModelEngineUtils.addModel(modelEntity, activeModel, true);
        ModelEngineUtils.setRotationLock(modelEntity, false);
        modelEntity.setBaseEntityVisible(false);
    }

    public void removeBaseEntity(@NotNull Entity baseEntity) {
        if (light.hasLightLevel()) light.removeBlockLight(baseEntity.getLocation().getBlock());
        if (hasSeats()) removeFurnitureSeats(baseEntity);
        FurnitureFactory.instance.furniturePacketManager().removeInteractionHitboxPacket(baseEntity, this);
        FurnitureFactory.instance.furniturePacketManager().removeBarrierHitboxPacket(baseEntity, this);

        if (!baseEntity.isDead()) baseEntity.remove();
    }

    private void removeFurnitureSeats(Entity baseEntity) {
        List<Entity> seats = baseEntity.getPersistentDataContainer()
                .getOrDefault(FurnitureSeat.SEAT_KEY, DataType.asList(DataType.UUID), new ArrayList<>())
                .stream().map(Bukkit::getEntity).filter(Objects::nonNull).filter(e -> e instanceof ArmorStand).toList();

        for (Entity seat : seats) {
            seat.getPassengers().forEach(seat::removePassenger);
            if (!seat.isDead()) seat.remove();
        }
    }

    public boolean hasEnoughSpace(Location rootLocation, float yaw) {
        return hitbox.hitboxLocations(rootLocation, yaw).stream().allMatch(l -> l.getBlock().isReplaceable());
    }

    public void runClickActions(final Player player) {
        for (final ClickAction action : clickActions) {
            if (action.canRun(player)) {
                action.performActions(player);
            }
        }
    }

    @Nullable
    public Entity baseEntity(Block block) {
        BlockPosition blockPosition = new BlockPosition(block.getX(), block.getY(), block.getZ());
        return FurnitureFactory.instance.furniturePacketManager().baseEntityFromHitbox(blockPosition);
    }

    @Nullable
    public Entity baseEntity(Location location) {
        if (location == null) return null;
        BlockPosition blockPosition = new BlockPosition(location.getBlockX(), location.getBlockY(), location.getBlockZ());
        return FurnitureFactory.instance.furniturePacketManager().baseEntityFromHitbox(blockPosition);
    }

    @Nullable Entity baseEntity(BlockPosition blockPosition) {
        return FurnitureFactory.instance.furniturePacketManager().baseEntityFromHitbox(blockPosition);
    }

    @Nullable Entity baseEntity(int interactionId) {
        return FurnitureFactory.instance.furniturePacketManager().baseEntityFromHitbox(interactionId);
    }

    public FurnitureType furnitureType() {
        return furnitureType;
    }

    public EntityType furnitureEntityType() {

        return switch (furnitureType) {
            case ITEM_FRAME -> EntityType.ITEM_FRAME;
            case GLOW_ITEM_FRAME -> EntityType.GLOW_ITEM_FRAME;
            case DISPLAY_ENTITY ->
                    OraxenPlugin.supportsDisplayEntities ? EntityType.ITEM_DISPLAY : EntityType.ITEM_FRAME;
            //case ARMOR_STAND -> EntityType.ARMOR_STAND;
        };
    }

    public Class<? extends Entity> furnitureEntityClass() {
        return switch (furnitureType) {
            case ITEM_FRAME -> ItemFrame.class;
            case GLOW_ITEM_FRAME -> GlowItemFrame.class;
            case DISPLAY_ENTITY -> OraxenPlugin.supportsDisplayEntities ? ItemDisplay.class : ItemFrame.class;
            //case ARMOR_STAND -> ArmorStand.class;
        };
    }

    public boolean hasDisplayEntityProperties() {
        return displayEntityProperties != null;
    }

    public DisplayEntityProperties displayEntityProperties() {
        return displayEntityProperties;
    }

    public RestrictedRotation restrictedRotation() {
        return restrictedRotation;
    }

    public void rotateFurniture(Entity baseEntity) {
        float yaw = FurnitureHelpers.furnitureYaw(baseEntity);
        yaw = FurnitureHelpers.rotationToYaw(rotateClockwise(FurnitureHelpers.yawToRotation(yaw)));
        FurnitureHelpers.furnitureYaw(baseEntity, yaw);

        hitbox.handleHitboxes(baseEntity, this);
    }

    private Rotation rotateClockwise(Rotation rotation) {
        int offset = restrictedRotation == RestrictedRotation.VERY_STRICT ? 2 : 1;
        return Rotation.values()[(rotation.ordinal() + offset) & 0x7];
    }

    public BlockLockerMechanic blocklocker() {
        return blockLocker;
    }

    public boolean hasLight() {
        return light.hasLightLevel();
    }

    public LightMechanic light() {
        return light;
    }
}
