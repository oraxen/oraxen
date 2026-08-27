package io.th0rgal.oraxen.packets;

import io.th0rgal.oraxen.api.OraxenFurniture;
import io.th0rgal.oraxen.api.OraxenItems;
import io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.FurnitureMechanic;
import org.bukkit.GameMode;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.jetbrains.annotations.Nullable;

/**
 * Handles creative middle-click (pick block / pick entity) for Oraxen furniture.
 * <p>
 * Since MC 1.21.4, picking is handled server-side via {@code ServerboundPickItemFromBlockPacket} /
 * {@code ServerboundPickItemFromEntityPacket} and no longer fires {@code InventoryCreativeEvent},
 * so the item a player gets is resolved from the vanilla block/entity (a barrier for furniture
 * hitboxes). This intercepts those packets and substitutes the real Oraxen furniture item.
 */
public final class PickItemHandler {

    private PickItemHandler() {
    }

    /**
     * Handles a pick-block on a block. If the block is an Oraxen furniture hitbox,
     * gives the player the actual Oraxen furniture item.
     *
     * @return true if the block was Oraxen furniture and the item was given
     */
    public static boolean handleBlockPick(Player player, Block block) {
        FurnitureMechanic mechanic = OraxenFurniture.getFurnitureMechanic(block);
        if (mechanic == null) return false;
        tryPickItem(player, OraxenItems.getItemById(mechanic.getItemID()).build());
        return true;
    }

    /**
     * Handles a pick-entity on an entity. If the entity is Oraxen furniture,
     * gives the player the actual Oraxen furniture item.
     *
     * @return true if the entity was Oraxen furniture and the item was given
     */
    public static boolean handleEntityPick(Player player, Entity entity) {
        FurnitureMechanic mechanic = OraxenFurniture.getFurnitureMechanic(entity);
        if (mechanic == null) return false;
        tryPickItem(player, OraxenItems.getItemById(mechanic.getItemID()).build());
        return true;
    }

    @Nullable
    public static Entity getEntityById(World world, int entityId) {
        for (Entity entity : world.getEntities()) {
            if (entity.getEntityId() == entityId) return entity;
        }
        return null;
    }

    /**
     * Replicates what vanilla would give for a non-Oraxen block pick. The pick packet is always
     * cancelled up-front (we must not touch the world on the Netty thread), so the vanilla item
     * has to be handed out manually for blocks that are not Oraxen furniture.
     */
    public static void pickBlockFallback(Player player, Block block) {
        tryPickItem(player, new ItemStack(block.getType()));
    }

    /**
     * Mimics vanilla {@code Inventory#tryPickItem}: selects a matching slot if the player already
     * owns the item, otherwise adds it (creative only) and selects it.
     */
    public static void tryPickItem(Player player, ItemStack item) {
        if (item == null || item.getType().isAir()) return;
        PlayerInventory inventory = player.getInventory();
        int slot = findSlotMatchingItem(inventory, item);
        if (slot >= 0) {
            if (slot < 9) {
                player.getInventory().setHeldItemSlot(slot);
            } else {
                // Mimic vanilla Inventory#pickSlot: swap the item into the selected hotbar slot
                int selected = player.getInventory().getHeldItemSlot();
                ItemStack previous = inventory.getItem(selected);
                inventory.setItem(selected, item);
                inventory.setItem(slot, previous);
            }
        } else if (player.getGameMode() == GameMode.CREATIVE) {
            // Mimic vanilla Inventory#addAndPickItem
            int free = firstEmptySlot(inventory);
            if (free >= 0) {
                inventory.setItem(free, item);
                if (free < 9) player.getInventory().setHeldItemSlot(free);
            }
        }
        player.updateInventory();
    }

    private static int findSlotMatchingItem(PlayerInventory inventory, ItemStack item) {
        for (int i = 0; i < 36; i++) {
            ItemStack candidate = inventory.getItem(i);
            if (candidate != null && candidate.isSimilar(item)) return i;
        }
        return -1;
    }

    private static int firstEmptySlot(PlayerInventory inventory) {
        for (int i = 0; i < 36; i++) {
            ItemStack candidate = inventory.getItem(i);
            if (candidate == null || candidate.getType().isAir()) return i;
        }
        return -1;
    }
}
