package org.playuniverse.snowypine.bukkit.inventory;

import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.enchantment.PrepareItemEnchantEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryCreativeEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.inventory.PrepareSmithingEvent;
import org.bukkit.event.inventory.TradeSelectEvent;

public interface GuiHandler {

	public default void onInit(GuiInventory inventory) {}

	public default void onPreAnvil(GuiInventory inventory, PrepareAnvilEvent event) {}

	public default void onPreCraft(GuiInventory inventory, PrepareItemCraftEvent event) {}

	public default void onPreSmith(GuiInventory inventory, PrepareSmithingEvent event) {}

	public default boolean onClose(GuiInventory inventory, InventoryCloseEvent event) {
		return false;
	}

	public default boolean onOpen(GuiInventory inventory, InventoryOpenEvent event) {
		return false;
	}

	public default boolean onPreEnchant(GuiInventory inventory, PrepareItemEnchantEvent event) {
		return false;
	}

	public default boolean onEnchant(GuiInventory inventory, EnchantItemEvent event) {
		return false;
	}

	public default boolean onClick(GuiInventory inventory, InventoryClickEvent event) {
		return false;
	}

	public default boolean onCraft(GuiInventory inventory, CraftItemEvent event) {
		return false;
	}

	public default boolean onCreative(GuiInventory inventory, InventoryCreativeEvent event) {
		return false;
	}

	public default boolean onDrag(GuiInventory inventory, InventoryDragEvent event) {
		return false;
	}

	public default boolean onTradeSelect(GuiInventory inventory, TradeSelectEvent event) {
		return false;
	}

	public default boolean onItemMove(GuiInventory inventory, InventoryMoveItemEvent event, MoveInventory state) {
		return false;
	}

}
