package org.playuniverse.snowypine.bukkit.inventory.handler;

import java.util.HashMap;

import org.bukkit.entity.HumanEntity;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.playuniverse.snowypine.bukkit.inventory.GuiInventory;

public interface BasicClickHandler {

	default boolean handleClickAction(GuiInventory inventory, InventoryClickEvent event) {
		InventoryAction action = event.getAction();
		int slot = event.getSlot();
		ItemStack stack;
		switch (action) {
		case DROP_ALL_SLOT:
			if (event.getClickedInventory() != inventory.getInventory()) {
				return false;
			}
			return onDrop(event.getWhoClicked(), inventory, (stack = inventory.get(slot)), slot, stack.getAmount());
		case DROP_ONE_SLOT:
			if (event.getClickedInventory() != inventory.getInventory()) {
				return false;
			}
			return onDrop(event.getWhoClicked(), inventory, inventory.get(slot), slot, 1);
		case PICKUP_ALL:
			if (event.getClickedInventory() != inventory.getInventory()) {
				return false;
			}
			return onPickup(event.getWhoClicked(), inventory, (stack = inventory.get(slot)), slot, stack.getAmount());
		case PICKUP_HALF:
			if (event.getClickedInventory() != inventory.getInventory()) {
				return false;
			}
			return onPickup(event.getWhoClicked(), inventory, (stack = inventory.get(slot)), slot, stack.getAmount() / 2);
		case PICKUP_ONE:
			if (event.getClickedInventory() != inventory.getInventory()) {
				return false;
			}
			return onPickup(event.getWhoClicked(), inventory, (stack = inventory.get(slot)), slot, 1);
		case PICKUP_SOME:
			if (event.getClickedInventory() != inventory.getInventory()) {
				return false;
			}
			int amount = event.getCursor().getMaxStackSize() - event.getCursor().getAmount();
			if(amount > (stack = inventory.get(slot)).getAmount()) {
				amount = stack.getAmount();
			}
			return onPickup(event.getWhoClicked(), inventory, stack, slot, amount);
		case PLACE_ALL:
			if (event.getClickedInventory() != inventory.getInventory()) {
				return false;
			}
			return onPlace(event.getWhoClicked(), inventory, event.getCursor(), slot, event.getCursor().getAmount());
		case PLACE_ONE:
			if (event.getClickedInventory() != inventory.getInventory()) {
				return false;
			}
			return onPlace(event.getWhoClicked(), inventory, event.getCursor(), slot, 1);
		case PLACE_SOME:
			if (event.getClickedInventory() != inventory.getInventory()) {
				return false;
			}
			return onPlace(event.getWhoClicked(), inventory, event.getCursor(), slot, (stack = inventory.get(slot)).getMaxStackSize() - inventory.get(slot).getAmount());
		case SWAP_WITH_CURSOR:
			if (event.getClickedInventory() != inventory.getInventory()) {
				return false;
			}
			return onSwap(event.getWhoClicked(), inventory, inventory.get(slot), event.getCursor(), slot);
		case HOTBAR_SWAP:
			if (event.getClickedInventory() != inventory.getInventory()) {
				return false;
			}
			return onSwap(event.getWhoClicked(), inventory, inventory.get(slot), event.getCurrentItem(), slot);
		case MOVE_TO_OTHER_INVENTORY:
			if (event.getClickedInventory() == inventory.getInventory()) {
				return onPickup(event.getWhoClicked(), inventory, event.getCurrentItem(), event.getSlot(), event.getCurrentItem().getAmount());
			} else {
				return onMove(event.getWhoClicked(), inventory, inventory.possible(event.getCurrentItem()), event.getCurrentItem().getAmount());
			}
		case COLLECT_TO_CURSOR:
			return true;
		default:
			return false;
		}
	}

	default boolean onMove(HumanEntity entity, GuiInventory inventory, HashMap<Integer, ItemStack> items, int amount) {
		return false;
	}

	default boolean onPickup(HumanEntity entity, GuiInventory inventory, ItemStack item, int slot, int amount) {
		return false;
	}

	default boolean onPlace(HumanEntity entity, GuiInventory inventory, ItemStack item, int slot, int amount) {
		return false;
	}

	default boolean onSwap(HumanEntity entity, GuiInventory inventory, ItemStack previous, ItemStack now, int slot) {
		return false;
	}

	default boolean onDrop(HumanEntity entity, GuiInventory inventory, ItemStack item, int slot, int amount) {
		return false;
	}

}
