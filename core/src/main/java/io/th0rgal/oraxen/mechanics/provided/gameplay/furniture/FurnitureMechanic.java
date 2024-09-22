package io.th0rgal.oraxen.mechanics.provided.gameplay.furniture;

import com.jeff_media.customblockdata.CustomBlockData;
import com.jeff_media.morepersistentdatatypes.DataType;
import com.ticxo.modelengine.api.ModelEngineAPI;
import com.ticxo.modelengine.api.model.ActiveModel;
import com.ticxo.modelengine.api.model.ModeledEntity;
import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.api.OraxenFurniture;
import io.th0rgal.oraxen.api.OraxenItems;
import io.th0rgal.oraxen.compatibilities.provided.blocklocker.BlockLockerMechanic;
import io.th0rgal.oraxen.mechanics.Mechanic;
import io.th0rgal.oraxen.mechanics.MechanicFactory;
import io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.evolution.EvolvingFurniture;
import io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.jukebox.JukeboxBlock;
import io.th0rgal.oraxen.mechanics.provided.gameplay.light.LightMechanic;
import io.th0rgal.oraxen.mechanics.provided.gameplay.limitedplacing.LimitedPlacing;
import io.th0rgal.oraxen.mechanics.provided.gameplay.storage.StorageMechanic;
import io.th0rgal.oraxen.utils.*;
import io.th0rgal.oraxen.utils.actions.ClickAction;
import io.th0rgal.oraxen.utils.blocksounds.BlockSounds;
import io.th0rgal.oraxen.utils.drops.Drop;
import io.th0rgal.oraxen.utils.logs.Logs;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.*;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Transformation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.*;

public class FurnitureMechanic extends Mechanic {

    public static final NamespacedKey FURNITURE_KEY = new NamespacedKey(OraxenPlugin.get(), "furniture");
    public static final NamespacedKey BASE_ENTITY_KEY = new NamespacedKey(OraxenPlugin.get(), "base_entity");
    public static final NamespacedKey INTERACTION_KEY = new NamespacedKey(OraxenPlugin.get(), "interaction");
    public static final NamespacedKey MODELENGINE_KEY = new NamespacedKey(OraxenPlugin.get(), "modelengine");
    public static final NamespacedKey SEAT_KEY = new NamespacedKey(OraxenPlugin.get(), "seat");
    public static final NamespacedKey ROOT_KEY = new NamespacedKey(OraxenPlugin.get(), "root");
    public static final NamespacedKey ORIENTATION_KEY = new NamespacedKey(OraxenPlugin.get(), "orientation");
    public static final NamespacedKey EVOLUTION_KEY = new NamespacedKey(OraxenPlugin.get(), "evolution");
    public static final NamespacedKey BARRIER_KEY = new NamespacedKey(OraxenPlugin.get(), "barriers");

    private final int hardness;
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
    private final LightMechanic light;
    private final String modelEngineID;
    private final String placedItemId;
    private float seatHeight;
    private float seatYaw;
    private final List<ClickAction> clickActions;
    private FurnitureType furnitureType;
    private final DisplayEntityProperties displayEntityProperties;
    private final FurnitureHitbox hitbox;
    private final boolean isRotatable;
    private final BlockLockerMechanic blockLocker;
    private final RestrictedRotation restrictedRotation;

    public record FurnitureHitbox(float width, float height) {
    }

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
        placedItemId = section.getString("item", "");
        modelEngineID = section.getString("modelengine_id", null);
        farmlandRequired = section.getBoolean("farmland_required", false);
        farmblockRequired = section.getBoolean("farmblock_required", false);
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

        barriers = new ArrayList<>();
        if (section.getBoolean("barrier", false))
            barriers.add(new BlockLocation(0, 0, 0));
        if (section.isList("barriers")) {
            for (Object barrierObject : section.getList("barriers", new ArrayList<>()))
                if (barrierObject instanceof String string && string.equals("origin"))
                    barriers.add(new BlockLocation(0, 0, 0));
                else if (barrierObject instanceof Map<?, ?> barrierMap) {
                    try {
                        barriers.add(new BlockLocation((Map<String, Integer>) barrierMap));
                    } catch (ClassCastException e) {
                        Logs.logError("Invalid barrier location: " + barrierMap + " for furniture: " + getItemID());
                    }
                }
        }

