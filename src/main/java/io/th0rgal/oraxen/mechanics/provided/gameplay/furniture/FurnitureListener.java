package io.th0rgal.oraxen.mechanics.provided.gameplay.furniture;

import de.jeff_media.customblockdata.CustomBlockData;
import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.items.OraxenItems;
import io.th0rgal.oraxen.mechanics.MechanicFactory;
import io.th0rgal.oraxen.utils.Utils;
import io.th0rgal.oraxen.utils.breaker.BreakerSystem;
import io.th0rgal.oraxen.utils.breaker.HardnessModifier;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.FurnitureMechanic.*;

public class FurnitureListener implements Listener {

    private final MechanicFactory factory;

    public FurnitureListener(MechanicFactory factory) {
        this.factory = factory;
        BreakerSystem.MODIFIERS.add(getHardnessModifier());
    }

    private HardnessModifier getHardnessModifier() {
        return new HardnessModifier() {

            @Override
            public boolean isTriggered(Player player, Block block, ItemStack tool) {
                return block.getType() == Material.BARRIER;
            }

            @Override
            public void breakBlock(Player player, Block block, ItemStack tool) {
                Bukkit.getScheduler().runTask(OraxenPlugin.get(), () -> {

                    final PersistentDataContainer customBlockData = new CustomBlockData(block, OraxenPlugin.get());
                    if (!customBlockData.has(FURNITURE_KEY, PersistentDataType.STRING))
                        return;
                    String mechanicID = customBlockData.get(FURNITURE_KEY, PersistentDataType.STRING);
                    FurnitureMechanic mechanic = (FurnitureMechanic) factory.getMechanic(mechanicID);
                    BlockLocation rootBlockLocation = new BlockLocation(customBlockData.get(ROOT_KEY,
                            PersistentDataType.STRING));
                    Location rootLocation = rootBlockLocation.toLocation(block.getWorld());

                    for (Location location : getLocations(customBlockData
                                    .get(ORIENTATION_KEY, PersistentDataType.FLOAT),
                            rootLocation,
                            mechanic.getBarriers())) {
                        location.getBlock().setType(Material.AIR);
                    }

                    for (Entity entity : rootLocation.getWorld().getNearbyEntities(rootLocation, 1, 1, 1))
                        if (entity instanceof ItemFrame frame
                                && entity.getLocation().getBlockX() == rootLocation.getX()
                                && entity.getLocation().getBlockY() == rootLocation.getY()
                                && entity.getLocation().getBlockZ() == rootLocation.getZ()
                                && entity.getPersistentDataContainer().has(FURNITURE_KEY, PersistentDataType.STRING)) {
                            if (entity.getPersistentDataContainer().has(SEAT_KEY, PersistentDataType.STRING)) {
                                Entity stand = Bukkit.getEntity(UUID.fromString(entity.getPersistentDataContainer()
                                        .get(SEAT_KEY, PersistentDataType.STRING)));
                                for (Entity passenger : stand.getPassengers())
                                    stand.removePassenger(passenger);
                                stand.remove();
                            }
                            frame.remove();
                            rootLocation.getBlock().setType(Material.AIR);
                            mechanic.getDrop().spawns(block.getLocation(), tool);
                        }
                });
            }

            @Override
            public long getPeriod(Player player, Block block, ItemStack tool) {
                return 1;
            }
        };
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onHangingPlaceEvent(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK)
            return;

        ItemStack item = event.getItem();
        String itemID = OraxenItems.getIdByItem(item);
        if (factory.isNotImplementedIn(itemID))
            return;
        Player player = event.getPlayer();
        Block placedAgainst = event.getClickedBlock();
        Block target;
        Material type = placedAgainst.getType();
        if (Utils.REPLACEABLE_BLOCKS.contains(type))
            target = placedAgainst;
        else {
            target = placedAgainst.getRelative(event.getBlockFace());
            if (target.getType() != Material.AIR && target.getType() != Material.WATER
                    && target.getType() != Material.CAVE_AIR)
                return;
        }
        if (isStandingInside(player, target))
            return;
        for (Entity entity : target.getWorld().getNearbyEntities(target.getLocation(), 1, 1, 1))
            if (entity instanceof ItemFrame
                    && entity.getLocation().getBlockX() == target.getX()
                    && entity.getLocation().getBlockY() == target.getY()
                    && entity.getLocation().getBlockZ() == target.getZ())
                return;

        BlockData curentBlockData = target.getBlockData();
        FurnitureMechanic mechanic = (FurnitureMechanic) factory.getMechanic(itemID);
        target.setType(Material.AIR);
        BlockState currentBlockState = target.getState();

        BlockPlaceEvent blockPlaceEvent = new BlockPlaceEvent(target, currentBlockState, placedAgainst, item, player,
                true, event.getHand());
        Bukkit.getPluginManager().callEvent(blockPlaceEvent);
        if (!blockPlaceEvent.canBuild() || blockPlaceEvent.isCancelled()) {
            target.setBlockData(curentBlockData, false); // false to cancel physic
            return;
        }
        ItemStack clone = item.clone();
        ItemMeta meta = clone.getItemMeta();
        meta.setDisplayName("");
        clone.setItemMeta(meta);
        Rotation rotation = mechanic.hasRotation()
                ? mechanic.getRotation()
                : getRotation(player.getEyeLocation().getYaw(),
                mechanic.hasBarriers()
                        && mechanic.getBarriers().size() > 1);
        float yaw = getYaw(rotation) + mechanic.getSeatYaw();
        String entityId;
        if (mechanic.hasSeat()) {
            ArmorStand seat = target.getWorld().spawn(target.getLocation()
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
                stand.getPersistentDataContainer().set(FURNITURE_KEY, PersistentDataType.STRING, itemID);
            });
            entityId = seat.getUniqueId().toString();
        } else entityId = null;

