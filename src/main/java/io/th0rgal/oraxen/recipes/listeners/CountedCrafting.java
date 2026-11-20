package io.th0rgal.oraxen.recipes.listeners;

import io.th0rgal.oraxen.recipes.RecipeCrafting;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;

final class CountedCrafting {
    private CountedCrafting() {}

    static void take(CraftItemEvent event, RecipeCrafting.Matcher matcher, RecipeCrafting.Match match, boolean allowShift,
                     BiFunction<RecipeCrafting.Match, ItemStack[], ItemStack> resultFactory) {
        // Vanilla takes one item per slot and can repeat internally on a shift-click.
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) return;
        boolean shift = event.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY;
        if (shift && !allowShift) return;
        ItemStack[] matrix = event.getInventory().getMatrix();
        boolean crafted = false;
        while (match != null) {
            ItemStack result;
            try {
                result = resultFactory.apply(match, matrix);
            } catch (IllegalArgumentException | IllegalStateException exception) {
                break;
            }
            if (empty(result) || !transfer(event, player, result)) break;
            consume(player, matrix, match.amounts());
            crafted = true;
            if (!shift) break;
            match = matcher.match(matrix);
        }
        if (crafted) event.getInventory().setMatrix(matrix);
    }

    private static boolean transfer(CraftItemEvent event, Player player, ItemStack result) {
        PlayerInventory inventory = player.getInventory();
        return switch (event.getAction()) {
            case MOVE_TO_OTHER_INVENTORY -> {
                if (!fits(inventory, result)) yield false;
                inventory.addItem(result.clone());
                yield true;
            }
            case PICKUP_ALL, PICKUP_HALF, PICKUP_ONE, PICKUP_SOME -> {
                ItemStack cursor = event.getCursor();
                if (!empty(cursor)) {
                    if (!cursor.isSimilar(result) || cursor.getAmount() + result.getAmount() > cursor.getMaxStackSize())
                        yield false;
                    result.setAmount(cursor.getAmount() + result.getAmount());
                }
                player.setItemOnCursor(result);
                yield true;
            }
            case HOTBAR_SWAP -> {
                int slot = event.getHotbarButton();
                if (slot == -1 && empty(inventory.getItemInOffHand())) inventory.setItemInOffHand(result);
                else if (slot >= 0 && slot < 9 && empty(inventory.getItem(slot))) inventory.setItem(slot, result);
                else yield false;
                yield true;
            }
            case DROP_ALL_SLOT, DROP_ONE_SLOT -> {
                drop(player, result);
                yield true;
            }
            default -> false;
        };
    }

    private static boolean fits(PlayerInventory inventory, ItemStack result) {
        int remaining = result.getAmount();
        for (ItemStack item : inventory.getStorageContents()) {
            if (empty(item)) remaining -= result.getMaxStackSize();
            else if (item.isSimilar(result)) remaining -= Math.max(0, item.getMaxStackSize() - item.getAmount());
            if (remaining <= 0) return true;
        }
        return false;
    }

    private static void consume(Player player, ItemStack[] matrix, int[] amounts) {
        List<ItemStack> overflow = new ArrayList<>();
        for (int slot = 0; slot < amounts.length; slot++) {
            int amount = amounts[slot];
            if (amount == 0) continue;
            ItemStack item = matrix[slot];
            int left = item.getAmount() - amount;
            matrix[slot] = left == 0 ? null : item.asQuantity(left);
            Material remainder = item.getType().getCraftingRemainingItem();
            if (remainder == null) continue;
            ItemStack returned = new ItemStack(remainder);
            if (matrix[slot] == null || matrix[slot].isSimilar(returned)) {
                int existing = matrix[slot] == null ? 0 : matrix[slot].getAmount();
                int inSlot = Math.min(amount, Math.max(0, returned.getMaxStackSize() - existing));
                if (inSlot > 0) matrix[slot] = returned.asQuantity(existing + inSlot);
                amount -= inSlot;
            }
            if (amount > 0) overflow.add(returned.asQuantity(amount));
        }
        if (!overflow.isEmpty())
            for (ItemStack item : player.getInventory().addItem(overflow.toArray(ItemStack[]::new)).values())
                drop(player, item);
    }

    private static void drop(Player player, ItemStack result) {
        var location = player.getEyeLocation();
        var item = player.getWorld().dropItem(location, result);
        item.setVelocity(location.getDirection().multiply(0.3));
        item.setPickupDelay(40);
        item.setThrower(player.getUniqueId());
    }

    static void consumeCrafterExtras(Inventory inventory, ItemStack[] matrix, RecipeCrafting.Match match) {
        // Let the crafter handle the last item and its remainder through the vanilla path.
        var location = inventory.getLocation();
        for (int slot = 0; slot < matrix.length; slot++) {
            int extra = match.amounts()[slot] - 1;
            if (extra <= 0) continue;
            ItemStack item = matrix[slot];
            inventory.setItem(slot, item.asQuantity(item.getAmount() - extra));
            Material remainder = item.getType().getCraftingRemainingItem();
            if (remainder != null && location != null && location.getWorld() != null)
                location.getWorld().dropItemNaturally(location, new ItemStack(remainder, extra));
        }
    }

    private static boolean empty(ItemStack stack) {
        return stack == null || stack.isEmpty();
    }
}
