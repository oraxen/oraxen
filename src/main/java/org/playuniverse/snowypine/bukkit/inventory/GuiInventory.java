package org.playuniverse.snowypine.bukkit.inventory;

import static java.lang.Math.*;

import java.util.Objects;

import org.bukkit.Bukkit;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.playuniverse.snowypine.data.properties.IProperties;

public final class GuiInventory implements InventoryHolder, ItemStorage<GuiInventory> {

	private final IProperties properties;

	private final GuiHandler handler;
	private final Inventory inventory;

	private final int size;

	protected GuiInventory(GuiBuilder builder) {
		this.properties = builder.properties();
		this.handler = Objects.requireNonNull(builder.handler());
		if (!builder.isInventoryValid()) {
			throw new IllegalStateException("Configured inventory is invalid");
		}
		if (builder.type() == InventoryType.CHEST) {
			this.inventory = Bukkit.createInventory(this, builder.size(), builder.nameAsString());
		} else {
			this.inventory = Bukkit.createInventory(this, builder.type(), builder.nameAsString());
		}
		if (inventory.getHolder() != this) {
			throw new IllegalStateException("InventoryHolder isn't GuiInventory?");
		}
		this.size = inventory.getSize();
		handler.onInit(this);
	}

	/*
	 * Getter
	 */

	public IProperties getProperties() {
		return properties;
	}

	public GuiHandler getHandler() {
		return handler;
	}

	@Override
	public Inventory getInventory() {
		return inventory;
	}

	@Override
	public int size() {
		return size;
	}

	/*
	 * ItemStorage implementation
	 */

	@Override
	public GuiInventory me() {
		return this;
	}

	@Override
	public ItemStack get(int id) {
		return inventory.getItem(min(id, size));
	}

	@Override
	public GuiInventory set(int id, ItemStack stack) {
		inventory.setItem(min(id, size), stack);
		return this;
	}

}