        ItemFrame itemFrame = target.getWorld().spawn(target.getLocation(), ItemFrame.class, (ItemFrame frame) -> {
            frame.setVisible(false);
            frame.setFixed(true);
            frame.setPersistent(true);
            frame.setItemDropChance(0);
            frame.setItem(clone);
            frame.setRotation(rotation);
            frame.setFacingDirection(mechanic.getFacing());
            frame.getPersistentDataContainer().set(FURNITURE_KEY, PersistentDataType.STRING, itemID);
            if (mechanic.hasSeat())
                frame.getPersistentDataContainer().set(SEAT_KEY, PersistentDataType.STRING, entityId);
        });


        if (!player.getGameMode().equals(GameMode.CREATIVE))
            item.setAmount(item.getAmount() - 1);

        if (mechanic.hasBarriers())
            for (Location location : getLocations(yaw, target.getLocation(), mechanic.getBarriers())) {
                Block block = location.getBlock();
                PersistentDataContainer data = new CustomBlockData(block, OraxenPlugin.get());
                data.set(FURNITURE_KEY, PersistentDataType.STRING, itemID);
                if (mechanic.hasSeat())
                    data.set(SEAT_KEY, PersistentDataType.STRING, entityId);
                data.set(ROOT_KEY, PersistentDataType.STRING, new BlockLocation(target.getLocation()).toString());
                data.set(ORIENTATION_KEY, PersistentDataType.FLOAT, yaw);
                block.setType(Material.BARRIER);
            }
    }

    private Rotation getRotation(double yaw, boolean restricted) {
        int id = (int) (((Location.normalizeYaw((float) yaw) + 180) * 8 / 360) + 0.5) % 8;
        if (restricted && id % 2 != 0)
            id -= 1;
        return Rotation.values()[id];
    }

    private float getYaw(Rotation rotation) {
        return (Arrays.asList(Rotation.values()).indexOf(rotation) * 360f) / 8f;
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerInteractWithItemFrame(PlayerInteractEntityEvent event) {
        if (event.getRightClicked() instanceof ItemFrame itemFrame) {
            PersistentDataContainer container = itemFrame.getPersistentDataContainer();
            if (container.has(FURNITURE_KEY, PersistentDataType.STRING)) {
                String itemID = container.get(FURNITURE_KEY, PersistentDataType.STRING);
                if (!OraxenItems.exists(itemID))
                    return;
                destroy(itemFrame, itemID, event.getPlayer());
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onFurnitureBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        if (block.getType() != Material.BARRIER || event.getPlayer().getGameMode() != GameMode.CREATIVE)
            return;

        for (Entity entity : block.getWorld().getNearbyEntities(block.getLocation(), 1, 1, 1))
            if (entity instanceof ItemFrame frame
                    && entity.getLocation().getBlockX() == block.getX()
                    && entity.getLocation().getBlockY() == block.getY()
                    && entity.getLocation().getBlockZ() == block.getZ()
                    && entity.getPersistentDataContainer().has(FURNITURE_KEY, PersistentDataType.STRING)) {
                frame.remove();
                if (entity.getPersistentDataContainer().has(SEAT_KEY, PersistentDataType.STRING)) {
                    Entity stand = Bukkit.getEntity(UUID.fromString(entity.getPersistentDataContainer()
                            .get(SEAT_KEY, PersistentDataType.STRING)));
                    stand.remove();
                }
                return;
            }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerClickOnFurniture(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getClickedBlock() == null)
            return;
        Block block = event.getClickedBlock();
        if (block.getType() != Material.BARRIER || event.getPlayer().isSneaking())
            return;
        for (Entity entity : block.getWorld().getNearbyEntities(block.getLocation(), 1, 1, 1))
            if (entity instanceof ItemFrame frame
                    && entity.getLocation().getBlockX() == block.getX()
                    && entity.getLocation().getBlockY() == block.getY()
                    && entity.getLocation().getBlockZ() == block.getZ()
                    && entity.getPersistentDataContainer().has(SEAT_KEY, PersistentDataType.STRING)) {
                String entityId = entity.getPersistentDataContainer().get(SEAT_KEY, PersistentDataType.STRING);
                Entity stand = Bukkit.getEntity(UUID.fromString(entityId));
                if (stand.getPassengers().size() == 0) {
                    stand.addPassenger(event.getPlayer());
                    event.setCancelled(true);
                }
                return;
            }
    }

    private void destroy(ItemFrame itemFrame, String itemID, Player player) {
        itemFrame.remove();
    }

    private boolean isStandingInside(Player player, Block block) {
        Location playerLocation = player.getLocation();
        Location blockLocation = block.getLocation();
        return playerLocation.getBlockX() == blockLocation.getBlockX()
                && (playerLocation.getBlockY() == blockLocation.getBlockY()
                || playerLocation.getBlockY() + 1 == blockLocation.getBlockY())
                && playerLocation.getBlockZ() == blockLocation.getBlockZ();
    }

    private List<Location> getLocations(float rotation, Location center, List<BlockLocation> relativeCoordinates) {
        List<Location> output = new ArrayList<>();
        for (BlockLocation modifier : relativeCoordinates)
            output.add(modifier.groundRotate(rotation).add(center));
        return output;
    }

}
