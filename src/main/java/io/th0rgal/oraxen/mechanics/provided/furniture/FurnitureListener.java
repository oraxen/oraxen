package io.th0rgal.oraxen.mechanics.provided.furniture;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.items.OraxenItems;
import io.th0rgal.oraxen.mechanics.MechanicFactory;
import io.th0rgal.oraxen.utils.Utils;
import io.th0rgal.oraxen.utils.breaker.BreakerSystem;
import io.th0rgal.oraxen.utils.breaker.HardnessModifier;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import static io.th0rgal.oraxen.mechanics.provided.furniture.FurnitureMechanic.FURNITURE_KEY;

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
                    for (Entity entity : block.getWorld().getNearbyEntities(block.getLocation(), 1, 1, 1))
                        if (entity instanceof ItemFrame frame
                                && entity.getLocation().getBlockX() == block.getX()
                                && entity.getLocation().getBlockY() == block.getY()
                                && entity.getLocation().getBlockZ() == block.getZ()
                                && entity.getPersistentDataContainer().has(FURNITURE_KEY, PersistentDataType.STRING)) {
                            block.setType(Material.AIR);
                            FurnitureMechanic mechanic = (FurnitureMechanic) factory.getMechanic
                                    (entity.getPersistentDataContainer().get(FURNITURE_KEY, PersistentDataType.STRING));
                            mechanic.getDrop().spawns(block.getLocation(), tool);
                            frame.remove();
                            return;
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
        ItemFrame itemFrame = target.getWorld().spawn(target.getLocation(), ItemFrame.class, (ItemFrame frame) -> {
            frame.setVisible(false);
            frame.setFixed(true);
            frame.setPersistent(true);
            frame.setItemDropChance(0);
            frame.setItem(clone);
            frame.setRotation(mechanic.getRotation());
            frame.setFacingDirection(mechanic.getFacing());
            frame.getPersistentDataContainer().set(FURNITURE_KEY, PersistentDataType.STRING, itemID);
        });

        if (!player.getGameMode().equals(GameMode.CREATIVE))
            item.setAmount(item.getAmount() - 1);

        if (mechanic.hasBarrier()) target.setType(Material.BARRIER);
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

}
