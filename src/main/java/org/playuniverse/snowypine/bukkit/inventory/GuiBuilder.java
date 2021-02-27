package org.playuniverse.snowypine.bukkit.inventory;

import org.bukkit.event.inventory.InventoryType;
import org.playuniverse.snowypine.bukkit.inventory.utils.ColorString;
import org.playuniverse.snowypine.data.properties.IProperties;

public class GuiBuilder {

	private IProperties properties = IProperties.create();

	private ColorString name = new ColorString();
	private InventoryType type = InventoryType.CHEST;
	private int size = 9;

	private GuiHandler handler;

	public IProperties properties() {
		return properties;
	}

	public ColorString name() {
		return name;
	}

	public String nameAsString() {
		return name.asColoredString();
	}

	public int size() {
		return size;
	}

	public InventoryType type() {
		return type;
	}

	public GuiHandler handler() {
		return handler;
	}

	public GuiBuilder properties(IProperties properties) {
		this.properties = properties;
		return this;
	}

	public GuiBuilder name(String... name) {
		this.name.set(name);
		return this;
	}

	public GuiBuilder size(int size) {
		this.size = size;
		return this;
	}

	public GuiBuilder type(InventoryType type) {
		this.type = type;
		return this;
	}

	public GuiBuilder handler(GuiHandler handler) {
		this.handler = handler;
		return this;
	}

	public boolean isInventoryValid() {
		return size > 0 && type != null;
	}

	public boolean hasHandler() {
		return handler != null;
	}

	public GuiInventory buildInventory() {
		return new GuiInventory(this);
	}

}
