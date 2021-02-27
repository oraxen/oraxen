package org.playuniverse.snowypine.bukkit.inventory;

import java.util.HashMap;

import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

public abstract class BasicGuiHandler implements GuiHandler {

	@Override
	public boolean onClick(GuiInventory inventory, InventoryClickEvent event) {
		return handleClickAction(inventory, event);
	}

	public final boolean handleClickAction(GuiInventory inventory, InventoryClickEvent event) {
		InventoryAction action = event.getAction();
		int slot = event.getSlot();
		ItemStack stack;
		switch (action) {
		case DROP_ALL_SLOT:
			if (event.getClickedInventory() != inventory.getInventory()) {
				return false;
			}
			return onDrop(inventory, (stack = inventory.get(slot)), slot, stack.getAmount());
		case DROP_ONE_SLOT:
			if (event.getClickedInventory() != inventory.getInventory()) {
				return false;
			}
			return onDrop(inventory, inventory.get(slot), slot, 1);
		case PICKUP_ALL:
			if (event.getClickedInventory() != inventory.getInventory()) {
				return false;
			}
			return onPickup(inventory, (stack = inventory.get(slot)), slot, stack.getAmount());
		case PICKUP_HALF:
			if (event.getClickedInventory() != inventory.getInventory()) {
				return false;
			}
			return onPickup(inventory, (stack = inventory.get(slot)), slot, stack.getAmount() / 2);
		case PICKUP_ONE:
			if (event.getClickedInventory() != inventory.getInventory()) {
				return false;
			}
			return onPickup(inventory, (stack = inventory.get(slot)), slot, 1);
		case PICKUP_SOME:
			if (event.getClickedInventory() != inventory.getInventory()) {
				return false;
			}
			return onPickup(inventory, (stack = inventory.get(slot)), slot, event.getCursor().getMaxStackSize() - event.getCursor().getAmount());
		case PLACE_ALL:
			if (event.getClickedInventory() != inventory.getInventory()) {
				return false;
			}
			return onPlace(inventory, event.getCursor(), slot, event.getCursor().getAmount());
		case PLACE_ONE:
			if (event.getClickedInventory() != inventory.getInventory()) {
				return false;
			}
			return onPlace(inventory, event.getCursor(), slot, 1);
		case PLACE_SOME:
			if (event.getClickedInventory() != inventory.getInventory()) {
				return false;
			}
			return onPlace(inventory, event.getCursor(), slot, (stack = inventory.get(slot)).getMaxStackSize() - inventory.get(slot).getAmount());
		case SWAP_WITH_CURSOR:
			if (event.getClickedInventory() != inventory.getInventory()) {
				return false;
			}
			return onSwap(inventory, inventory.get(slot), event.getCursor(), slot);
		case HOTBAR_SWAP:
			return onSwap(inventory, inventory.get(slot), event.getCurrentItem(), slot);
		case MOVE_TO_OTHER_INVENTORY:
			if (event.getCurrentItem() != inventory.getInventory()) {
				return onPickup(inventory, event.getCurrentItem(), event.getSlot(), event.getCurrentItem().getAmount());
			} else {
				return onMove(inventory, inventory.search(event.getCurrentItem()), event.getCurrentItem().getAmount());
			}
		case COLLECT_TO_CURSOR:
			return true;
		default:
			return false;
		}
	}

	protected boolean onMove(GuiInventory inventory, HashMap<Integer, ItemStack> items, int amount) {
		return false;
	}

	protected boolean onSwap(GuiInventory inventory, ItemStack previous, ItemStack now, int slot) {
		return false;
	}

	protected boolean onPlace(GuiInventory inventory, ItemStack item, int slot, int amount) {
		return false;
	}

	protected boolean onPickup(GuiInventory inventory, ItemStack item, int slot, int amount) {
		return false;
	}

	protected boolean onDrop(GuiInventory inventory, ItemStack item, int slot, int amount) {
		return false;
	}

}
