package io.th0rgal.oraxen.mechanics.provided.gameplay.furniture;

import com.jeff_media.customblockdata.CustomBlockData;
import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.compatibilities.CompatibilitiesManager;
import io.th0rgal.oraxen.compatibilities.provided.lightapi.WrappedLightAPI;
import io.th0rgal.oraxen.items.OraxenItems;
import io.th0rgal.oraxen.mechanics.Mechanic;
import io.th0rgal.oraxen.mechanics.MechanicFactory;
import io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.evolution.EvolvingFurniture;
import io.th0rgal.oraxen.utils.BlockHelpers;
import io.th0rgal.oraxen.utils.actions.ClickAction;
import io.th0rgal.oraxen.utils.drops.Drop;
import io.th0rgal.oraxen.utils.drops.Loot;
import io.th0rgal.oraxen.utils.limitedplacing.LimitedPlacing;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;

import static io.th0rgal.oraxen.utils.BlockHelpers.VANILLA_STONE_BREAK;
import static io.th0rgal.oraxen.utils.BlockHelpers.VANILLA_STONE_PLACE;

public class FurnitureMechanic extends Mechanic {

    public static final NamespacedKey FURNITURE_KEY = new NamespacedKey(OraxenPlugin.get(), "furniture");
    public static final NamespacedKey SEAT_KEY = new NamespacedKey(OraxenPlugin.get(), "seat");
    public static final NamespacedKey ROOT_KEY = new NamespacedKey(OraxenPlugin.get(), "root");
    public static final NamespacedKey ORIENTATION_KEY = new NamespacedKey(OraxenPlugin.get(), "orientation");
    public static final NamespacedKey EVOLUTION_KEY = new NamespacedKey(OraxenPlugin.get(), "evolution");
    private final LimitedPlacing limitedPlacing;
    public final boolean farmlandRequired;
    public final boolean farmblockRequired;
    private final String breakSound;
    private final String placeSound;
    private final String stepSound;
    private final String hitSound;
    private final String fallSound;
    private final List<BlockLocation> barriers;
    private final boolean hasRotation;
    private final boolean hasSeat;
    private boolean hasSeatYaw;
    private final BlockFace facing;
    private final Drop drop;
    private final EvolvingFurniture evolvingFurniture;
    private final int light;
    private String placedItemId;
    private ItemStack placedItem;
    private Rotation rotation;
    private float seatHeight;
    private float seatYaw;
    private final List<ClickAction> clickActions;

