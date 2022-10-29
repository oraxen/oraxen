package io.th0rgal.oraxen.api;

import io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.BlockLocation;
import io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.FurnitureFactory;
import io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.FurnitureMechanic;
import io.th0rgal.oraxen.utils.BlockHelpers;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import javax.annotation.Nullable;

import static io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.FurnitureMechanic.FURNITURE_KEY;

public class OraxenFurniture {

    /**
     * Check if a block is an instance of a Furniture
     *
     * @param block The block to check
     * @return true if the block is an instance of a Furniture, otherwise false
     */
    public static boolean isOraxenFurniture(Block block) {
        return block.getType() == Material.BARRIER && getFurnitureMechanic(block) != null;
    }
    /**
     * Check if an itemID has a FurnitureMechanic
     *
     * @param itemID The itemID to check
     * @return true if the itemID has a FurnitureMechanic, otherwise false
     */
    public static boolean isOraxenFurniture(String itemID) {
        return !FurnitureFactory.getInstance().isNotImplementedIn(itemID);
    }

    public static boolean place(Location location, String itemID, @Nullable Player player) {
        FurnitureMechanic mechanic = (FurnitureMechanic) FurnitureFactory.getInstance().getMechanic(itemID);
        if (mechanic == null) return false;
        return mechanic.place(location, player) != null;
    }

    public static boolean remove(Location location, @Nullable Player player) {
        Block block = location.getBlock();
        FurnitureMechanic mechanic = getFurnitureMechanic(block);
        ItemStack itemStack = player != null ? player.getInventory().getItemInMainHand() : new ItemStack(Material.AIR);
        if (mechanic == null) return false;

        ItemFrame itemFrame = mechanic.getItemFrame(block);

        if (mechanic.removeSolid(block) && (!mechanic.isStorage() || !mechanic.getStorage().isShulker())) {
            if (player != null && player.getGameMode() != GameMode.CREATIVE)
                mechanic.getDrop().furnitureSpawns(itemFrame, itemStack);
        }
        if (mechanic.hasBarriers())
            mechanic.removeSolid(itemFrame.getWorld(), new BlockLocation(itemFrame.getLocation()), mechanic.getYaw(itemFrame.getRotation()));
        else mechanic.removeAirFurniture(itemFrame);
        return true;
    }

    public static FurnitureMechanic getFurnitureMechanic(Block block) {
        if (block.getType() != Material.BARRIER) return null;
        final String mechanicID = BlockHelpers.getPDC(block).get(FURNITURE_KEY, PersistentDataType.STRING);
        return (FurnitureMechanic) FurnitureFactory.getInstance().getMechanic(mechanicID);
    }

    public static FurnitureMechanic getFurnitureMechanic(Entity entity) {
        if (!(entity instanceof ItemFrame)) return null;
        final String itemID = entity.getPersistentDataContainer().get(FURNITURE_KEY, PersistentDataType.STRING);
        if (!OraxenItems.exists(itemID)) return null;
        return (FurnitureMechanic) FurnitureFactory.getInstance().getMechanic(itemID);
    }
}
