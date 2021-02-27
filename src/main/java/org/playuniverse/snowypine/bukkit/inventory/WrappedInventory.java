package org.playuniverse.snowypine.bukkit.inventory;

import static java.lang.Math.min;

import java.util.Objects;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class WrappedInventory implements ItemStorage<WrappedInventory> {

	private final int size;
	private final Inventory inventory;

	public WrappedInventory(Inventory inventory) {
		this.inventory = Objects.requireNonNull(inventory);
		this.size = inventory.getSize();
	}
	
	public Inventory getInventory() {
		return inventory;
	}

	@Override
	public int size() {
		return size;
	}

	@Override
	public ItemStack get(int id) {
		return inventory.getItem(min(id, size));
	}

	@Override
	public WrappedInventory set(int id, ItemStack stack) {
		inventory.setItem(min(id, size), stack);
		return this;
	}

	@Override
	public WrappedInventory me() {
		return this;
	}

}
