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
import io.th0rgal.oraxen.mechanics.provided.misc.storage.StorageMechanic;
import io.th0rgal.oraxen.utils.BlockHelpers;
import io.th0rgal.oraxen.utils.Utils;
import io.th0rgal.oraxen.utils.VersionUtil;
import io.th0rgal.oraxen.utils.actions.ClickAction;
import io.th0rgal.oraxen.utils.blocksounds.BlockSounds;
import io.th0rgal.oraxen.utils.drops.Drop;
import io.th0rgal.oraxen.utils.drops.Loot;
import io.th0rgal.oraxen.utils.limitedplacing.LimitedPlacing;
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
    private final boolean isRotatable;

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
                Logs.logWarning("Setting type to ITEM_FRAME for furniture: <i><gold>" + getItemID());
                furnitureType = FurnitureType.ITEM_FRAME;
            }
        } catch (IllegalArgumentException e) {
            Logs.logError("Use of illegal EntityType in furniture: <gold>" + getItemID());
            Logs.logWarning("Allowed ones are: <gold>" + Arrays.stream(FurnitureType.values()).map(Enum::name).toList());
            Logs.logWarning("Setting type to ITEM_FRAME for furniture: <gold>" + getItemID());
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

        if (section.getBoolean("rotatable", false)) {
            if (barriers.stream().anyMatch(b -> b.getX() != 0 || b.getZ() != 0)) {
                Logs.logWarning("Furniture <gold>" + getItemID() + " </gold>has barriers with non-zero X or Z coordinates.");
                Logs.logWarning("Furniture rotation will be disabled for this furniture.");
                isRotatable = false;
            } else isRotatable = true;
        } else isRotatable = false;

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

    public boolean isRotatable() { return isRotatable; }

    public boolean isInteractable() { return isRotatable || hasSeat || isStorage(); }

    public void setPlacedItem() {
        if (placedItem == null) {
            placedItem = OraxenItems.getItemById(placedItemId != null ? placedItemId : getItemID()).build();
            //Utils.editItemMeta(placedItem, meta -> meta.setDisplayName(""));
        }
    }

    public Entity place(Location location) {
        setPlacedItem();
        return place(location, placedItem, 0f, BlockFace.NORTH);
    }

    public Entity place(Location location, float yaw, BlockFace facing) {
        setPlacedItem();
        return place(location, placedItem, yaw, facing);
    }

    public Entity place(Location location, ItemStack originalItem, Float yaw, BlockFace facing) {
        if (!location.isWorldLoaded()) return null;
        if (this.notEnoughSpace(yaw, location)) return null;
        assert location.getWorld() != null;
        setPlacedItem();
        assert location.getWorld() != null;

        Class<? extends Entity> entityClass = getFurnitureEntityType().getEntityClass();
        if (entityClass == null) entityClass = ItemFrame.class;

        ItemStack item;
        if (evolvingFurniture == null) {
            item = Utils.editItemMeta(originalItem.clone(), meta -> meta.setDisplayName(""));
        } else item = placedItem;
        item.setAmount(1);

        Entity baseEntity = location.getWorld().spawn(BlockHelpers.toCenterBlockLocation(location), entityClass, (entity) ->
                setEntityData(entity, yaw, item, facing));

        if (this.isModelEngine() && Bukkit.getPluginManager().isPluginEnabled("ModelEngine")) {
            spawnModelEngineFurniture(baseEntity, yaw);
        }

        return baseEntity;
    }

    private void setEntityData(Entity entity, float yaw, ItemStack item, BlockFace facing) {
        setBaseFurnitureData(entity);
        if (entity instanceof ItemFrame frame) {
            setFrameData(frame, item, yaw, facing);
            Location location = entity.getLocation();

            if (hasBarriers()) setBarrierHitbox(location, yaw, true);
            else {
                float width = hasHitbox() ? hitbox.width : 1f;
                float height = hasHitbox() ? hitbox.height : 1f;
                spawnInteractionEntity(entity, location, width, height, true);

                Block block = location.getBlock();
                if (hasSeat()) {
                    UUID entityId = spawnSeat(this, block, hasSeatYaw ? seatYaw : location.getYaw());
                    if (entityId != null) frame.getPersistentDataContainer().set(SEAT_KEY, DataType.UUID, entityId);
                }
                if (light != -1) {
                    WrappedLightAPI.createBlockLight(location, light);
                }
            }
        } else if (entity instanceof ItemDisplay itemDisplay) {
            setItemDisplayData(itemDisplay, item, yaw, displayEntityProperties);
            Location location = itemDisplay.getLocation();
            float width = hasHitbox() ? hitbox.width : displayEntityProperties.getDisplayWidth();
            float height = hasHitbox() ? hitbox.height : displayEntityProperties.getDisplayHeight();
            Interaction interaction = spawnInteractionEntity(itemDisplay, location, width, height, displayEntityProperties.isInteractable());

            if (hasBarriers()) setBarrierHitbox(location, yaw, false);
            else if (hasSeat()) {
                UUID entityId = spawnSeat(this, location.getBlock(), hasSeatYaw ? seatYaw : location.getYaw());
                if (entityId != null) {
                    if (interaction != null)
                        interaction.getPersistentDataContainer().set(SEAT_KEY, DataType.UUID, entityId);
                    itemDisplay.getPersistentDataContainer().set(SEAT_KEY, DataType.UUID, entityId);
                }
            }
        }
    }

    private Interaction spawnInteractionEntity(Entity entity, Location location, float width, float height, boolean responsive) {
        if (!OraxenPlugin.supportsDisplayEntities) return null;
        Interaction interaction = entity.getWorld().spawn(BlockHelpers.toCenterBlockLocation(location), Interaction.class, (Interaction i) -> {
            i.setInteractionWidth(width);
            i.setInteractionHeight(height);
            i.setResponsive(responsive);
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
        if (hasEvolution()) pdc.set(EVOLUTION_KEY, PersistentDataType.INTEGER, 0);
        if (isStorage() && getStorage().getStorageType() == StorageMechanic.StorageType.STORAGE) {
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
        else if (light != -1) itemDisplay.setBrightness(new Display.Brightness(light, 0));

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
        float alterYaw;
        if (VersionUtil.isSupportedVersionOrNewer(VersionUtil.v1_20_R1)) {
            pitch = isFixed && hasLimitedPlacing() && (limitedPlacing.isFloor() || limitedPlacing.isRoof()) ? -90 : 0;
            alterYaw = yaw;
        } else {
            pitch = isFixed && hasLimitedPlacing() ? limitedPlacing.isFloor() ? 90 : limitedPlacing.isWall() ? 0 : limitedPlacing.isRoof() ? -90 : 0 : 0;
            alterYaw = yaw - 180;
        }
        //TODO isWall will be put of the wall slightly. Fixing this is annoying as it is direction relative
        Location fixedLocation = !isFixed || !hasLimitedPlacing() || limitedPlacing.isWall()
                ? BlockHelpers.toCenterLocation(itemDisplay.getLocation())
                // Add .9 to raise the item up due to pitch change
                : limitedPlacing.isRoof() ? BlockHelpers.toCenterBlockLocation(itemDisplay.getLocation()).add(0, 0.9, 0)
                : BlockHelpers.toCenterBlockLocation(itemDisplay.getLocation());
        itemDisplay.teleport(fixedLocation);
        itemDisplay.setTransformation(transform);
        itemDisplay.setRotation(alterYaw, pitch);
    }

    private void setFrameData(ItemFrame frame, ItemStack item, float yaw, BlockFace facing) {
        frame.setVisible(false);
        frame.setItemDropChance(0);
        frame.setFacingDirection(facing, true);
        frame.setItem(item);
        frame.setRotation(yawToRotation(yaw));

        if (hasLimitedPlacing()) {
            if (limitedPlacing.isFloor() && !limitedPlacing.isWall() && frame.getLocation().getBlock().getRelative(BlockFace.DOWN).getType().isSolid()) {
                frame.setFacingDirection(BlockFace.UP, true);
            } else if (limitedPlacing.isWall()) {
                frame.setRotation(Rotation.NONE);
            }
        }
    }

    private void setBarrierHitbox(Location location, float yaw, boolean handleLight) {
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
            block.setType(Material.BARRIER);
            if (handleLight && light != -1)
                WrappedLightAPI.createBlockLight(barrierLocation, light);
        }
    }

    private void spawnModelEngineFurniture(Entity entity, float yaw) {
        ModeledEntity modelEntity = ModelEngineAPI.getOrCreateModeledEntity(entity);
        ActiveModel activeModel = ModelEngineAPI.createActiveModel(ModelEngineAPI.getBlueprint(getModelEngineID()));

        modelEntity.addModel(activeModel, false);
        modelEntity.setBaseEntityVisible(false);
        modelEntity.setModelRotationLock(true);

        if (OraxenPlugin.supportsDisplayEntities && entity instanceof ItemDisplay itemDisplay)
            itemDisplay.setItemStack(new ItemStack(Material.AIR));
        else if (entity instanceof ItemFrame itemFrame)
            itemFrame.setItem(new ItemStack(Material.AIR), false);
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
            default -> {}
        }
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
                if (seat != null) {
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
        if (hasSeat) {
            Entity stand = getSeat(baseEntity.getLocation());
            if (stand == null) stand = getSeat(baseEntity);
            if (stand != null) {
                stand.getPassengers().forEach(stand::removePassenger);
                stand.remove();
            }
        }

        if (OraxenPlugin.supportsDisplayEntities) {
            Interaction interaction = getInteractionEntity(baseEntity);
            if (interaction != null) interaction.remove();
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
        return (entity instanceof ItemFrame itemFrame) ? rotationToYaw(itemFrame.getRotation()) : entity.getLocation().getYaw();
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
            });
            return seat.getUniqueId();
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
        UUID baseEntityUUID = entity.getPersistentDataContainer().get(BASE_ENTITY_KEY, DataType.UUID);
        return baseEntityUUID != null && Bukkit.getEntity(baseEntityUUID) != null ? Bukkit.getEntity(baseEntityUUID) : getBaseEntityAlter(entity);
    }

    /**
     * Old method and inefficient method for getting the interaction entity. Kept for backwards compatibility.
     * When ran it will update the furniture to the new method without needing to replace it
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

    public static void rotateFurniture(Entity baseEntity) {
        float yaw = FurnitureMechanic.getFurnitureYaw(baseEntity);
        Rotation newRotation = FurnitureMechanic.yawToRotation(yaw).rotateClockwise();
        if (baseEntity instanceof ItemFrame frame) frame.setRotation(newRotation);
        else baseEntity.setRotation(FurnitureMechanic.rotationToYaw(newRotation), baseEntity.getLocation().getPitch());
    }
}
