package io.th0rgal.oraxen.recipes.listeners;

import io.th0rgal.oraxen.recipes.CustomWorkstationRecipe;
import io.th0rgal.oraxen.recipes.CustomWorkstationRegistry;
import io.th0rgal.oraxen.recipes.builders.AnvilBuilder;
import io.th0rgal.oraxen.recipes.builders.GrindstoneBuilder;
import io.th0rgal.oraxen.utils.InventoryUtils;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.Levelled;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.event.inventory.PrepareGrindstoneEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.AnvilInventory;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.GrindstoneInventory;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.Map;

public class CustomWorkstationEvents implements Listener {

    @EventHandler(priority = EventPriority.HIGHEST)
    public void prepareAnvil(PrepareAnvilEvent event) {
        if (RecipesBuilderEvents.isBuilderInventory(event, AnvilBuilder.class)) {
            event.setResult(null);
            return;
        }

        Player player = InventoryUtils.playerFromView(event);
        CustomWorkstationRecipe recipe = CustomWorkstationRegistry.match(CustomWorkstationRecipe.Type.ANVIL,
                event.getInventory().getItem(0), event.getInventory().getItem(1));
        if (recipe == null) return;
        if (!permitted(recipe, player)) {
            event.setResult(null);
            return;
        }
        event.setResult(recipe.createResult());
        event.getInventory().setRepairCost(recipe.value());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void prepareGrindstone(PrepareGrindstoneEvent event) {
        if (RecipesBuilderEvents.isBuilderInventory(event, GrindstoneBuilder.class)) {
            event.setResult(null);
            return;
        }

        Player player = InventoryUtils.playerFromView(event);
        Match match = grindstoneRecipe(event.getInventory().getItem(0), event.getInventory().getItem(1));
        if (match == null) return;
        event.setResult(permitted(match.recipe(), player) ? match.recipe().createResult() : null);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void takeResult(InventoryClickEvent event) {
        // A double-click collect can pull the result slot from anywhere in the view, so it must be
        // handled regardless of which raw slot received the click.
        boolean collectToCursor = event.getAction() == InventoryAction.COLLECT_TO_CURSOR;
        if ((event.getRawSlot() != 2 && !collectToCursor) || !(event.getWhoClicked() instanceof Player player)) return;
        Inventory inventory = event.getInventory();
        Match match;
        boolean grindstone = inventory instanceof GrindstoneInventory;
        if (inventory instanceof AnvilInventory)
            match = match(CustomWorkstationRegistry.match(CustomWorkstationRecipe.Type.ANVIL,
                    inventory.getItem(0), inventory.getItem(1)), 0, 1);
        else if (grindstone) match = grindstoneRecipe(inventory.getItem(0), inventory.getItem(1));
        else return;
        if (match == null) return;

        if (collectToCursor) {
            collectResultToCursor(event, player, inventory, match, grindstone);
            return;
        }

        event.setCancelled(true); // A structural match is ours, including stale outputs and permission changes.
        CustomWorkstationRecipe recipe = match.recipe();
        if (!permitted(recipe, player) || !supportedAction(event.getAction())) return;
        ItemStack resultSlot = event.getCurrentItem();
        ItemStack expected = recipe.createResult();
        if (resultSlot == null || resultSlot.isEmpty() || !resultSlot.isSimilar(expected)
                || resultSlot.getAmount() != expected.getAmount()) return;
        if (!grindstone && player.getGameMode() != GameMode.CREATIVE && player.getLevel() < recipe.value()) return;

        InventoryAction action = event.getAction();
        if (action == InventoryAction.MOVE_TO_OTHER_INVENTORY) {
            if (!giveToInventory(player.getInventory(), expected)) return;
        } else if (action == InventoryAction.HOTBAR_SWAP) {
            if (!giveToHotbar(player.getInventory(), event.getHotbarButton(), expected)) return;
        } else if (action == InventoryAction.DROP_ALL_SLOT || action == InventoryAction.DROP_ONE_SLOT) {
            dropResult(player, expected);
        } else {
            ItemStack cursor = event.getCursor();
            if (!cursor.isEmpty() && (!cursor.isSimilar(expected)
                    || cursor.getAmount() + expected.getAmount() > cursor.getMaxStackSize())) return;
            ItemStack transferred = cursor.isEmpty() ? expected : cursor.clone();
            if (!cursor.isEmpty()) transferred.setAmount(cursor.getAmount() + expected.getAmount());
            player.setItemOnCursor(transferred);
        }
        consume(inventory, match.baseSlot(), recipe.base().amount());
        if (recipe.addition() != null) consume(inventory, match.additionSlot(), recipe.addition().amount());
        if (player.getGameMode() != GameMode.CREATIVE && !grindstone) player.setLevel(player.getLevel() - recipe.value());
        if (grindstone && recipe.value() > 0) player.giveExp(recipe.value());
        inventory.setItem(2, null);
    }

    /**
     * Handles COLLECT_TO_CURSOR (double-click) for custom workstation results. Vanilla's collect
     * pulls a matching result stack through the vanilla take path, which ignores the configured
     * ingredient amounts and (for grindstones) pops vanilla XP. Only intervene when the cursor
     * would actually collect our result; unrelated double-click collection stays vanilla.
     */
    private void collectResultToCursor(InventoryClickEvent event, Player player, Inventory inventory,
                                       Match match, boolean grindstone) {
        ItemStack cursor = event.getCursor();
        ItemStack resultSlot = inventory.getItem(2);
        if (cursor.isEmpty() || resultSlot == null || resultSlot.isEmpty() || !cursor.isSimilar(resultSlot)) return;

        event.setCancelled(true); // Vanilla would collect our result with its own consumption rules.
        CustomWorkstationRecipe recipe = match.recipe();
        if (!permitted(recipe, player)) return;
        ItemStack expected = recipe.createResult();
        if (!resultSlot.isSimilar(expected) || resultSlot.getAmount() != expected.getAmount()) return;
        if (!grindstone && player.getGameMode() != GameMode.CREATIVE && player.getLevel() < recipe.value()) return;
        // No partial takes: the whole result must fit on the cursor, mirroring the pickup path.
        if (cursor.getAmount() + expected.getAmount() > cursor.getMaxStackSize()) return;

        ItemStack transferred = cursor.clone();
        transferred.setAmount(cursor.getAmount() + expected.getAmount());
        player.setItemOnCursor(transferred);
        consume(inventory, match.baseSlot(), recipe.base().amount());
        if (recipe.addition() != null) consume(inventory, match.additionSlot(), recipe.addition().amount());
        if (player.getGameMode() != GameMode.CREATIVE && !grindstone) player.setLevel(player.getLevel() - recipe.value());
        if (grindstone && recipe.value() > 0) player.giveExp(recipe.value());
        inventory.setItem(2, null);
    }

    private boolean supportedAction(InventoryAction action) {
        return action == InventoryAction.MOVE_TO_OTHER_INVENTORY || action == InventoryAction.PICKUP_ALL
                || action == InventoryAction.PICKUP_HALF || action == InventoryAction.PICKUP_ONE
                || action == InventoryAction.PICKUP_SOME || action == InventoryAction.HOTBAR_SWAP
                || action == InventoryAction.DROP_ALL_SLOT || action == InventoryAction.DROP_ONE_SLOT;
    }

    /** Moves the result to the pressed hotbar slot (or offhand) if that slot is empty, like vanilla result slots. */
    private boolean giveToHotbar(PlayerInventory inventory, int hotbarButton, ItemStack result) {
        if (hotbarButton == -1) { // Offhand swap key
            if (!inventory.getItemInOffHand().isEmpty()) return false;
            inventory.setItemInOffHand(result);
            return true;
        }
        if (hotbarButton < 0 || hotbarButton > 8) return false;
        ItemStack hotbarItem = inventory.getItem(hotbarButton);
        if (hotbarItem != null && !hotbarItem.isEmpty()) return false;
        inventory.setItem(hotbarButton, result);
        return true;
    }

    /** Tosses the result in front of the player, mirroring a vanilla result-slot drop. */
    private void dropResult(Player player, ItemStack result) {
        Item drop = player.getWorld().dropItem(player.getEyeLocation(), result);
        drop.setVelocity(player.getEyeLocation().getDirection().multiply(0.3));
        drop.setPickupDelay(40);
        drop.setThrower(player.getUniqueId());
    }

    private Match grindstoneRecipe(ItemStack first, ItemStack second) {
        CustomWorkstationRecipe recipe = CustomWorkstationRegistry.match(CustomWorkstationRecipe.Type.GRINDSTONE, first, second);
        if (recipe != null) return new Match(recipe, 0, 1);
        // Only an empty first slot needs the swapped lookup: with an empty second slot, the primary
        // call above already matches single-ingredient recipes, since Ingredient.matches requires the
        // addition slot to be empty when the recipe has no addition.
        if (first == null || first.isEmpty()) {
            recipe = CustomWorkstationRegistry.match(CustomWorkstationRecipe.Type.GRINDSTONE, second, first);
            if (recipe != null) return new Match(recipe, 1, 0);
        }
        return null;
    }

    private Match match(CustomWorkstationRecipe recipe, int baseSlot, int additionSlot) {
        return recipe == null ? null : new Match(recipe, baseSlot, additionSlot);
    }

    private record Match(CustomWorkstationRecipe recipe, int baseSlot, int additionSlot) {}

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void useCauldron(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND || event.getAction() != Action.RIGHT_CLICK_BLOCK
                || event.getClickedBlock() == null) return;
        Player player = event.getPlayer();
        ItemStack held = player.getInventory().getItemInMainHand();
        Block block = event.getClickedBlock();
        for (CustomWorkstationRecipe recipe : CustomWorkstationRegistry.recipes(CustomWorkstationRecipe.Type.CAULDRON)) {
            if (!recipe.base().matches(held) || !matchesCauldron(block, recipe)) continue;
            // Check permission before cancelling the event: unpermitted players keep vanilla
            // cauldron behavior (and another matching recipe may still be permitted).
            if (!permitted(recipe, player)) continue;
            event.setCancelled(true);
            if (!validTransition(block, recipe)) return;
            ItemStack result = recipe.createResult();
            if (player.getGameMode() != GameMode.CREATIVE) held.setAmount(held.getAmount() - recipe.base().amount());
            Map<Integer, ItemStack> overflow = player.getInventory().addItem(result);
            overflow.values().forEach(item -> block.getWorld().dropItemNaturally(block.getLocation().toCenterLocation(), item));
            applyTransition(block, recipe);
            return;
        }
    }

    private boolean permitted(CustomWorkstationRecipe recipe, Player player) {
        return recipe.permission() == null || player.hasPermission(recipe.permission());
    }

    private boolean matchesCauldron(Block block, CustomWorkstationRecipe recipe) {
        Material expected = switch (recipe.fluid()) {
            case "lava" -> Material.LAVA_CAULDRON;
            case "powder_snow" -> Material.POWDER_SNOW_CAULDRON;
            case "empty" -> Material.CAULDRON;
            default -> Material.WATER_CAULDRON;
        };
        if (block.getType() != expected) return false;
        return !(block.getBlockData() instanceof Levelled levelled) || levelled.getLevel() == recipe.level();
    }

    private boolean validTransition(Block block, CustomWorkstationRecipe recipe) {
        if (recipe.fluid().equals("empty")) return recipe.value() == 0;
        if (recipe.fluid().equals("lava")) return recipe.value() == 0 || recipe.value() == -1;
        if (!(block.getBlockData() instanceof Levelled levelled)) return false;
        int target = levelled.getLevel() + recipe.value();
        return target <= 0 || target <= levelled.getMaximumLevel();
    }

    private void applyTransition(Block block, CustomWorkstationRecipe recipe) {
        if (recipe.value() == 0) return;
        if (recipe.fluid().equals("lava")) {
            block.setType(Material.CAULDRON);
            return;
        }
        Levelled levelled = (Levelled) block.getBlockData();
        int target = levelled.getLevel() + recipe.value();
        if (target <= 0) block.setType(Material.CAULDRON);
        else {
            levelled.setLevel(target);
            block.setBlockData(levelled);
        }
    }

    private boolean giveToInventory(PlayerInventory inventory, ItemStack result) {
        ItemStack[] contents = inventory.getStorageContents();
        int remaining = result.getAmount();
        for (ItemStack item : contents) {
            if (item != null && !item.isEmpty() && item.isSimilar(result))
                remaining -= item.getMaxStackSize() - item.getAmount();
        }
        for (ItemStack item : contents) {
            if (item == null || item.isEmpty()) remaining -= result.getMaxStackSize();
        }
        if (remaining > 0) return false;

        remaining = result.getAmount();
        for (int slot = 0; slot < contents.length && remaining > 0; slot++) {
            ItemStack item = contents[slot];
            if (item == null || item.isEmpty() || !item.isSimilar(result)) continue;
            int added = Math.min(remaining, item.getMaxStackSize() - item.getAmount());
            if (added <= 0) continue;
            item = item.clone();
            item.setAmount(item.getAmount() + added);
            contents[slot] = item;
            remaining -= added;
        }
        for (int slot = 0; slot < contents.length && remaining > 0; slot++) {
            ItemStack item = contents[slot];
            if (item != null && !item.isEmpty()) continue;
            ItemStack added = result.clone();
            added.setAmount(Math.min(remaining, result.getMaxStackSize()));
            contents[slot] = added;
            remaining -= added.getAmount();
        }
        inventory.setStorageContents(contents);
        return true;
    }

    private void consume(Inventory inventory, int slot, int amount) {
        ItemStack item = inventory.getItem(slot);
        if (item == null || amount <= 0 || item.getAmount() < amount) return;
        item.setAmount(item.getAmount() - amount);
        inventory.setItem(slot, item.isEmpty() ? null : item);
    }
}