        ConfigurationSection hitboxSection = section.getConfigurationSection("hitbox");
        if (hitboxSection != null) {
            float width = (float) hitboxSection.getDouble("width", 1.0), height = (float) hitboxSection.getDouble("height", 1.0);
            hitbox = width > 0 && height > 0 ? new FurnitureHitbox(width, height) : null;
        } else hitbox = !hasBarriers() ? new FurnitureHitbox(1.0f, 1.0f) : null;

        ConfigurationSection seatSection = section.getConfigurationSection("seat");
        if (seatSection != null) {
            hasSeat = true;
            seatHeight = (float) seatSection.getDouble("height");
            hasSeatYaw = seatSection.contains("yaw");
            if (hasSeatYaw) seatYaw = (float) seatSection.getDouble("yaw");
        } else hasSeat = false;

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
            if (barriers.stream().anyMatch(b -> b.getX() != 0 || b.getZ() != 0)) {
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

    public static ArmorStand getSeat(Entity baseEntity) {
        PersistentDataContainer pdc = baseEntity.getPersistentDataContainer();
        if (!pdc.has(SEAT_KEY, DataType.UUID)) return null;
        UUID seatUUID = pdc.get(SEAT_KEY, DataType.UUID);
        if (seatUUID == null) return null;
        Entity seat = Bukkit.getEntity(seatUUID);
        return seat instanceof ArmorStand ? (ArmorStand) seat : null;
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

    /**
     * Checks if the given entity has barriers.
     * This method also checks the actual entity in-case the config changed.
     * @param baseEntity the entity to check
     * @return true if the entity has barriers
     */
    public boolean hasBarriers(@NotNull Entity baseEntity) {
        return hasBarriers() || !baseEntity.getPersistentDataContainer().getOrDefault(BARRIER_KEY, DataType.asList(BlockLocation.dataType), new ArrayList<>()).isEmpty();
    }

    public List<BlockLocation> getBarriers() {
        return barriers;
    }

    /**
     * Gets the barriers for the given entity.
     * This method also checks the actual entity in-case the config changed.
     * @param baseEntity the entity to get the barriers for
     * @return the barriers for the given entity
     */
    public List<BlockLocation> getBarriers(@NotNull Entity baseEntity) {
        List<BlockLocation> barriers = baseEntity.getPersistentDataContainer().getOrDefault(BARRIER_KEY, DataType.asList(BlockLocation.dataType), new ArrayList<>());
        return barriers.isEmpty() ? this.barriers : barriers;
    }

    public boolean hasHitbox() {
        return hitbox != null;
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

    public boolean isRotatable() {
        return isRotatable;
    }

    public boolean isInteractable() {
        return isRotatable || hasSeat || isStorage();
    }

    public Entity place(Location location) {

        return place(location, OraxenItems.getItemById(getItemID()).build(), 0f, BlockFace.NORTH, true);
    }

    public Entity place(Location location, Float yaw, BlockFace facing) {
        return place(location, OraxenItems.getItemById(getItemID()).build(), yaw, facing, true);
    }

    public Entity place(Location location, ItemStack originalItem, Float yaw, BlockFace facing, boolean checkSpace) {
        if (!location.isWorldLoaded()) return null;
        if (checkSpace && this.notEnoughSpace(yaw, location)) return null;
        assert location.getWorld() != null;
        assert location.getWorld() != null;

        Class<? extends Entity> entityClass = getFurnitureEntityType().getEntityClass();
        if (entityClass == null) entityClass = ItemFrame.class;

        ItemStack item = OraxenItems.getOptionalItemById(placedItemId).map(b -> b.build().clone()).orElse(originalItem.clone());
        if (evolvingFurniture == null) {
            ItemUtils.editItemMeta(item, meta -> meta.setDisplayName(""));
        }
        item.setAmount(1);

        Entity baseEntity = EntityUtils.spawnEntity(correctedSpawnLocation(location, facing), entityClass, (e) -> setEntityData(e, yaw, item, facing));
        if (this.isModelEngine() && PluginUtils.isEnabled("ModelEngine")) {
            spawnModelEngineFurniture(baseEntity);
        }

        return baseEntity;
    }

    private boolean allowWallForLimitedFloor(Location location, BlockFace blockFace) {
        return blockFace.getModY() == 0 && location.getBlock().getRelative(BlockFace.DOWN).isSolid();
    }

    private Location correctedSpawnLocation(Location baseLocation, BlockFace facing) {
        boolean isWall = hasLimitedPlacing() && limitedPlacing.isWall();
        boolean isRoof = hasLimitedPlacing() && limitedPlacing.isRoof();
        boolean isFixed = hasDisplayEntityProperties() && displayEntityProperties.getDisplayTransform() == ItemDisplay.ItemDisplayTransform.FIXED;
        Location correctedLocation = isFixed && (facing == BlockFace.UP || allowWallForLimitedFloor(baseLocation, facing))
                ? BlockHelpers.toCenterBlockLocation(baseLocation) : BlockHelpers.toCenterLocation(baseLocation);

        if (furnitureType != FurnitureType.DISPLAY_ENTITY || !hasDisplayEntityProperties()) return correctedLocation;
        if (displayEntityProperties.getDisplayTransform() != ItemDisplay.ItemDisplayTransform.NONE && !isWall && !isRoof) return correctedLocation;
        float scale = displayEntityProperties.hasScale() ? displayEntityProperties.getScale().y() : 1;
        // Since roof-furniture need to be more or less flipped, we have to add 0.5 (0.49 or it is "inside" the block above) to the Y coordinate
        if (isFixed && isWall && facing.getModY() == 0) correctedLocation.add(-facing.getModX() * (0.49 * scale), 0, -facing.getModZ() * (0.49 * scale));

        float hitboxOffset = (hasHitbox() ? hitbox.height : 1) - 1;
        double yCorrection = ((isRoof && facing == BlockFace.DOWN) ? isFixed ? 0.49 : -1 * hitboxOffset : 0);

        return correctedLocation.add(0,  yCorrection, 0);
    }

    public void setEntityData(Entity entity, float yaw, BlockFace facing) {
        setEntityData(entity, yaw, getFurnitureItem(entity), facing);
    }

    public void setEntityData(Entity entity, float yaw, ItemStack item, BlockFace facing) {
        setBaseFurnitureData(entity);
        Location location = entity.getLocation();
        if (entity instanceof ItemFrame frame) {
            setFrameData(frame, item, yaw, facing);

            if (hasBarriers()) setBarrierHitbox(entity, location, yaw);
            else {
                float width = hasHitbox() ? hitbox.width : 1f;
                float height = hasHitbox() ? hitbox.height : 1f;
                Entity interaction = spawnInteractionEntity(frame, location, width, height);

                Block block = location.getBlock();
                if (hasSeat() && interaction != null) {
                    UUID seatUuid = spawnSeat(block, hasSeatYaw ? seatYaw : FurnitureMechanic.getFurnitureYaw(frame));
                    interaction.getPersistentDataContainer().set(SEAT_KEY, DataType.UUID, seatUuid);
                    frame.getPersistentDataContainer().set(SEAT_KEY, DataType.UUID, seatUuid);
                }
                if (light.hasLightLevel()) light.createBlockLight(block);
            }
        } else if (entity instanceof ItemDisplay itemDisplay) {
            setItemDisplayData(itemDisplay, item, yaw, displayEntityProperties, facing);
            float width = hasHitbox() ? hitbox.width : displayEntityProperties.getDisplayWidth();
            float height = hasHitbox() ? hitbox.height : displayEntityProperties.getDisplayHeight();
            boolean isFixed = displayEntityProperties.getDisplayTransform() == ItemDisplay.ItemDisplayTransform.FIXED;
            Location interactionLoc = location.clone().subtract(0, (hasLimitedPlacing() && limitedPlacing.isRoof() && isFixed) ? 1.5 * (height - 1) : 0, 0);
            Interaction interaction = spawnInteractionEntity(itemDisplay, interactionLoc, width, height);
            Location barrierLoc = EntityUtils.isNone(itemDisplay) && displayEntityProperties.hasScale()
                            ? location.clone().subtract(0, 0.5 * displayEntityProperties.getScale().y(), 0) : location;

            if (hasBarriers()) setBarrierHitbox(entity, barrierLoc, yaw);
            else if (hasSeat() && interaction != null) {
                UUID seatUuid = spawnSeat(location.getBlock(), hasSeatYaw ? seatYaw : yaw);
                interaction.getPersistentDataContainer().set(SEAT_KEY, DataType.UUID, seatUuid);
                itemDisplay.getPersistentDataContainer().set(SEAT_KEY, DataType.UUID, seatUuid);
            }
            if (light.hasLightLevel()) light.createBlockLight(location.getBlock());
        }
    }

    private Interaction spawnInteractionEntity(Entity entity, Location location, float width, float height) {
        if (!OraxenPlugin.supportsDisplayEntities || width <= 0f || height <= 0f) return null;
        UUID existingInteractionUUID = entity.getPersistentDataContainer().get(INTERACTION_KEY, DataType.UUID);
        if (existingInteractionUUID != null) {
            Entity existingInteraction = Bukkit.getEntity(existingInteractionUUID);
            if (existingInteraction instanceof Interaction interaction) return interaction;
        }

        Interaction interaction = EntityUtils.spawnEntity(BlockHelpers.toCenterBlockLocation(location), Interaction.class, (i) -> {
            i.setInteractionWidth(width);
            i.setInteractionHeight(height);
            i.setPersistent(true);
        });

        PersistentDataContainer pdc = interaction.getPersistentDataContainer();
        pdc.set(FURNITURE_KEY, DataType.STRING, getItemID());
        pdc.set(BASE_ENTITY_KEY, DataType.UUID, entity.getUniqueId());
        entity.getPersistentDataContainer().set(INTERACTION_KEY, DataType.UUID, interaction.getUniqueId());

        return interaction;
    }

    private void setBaseFurnitureData(Entity entity) {
        entity.setPersistent(true);
        entity.setCustomNameVisible(false);
        PersistentDataContainer pdc = entity.getPersistentDataContainer();
        pdc.set(FURNITURE_KEY, PersistentDataType.STRING, getItemID());
        pdc.set(BARRIER_KEY, DataType.asList(BlockLocation.dataType), barriers);
        if (hasEvolution()) pdc.set(EVOLUTION_KEY, PersistentDataType.INTEGER, 0);
        if (isStorage() && getStorage().getStorageType() == StorageMechanic.StorageType.STORAGE) {
            pdc.set(StorageMechanic.STORAGE_KEY, DataType.ITEM_STACK_ARRAY, new ItemStack[]{});
        }
    }

    private void setItemDisplayData(ItemDisplay itemDisplay, ItemStack item, Float yaw, DisplayEntityProperties properties, BlockFace facing) {
        itemDisplay.setItemDisplayTransform(properties.getDisplayTransform());
        if (properties.hasSpecifiedViewRange()) itemDisplay.setViewRange(properties.getViewRange());
        if (properties.hasInterpolationDuration()) itemDisplay.setInterpolationDuration(properties.getInterpolationDuration());
        if (properties.hasInterpolationDelay()) itemDisplay.setInterpolationDelay(properties.getInterpolationDelay());
        if (properties.hasTrackingRotation()) itemDisplay.setBillboard(properties.getTrackingRotation());
        if (properties.hasShadowRadius()) itemDisplay.setShadowRadius(properties.getShadowRadius());
        if (properties.hasShadowStrength()) itemDisplay.setShadowStrength(properties.getShadowStrength());
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
        if (VersionUtil.atOrAbove("1.20.1")) {
            if (hasLimitedPlacing() && isFixed) {
                if (limitedPlacing.isFloor() && (facing == BlockFace.UP || allowWallForLimitedFloor(itemDisplay.getLocation(), facing))) pitch = -90;
                else if (limitedPlacing.isRoof() && facing == BlockFace.DOWN) pitch = 90;
                else pitch = 0;

                if (limitedPlacing.isRoof() && facing == BlockFace.DOWN) yaw -= 180;
                else if (limitedPlacing.isWall() && facing.getModY() == 0) yaw = 90f * facing.ordinal() - 180;
            } else pitch = 0;
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
        frame.setRotation(yawToRotation(yaw));

        if (hasLimitedPlacing()) {
            if (limitedPlacing.isFloor() && !limitedPlacing.isWall() && frame.getLocation().getBlock().getRelative(BlockFace.DOWN).getType().isSolid())
                frame.setFacingDirection(BlockFace.UP, true);
            else if (limitedPlacing.isWall() && facing.getModY() == 0)
                frame.setRotation(Rotation.NONE);
            else if (limitedPlacing.isRoof() && facing == BlockFace.DOWN)
                frame.setFacingDirection(BlockFace.DOWN, true);
        }
    }

    private void setBarrierHitbox(Entity entity, Location location, float yaw) {
        List<Location> barrierLocations = getLocations(yaw, BlockHelpers.toCenterBlockLocation(location), barriers);
        for (Location barrierLocation : barrierLocations) {
            Block block = barrierLocation.getBlock();
            block.setType(Material.BARRIER);
            PersistentDataContainer data = BlockHelpers.getPDC(block);
            data.set(FURNITURE_KEY, PersistentDataType.STRING, getItemID());
            if (hasSeat) data.set(SEAT_KEY, DataType.UUID, spawnSeat(block, hasSeatYaw ? seatYaw : yaw));
            data.set(ROOT_KEY, PersistentDataType.STRING, new BlockLocation(location.clone()).toString());
            data.set(ORIENTATION_KEY, PersistentDataType.FLOAT, yaw);
            data.set(BASE_ENTITY_KEY, DataType.UUID, entity.getUniqueId());
            if (light.hasLightLevel()) light.createBlockLight(block);
        }
    }

    void spawnModelEngineFurniture(Entity entity) {
        ModeledEntity modelEntity = ModelEngineAPI.getOrCreateModeledEntity(entity);
        ActiveModel activeModel = ModelEngineAPI.createActiveModel(getModelEngineID());
        ModelEngineUtils.addModel(modelEntity, activeModel, true);
        ModelEngineUtils.setRotationLock(modelEntity, false);
        modelEntity.setBaseEntityVisible(false);
    }

    public static ItemStack getFurnitureItem(Entity entity) {
        return switch (entity.getType()) {
            case ARMOR_STAND -> ((ArmorStand) entity).getEquipment().getHelmet();
            case ITEM_DISPLAY -> OraxenPlugin.supportsDisplayEntities ? ((ItemDisplay) entity).getItemStack() : null;
            default -> ((ItemFrame) entity).getItem();
        };
    }

    public static void setFurnitureItem(Entity entity, ItemStack item) {
        switch (entity.getType()) {
            case ITEM_FRAME, GLOW_ITEM_FRAME -> ((ItemFrame) entity).setItem(item, false);
            case ARMOR_STAND -> ((ArmorStand) entity).getEquipment().setHelmet(item);
            case ITEM_DISPLAY -> {
                if (OraxenPlugin.supportsDisplayEntities) ((ItemDisplay) entity).setItemStack(item);
            }
            default -> {
            }
        }
    }

    public void removeSolid(Entity baseEntity, Location rootLocation, float orientation) {
        if (hasLimitedPlacing() && limitedPlacing.isRoof() && furnitureType == FurnitureType.DISPLAY_ENTITY)
            orientation = orientation - 180;
        List<BlockLocation> blockLocations = baseEntity.getPersistentDataContainer().getOrDefault(BARRIER_KEY, DataType.asList(BlockLocation.dataType), new ArrayList<>());
        List<Location> barrierLocations = getLocations(orientation, rootLocation, blockLocations.isEmpty() ? getBarriers() : blockLocations);

        for (Location location : barrierLocations) {
            Block block = location.getBlock();
            if (hasSeat) removeFurnitureSeat(location);
            if (block.getType() != Material.BARRIER) continue;
            if (!BlockHelpers.getPDC(block).getOrDefault(BASE_ENTITY_KEY, DataType.UUID, UUID.randomUUID()).equals(baseEntity.getUniqueId())) continue;

            block.setType(Material.AIR);
            new CustomBlockData(location.getBlock(), OraxenPlugin.get()).clear();
            if (light.hasLightLevel()) light.removeBlockLight(block);
        }
        removeBaseEntity(baseEntity);
    }

    public void removeNonSolidFurniture(Entity baseEntity) {
        removeBaseEntity(baseEntity);
    }

    private void removeBaseEntity(Entity baseEntity) {
        if (baseEntity == null) return;
        removeSubEntitiesOfFurniture(baseEntity);
        if (light.hasLightLevel()) light.removeBlockLight(baseEntity.getLocation().getBlock());
        if (!baseEntity.isDead()) baseEntity.remove();
    }

    private void removeSubEntitiesOfFurniture(Entity baseEntity) {
        if (light.hasLightLevel()) light.removeBlockLight(baseEntity.getLocation().getBlock());
        if (hasSeat) removeFurnitureSeat(baseEntity.getLocation());

        if (OraxenPlugin.supportsDisplayEntities) {
            Interaction interaction = getInteractionEntity(baseEntity);
            if (interaction != null && !interaction.isDead()) interaction.remove();
        }
    }

    private void removeFurnitureSeat(Location location) {
        ArmorStand seat = getSeat(location);
        if (seat != null) {
            seat.getPassengers().forEach(seat::removePassenger);
            if (!seat.isDead()) seat.remove();
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
        FurnitureMechanic mechanic = OraxenFurniture.getFurnitureMechanic(entity);
        if (mechanic == null) return entity.getLocation().getYaw();

        if (entity instanceof ItemFrame itemFrame) {
            if (mechanic.hasLimitedPlacing() && mechanic.limitedPlacing.isWall() && itemFrame.getFacing().getModY() == 0)
                return entity.getLocation().getYaw();
            else return rotationToYaw(itemFrame.getRotation());
        } else return entity.getLocation().getYaw();
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

    private UUID spawnSeat(Block target, float yaw) {
        final ArmorStand seat = EntityUtils.spawnEntity(target.getLocation().add(0.5, seatHeight - 1, 0.5), ArmorStand.class, (ArmorStand stand) -> {
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
            stand.getPersistentDataContainer().set(FURNITURE_KEY, PersistentDataType.STRING, getItemID());
        });
        return seat.getUniqueId();
    }

    @Nullable
    public Entity getBaseEntity(Block block) {
        PersistentDataContainer pdc = BlockHelpers.getPDC(block);
        if (pdc.isEmpty()) return null;
        final BlockLocation blockLoc = new BlockLocation(Objects.requireNonNull(pdc.get(ROOT_KEY, PersistentDataType.STRING)));
        Location originLoc = blockLoc.toLocation(block.getWorld());

        if (pdc.has(BASE_ENTITY_KEY, DataType.UUID))
            return Bukkit.getEntity(pdc.getOrDefault(BASE_ENTITY_KEY, DataType.UUID, UUID.randomUUID()));
        else if (hasBarriers()) for (Entity entity : block.getWorld().getNearbyEntities(originLoc, 1, 1, 1)) {
            if (entity.getType() == getFurnitureEntityType()
                    && entity.getLocation().getBlockX() == originLoc.getBlockX()
                    && entity.getLocation().getBlockY() == originLoc.getBlockY()
                    && entity.getLocation().getBlockZ() == originLoc.getBlockZ()
                    && OraxenFurniture.isFurniture(block))
                return getBaseEntity(entity);
        }

        return null;
    }

    @Nullable
    public Entity getBaseEntity(Entity entity) {
        // If the entity is the same type as the base entity, return it
        // Since ItemDisplay entities have no hitbox it will only be for ITEM_FRAME based ones
        if (getFurnitureEntityType() == entity.getType()) return entity;
        UUID baseEntityUUID = entity.getPersistentDataContainer().get(BASE_ENTITY_KEY, DataType.UUID);
        // If the baseEntity is null but the entity is a furniture, assume the config has changed the baseEntity type
        // This would cause the above check to fail and assume it is the interaction entity or not a furniture
        if (baseEntityUUID == null && OraxenFurniture.isFurniture(entity) && !OraxenFurniture.isInteractionEntity(entity))
            return entity;
        return baseEntityUUID != null && Bukkit.getEntity(baseEntityUUID) != null ? Bukkit.getEntity(baseEntityUUID) : getBaseEntityAlter(entity);
    }

    /**
     * Old method and inefficient method for getting the interaction entity. Kept for backwards compatibility.
     * When ran it will update the furniture to the new method without needing to replace it
     *
     * @apiNote Remove for 1.20
     */
    @Nullable
    private Entity getBaseEntityAlter(Entity entity) {
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
            // Update to new format
            entity.getPersistentDataContainer().set(BASE_ENTITY_KEY, DataType.UUID, baseEntity.getUniqueId());
            return baseEntity;
        }
        return null;
    }

    @Nullable
    public Interaction getInteractionEntity(@NotNull Entity baseEntity) {
        UUID interactionUUID = baseEntity.getPersistentDataContainer().get(INTERACTION_KEY, DataType.UUID);
        return OraxenPlugin.supportsDisplayEntities && interactionUUID != null && Bukkit.getEntity(interactionUUID) instanceof Interaction interaction
                ? interaction : getInteractionEntityAlter(baseEntity);
    }

    /**
     * Old method and inefficient method for getting the interaction entity. Kept for backwards compatibility.
     * When ran it will update the furniture to the new method without needing to replace it
     *
     * @apiNote Remove for 1.20
     */
    @Nullable
    private Interaction getInteractionEntityAlter(Entity baseEntity) {
        if (OraxenPlugin.supportsDisplayEntities) {
            for (Entity entity : baseEntity.getNearbyEntities(0.1, 0.1, 0.1)) {
                if (!(entity instanceof Interaction interaction)) continue;
                PersistentDataContainer pdc = interaction.getPersistentDataContainer();
                if (pdc.has(FURNITURE_KEY, DataType.STRING) && pdc.getOrDefault(FURNITURE_KEY, DataType.STRING, "").equals(getItemID())) {
                    if (pdc.has(ROOT_KEY, DataType.LOCATION) && Objects.equals(pdc.get(ROOT_KEY, DataType.LOCATION), baseEntity.getLocation())) {
                        // Update to new format
                        baseEntity.getPersistentDataContainer().set(INTERACTION_KEY, DataType.UUID, interaction.getUniqueId());
                        return interaction;
                    }
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
            case DISPLAY_ENTITY ->
                    OraxenPlugin.supportsDisplayEntities ? EntityType.ITEM_DISPLAY : EntityType.ITEM_FRAME;
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

    public boolean hasDisplayEntityProperties() {
        return displayEntityProperties != null;
    }

    public DisplayEntityProperties getDisplayEntityProperties() {
        return displayEntityProperties;
    }

    public static void sitOnSeat(PersistentDataContainer pdc, Player player) {
        UUID entityUuid = pdc.has(SEAT_KEY, DataType.UUID) ? pdc.get(SEAT_KEY, DataType.UUID) : null;

        //Convert old seats to new, remove in a good while
        if (entityUuid == null) {
            String oldUUID = pdc.has(SEAT_KEY, PersistentDataType.STRING) ? pdc.get(SEAT_KEY, PersistentDataType.STRING) : null;
            if (oldUUID != null) {
                entityUuid = UUID.fromString(oldUUID);
                pdc.remove(SEAT_KEY);
                pdc.set(SEAT_KEY, DataType.UUID, entityUuid);
            }
        }

        if (entityUuid != null) {
            Entity stand = Bukkit.getEntity(entityUuid);
            if (stand != null && stand.getPassengers().isEmpty()) {
                stand.addPassenger(player);
            }
        }
    }

    public RestrictedRotation getRestrictedRotation() {
        return restrictedRotation;
    }

    public void rotateFurniture(Entity baseEntity) {
        float yaw = FurnitureMechanic.getFurnitureYaw(baseEntity);
        Rotation newRotation = rotateClockwise(yawToRotation(yaw));
        if (baseEntity instanceof ItemFrame frame) frame.setRotation(newRotation);
        else baseEntity.setRotation(FurnitureMechanic.rotationToYaw(newRotation), baseEntity.getLocation().getPitch());
    }

    private Rotation rotateClockwise(Rotation rotation) {
        int offset = restrictedRotation == RestrictedRotation.VERY_STRICT ? 2 : 1;
        return Rotation.values()[(rotation.ordinal() + offset) & 0x7];
    }

    public BlockLockerMechanic getBlockLocker() {
        return blockLocker;
    }

    public boolean hasLight() {
        return light.hasLightLevel();
    }

    public LightMechanic getLight() {
        return light;
    }
}