    @SuppressWarnings("unchecked")
    public FurnitureMechanic(MechanicFactory mechanicFactory, ConfigurationSection section) {
        super(mechanicFactory, section, itemBuilder -> itemBuilder.setCustomTag(FURNITURE_KEY,
                PersistentDataType.BYTE, (byte) 1));

        placeSound = section.getString("place_sound", null);
        breakSound = section.getString("break_sound", null);
        stepSound = section.getString("step_sound", null);
        hitSound = section.getString("hit_sound", null);
        fallSound = section.getString("fall_sound", null);

        if (section.isString("item"))
            placedItemId = section.getString("item");

        barriers = new ArrayList<>();
        if (CompatibilitiesManager.hasPlugin("ProtocolLib") && section.getBoolean("barrier", false))
            barriers.add(new BlockLocation(0, 0, 0));
        if (CompatibilitiesManager.hasPlugin("ProtocolLib") && section.isList("barriers"))
            for (Object barrierObject : section.getList("barriers"))
                barriers.add(new BlockLocation((Map<String, Object>) barrierObject));

        if (section.isConfigurationSection("seat")) {
            ConfigurationSection seatSection = section.getConfigurationSection("seat");
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

        if (section.isConfigurationSection("evolution")) {
            evolvingFurniture = new EvolvingFurniture(getItemID(), section.getConfigurationSection("evolution"));
            ((FurnitureFactory) getFactory()).registerEvolution();
        } else evolvingFurniture = null;

        if (section.isString("rotation")) {
            rotation = Rotation.valueOf(section.getString("rotation", "NONE").toUpperCase());
            hasRotation = true;
        } else
            hasRotation = false;

        light = section.getInt("light", -1);

        farmlandRequired = section.getBoolean("farmland_required", false);
        farmblockRequired = section.getBoolean("farmblock_required", false);

        facing = section.isString("facing")
                ? BlockFace.valueOf(section.getString("facing").toUpperCase())
                : null;

        List<Loot> loots = new ArrayList<>();
        if (section.isConfigurationSection("drop")) {
            ConfigurationSection drop = section.getConfigurationSection("drop");
            for (LinkedHashMap<String, Object> lootConfig : (List<LinkedHashMap<String, Object>>)
                    drop.getList("loots"))
                loots.add(new Loot(lootConfig));

            if (drop.isString("minimal_type")) {
                FurnitureFactory mechanic = (FurnitureFactory) mechanicFactory;
                List<String> bestTools = drop.isList("best_tools")
                        ? drop.getStringList("best_tools")
                        : new ArrayList<>();
                this.drop = new Drop(mechanic.toolTypes, loots, drop.getBoolean("silktouch"),
                        drop.getBoolean("fortune"), getItemID(),
                        drop.getString("minimal_type"),
                        bestTools);
            } else
                this.drop = new Drop(loots, drop.getBoolean("silktouch"), drop.getBoolean("fortune"),
                        getItemID());
        } else
            drop = new Drop(loots, false, false, getItemID());

        if (section.isConfigurationSection("limited_placing")) {
            limitedPlacing = new LimitedPlacing(section.getConfigurationSection("limited_placing"));
        } else limitedPlacing = null;

        clickActions = ClickAction.parseList(section);
    }

    public static ArmorStand getSeat(Location location) {
        Location seatLoc = location.clone().add(0.5, 0.0, 0.5);
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

    public boolean hasBreakSound() {
        return breakSound != null;
    }

    public String getBreakSound() {
        return validateReplacedSounds(breakSound);
    }

    public boolean hasPlaceSound() {
        return placeSound != null;
    }

    public String getPlaceSound() {
        return validateReplacedSounds(placeSound);
    }

    public boolean hasStepSound() {
        return stepSound != null;
    }

    public String getStepSound() {
        return validateReplacedSounds(stepSound);
    }

    public boolean hasHitSound() {
        return hitSound != null;
    }

    public String getHitSound() {
        return validateReplacedSounds(hitSound);
    }

    public boolean hasFallSound() {
        return fallSound != null;
    }

    public String getFallSound() {
        return validateReplacedSounds(fallSound);
    }

    private String validateReplacedSounds(String sound) {
        if (sound.startsWith("block.wood"))
            return sound.replace("block.wood", "required.wood.");
        else if (sound.startsWith("block.stone"))
            return sound.replace("block.stone", "required.stone.");
        else return sound;
    }

    public boolean hasLimitedPlacing() { return limitedPlacing != null; }
    public LimitedPlacing getLimitedPlacing() { return limitedPlacing; }

    public boolean hasBarriers() {
        return !barriers.isEmpty();
    }

    public List<BlockLocation> getBarriers() {
        return barriers;
    }

    public boolean hasRotation() {
        return hasRotation;
    }

    public Rotation getRotation() {
        return rotation;
    }

    public boolean hasSeat() {
        return hasSeat;
    }

    public float getSeatHeight() {
        return seatHeight;
    }

    public float getSeatYaw() {
        return seatYaw;
    }

    public boolean hasFacing() {
        return facing != null;
    }

    public BlockFace getFacing() {
        return facing;
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
            meta.setDisplayName("");
            placedItem.setItemMeta(meta);
        }
    }

    public ItemFrame place(Rotation rotation, float yaw, BlockFace facing, Location location) {
        setPlacedItem();
        return place(rotation, yaw, facing, location, placedItem);
    }

    public ItemFrame place(Rotation rotation, float yaw, BlockFace facing, Location location, ItemStack item) {
        if (!this.isEnoughSpace(yaw, location)) return null;
        if (!location.isWorldLoaded()) return null;

        setPlacedItem();
        ItemFrame output = location.getWorld().spawn(location, ItemFrame.class, (ItemFrame frame) -> {
            frame.setVisible(false);
            frame.setFixed(false);
            frame.setPersistent(true);
            frame.setItemDropChance(0);
            if (evolvingFurniture == null) {
                ItemStack clone = item.clone();
                ItemMeta meta = clone.getItemMeta();
                meta.setDisplayName("");
                clone.setItemMeta(meta);
                frame.setItem(clone);
            } else
                frame.setItem(placedItem, false);
            frame.setRotation(rotation);
            frame.setFacingDirection(hasFacing() ? getFacing() : facing, true);
            frame.getPersistentDataContainer().set(FURNITURE_KEY, PersistentDataType.STRING, getItemID());
            if (hasEvolution())
                frame.getPersistentDataContainer().set(EVOLUTION_KEY, PersistentDataType.INTEGER, 0);
        });

        if (hasBarriers())
            for (Location barrierLocation : getLocations(yaw, location, getBarriers())) {
                Block block = barrierLocation.getBlock();
                PersistentDataContainer data = new CustomBlockData(block, OraxenPlugin.get());
                data.set(FURNITURE_KEY, PersistentDataType.STRING, getItemID());
                if (hasSeat()) {
                    String entityId = spawnSeat(this, block, hasSeatYaw ? seatYaw : yaw);
                    data.set(SEAT_KEY, PersistentDataType.STRING, entityId);
                }
                data.set(ROOT_KEY, PersistentDataType.STRING, new BlockLocation(location).toString());
                data.set(ORIENTATION_KEY, PersistentDataType.FLOAT, yaw);
                block.setType(Material.BARRIER, false);
                if (light != -1)
                    WrappedLightAPI.createBlockLight(barrierLocation, light);
            }
        else if (light != -1)
            WrappedLightAPI.createBlockLight(location, light);

        BlockHelpers.playCustomBlockSound(location, hasPlaceSound() ? getPlaceSound() : VANILLA_STONE_PLACE);
        return output;
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
            location.getBlock().setType(Material.AIR);
            new CustomBlockData(location.getBlock(), OraxenPlugin.get()).clear();
        }

        boolean removed = false;
        for (Entity entity : rootLocation.getWorld().getNearbyEntities(rootLocation, 1, 1, 1)) {
            PersistentDataContainer pdc = entity.getPersistentDataContainer();
            if (entity instanceof ItemFrame frame
                    && entity.getLocation().getBlockX() == rootLocation.getX()
                    && entity.getLocation().getBlockY() == rootLocation.getY()
                    && entity.getLocation().getBlockZ() == rootLocation.getZ()
                    && pdc.has(FURNITURE_KEY, PersistentDataType.STRING)) {
                if (pdc.has(SEAT_KEY, PersistentDataType.STRING)) {
                    Entity seat = Bukkit.getEntity(UUID.fromString(pdc.get(SEAT_KEY, PersistentDataType.STRING)));
                    if (seat != null) {
                        seat.getPassengers().clear();
                        seat.remove();
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

        BlockHelpers.playCustomBlockSound(rootLocation, hasBreakSound() ? getBreakSound() : VANILLA_STONE_BREAK);
        return removed;
    }

    public void removeAirFurniture(ItemFrame frame) {
        PersistentDataContainer framePDC = frame.getPersistentDataContainer();
        if (framePDC.has(SEAT_KEY, PersistentDataType.STRING)) {
            Entity stand = Bukkit.getEntity(UUID.fromString(framePDC.get(SEAT_KEY, PersistentDataType.STRING)));
            if (stand != null) {
                stand.getPassengers().forEach(stand::removePassenger);
                stand.remove();
            }
        }
        Location location = frame.getLocation().getBlock().getLocation();
        if (light != -1) {
            WrappedLightAPI.removeBlockLight(location);
        }
        frame.remove();
        if (hasBreakSound())
            BlockHelpers.playCustomBlockSound(location, getBreakSound());
    }

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

    public boolean isEnoughSpace(float yaw, Location rootLocation) {
        if (!hasBarriers())
            return true;
        return getLocations(yaw, rootLocation, getBarriers()).stream()
                .allMatch(sideLocation -> sideLocation.getBlock().getType().isAir());
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
                stand.getPersistentDataContainer().set(FURNITURE_KEY, PersistentDataType.STRING, mechanic.getItemID());
                stand.getPersistentDataContainer().set(SEAT_KEY, PersistentDataType.STRING, stand.getUniqueId().toString());
            });
            return seat.getUniqueId().toString();
        }
        return null;
    }

    public ItemFrame getItemFrame(Block block, BlockLocation blockLocation, Float orientation) {
        if (hasBarriers()) {
            for (Location sideLocation : getLocations(orientation, blockLocation.toLocation(block.getWorld()), getBarriers())) {
                for (Entity entity : sideLocation.getWorld().getNearbyEntities(sideLocation, 1, 1, 1))
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
}
