package org.playuniverse.snowypine.bukkit.inventory;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
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
import org.bukkit.inventory.Inventory;
import org.playuniverse.snowypine.helper.task.TaskHelper;

public final class GuiListener implements Listener {

	public static final GuiListener LISTENER = new GuiListener();

	private GuiListener() {}

	private GuiInventory getGui(Inventory inventory) {
		if (inventory.getHolder() instanceof GuiInventory) {
			return (GuiInventory) inventory.getHolder();
		}
		return null;
	}

	@EventHandler
	public void onEvent(PrepareAnvilEvent event) {
		GuiInventory inventory = getGui(event.getInventory());
		if (inventory == null) {
			return;
		}
		inventory.getHandler().onPreAnvil(inventory, event);
	}

	@EventHandler
	public void onEvent(PrepareItemCraftEvent event) {
		GuiInventory inventory = getGui(event.getInventory());
		if (inventory == null) {
			return;
		}
		inventory.getHandler().onPreCraft(inventory, event);
	}

	@EventHandler
	public void onEvent(PrepareSmithingEvent event) {
		GuiInventory inventory = getGui(event.getInventory());
		if (inventory == null) {
			return;
		}
		inventory.getHandler().onPreSmith(inventory, event);
	}

	@EventHandler
	public void onEvent(InventoryCloseEvent event) {
		GuiInventory inventory = getGui(event.getInventory());
		if (inventory == null) {
			return;
		}
		if (inventory.getHandler().onClose(inventory, event)) {
			TaskHelper.runSync(() -> {
				event.getPlayer().openInventory(inventory.getInventory());
			});
		}
	}

	@EventHandler
	public void onEvent(InventoryOpenEvent event) {
		GuiInventory inventory = getGui(event.getInventory());
		if (inventory == null) {
			return;
		}
		event.setCancelled(inventory.getHandler().onOpen(inventory, event));
	}

	@EventHandler
	public void onEvent(PrepareItemEnchantEvent event) {
		GuiInventory inventory = getGui(event.getInventory());
		if (inventory == null) {
			return;
		}
		event.setCancelled(inventory.getHandler().onPreEnchant(inventory, event));
	}

	@EventHandler
	public void onEvent(EnchantItemEvent event) {
		GuiInventory inventory = getGui(event.getInventory());
		if (inventory == null) {
			return;
		}
		event.setCancelled(inventory.getHandler().onEnchant(inventory, event));
	}

	@EventHandler
	public void onEvent(InventoryClickEvent event) {
		GuiInventory inventory = getGui(event.getInventory());
		if (inventory == null || event instanceof CraftItemEvent || event instanceof InventoryCreativeEvent) {
			return;
		}
		event.setCancelled(inventory.getHandler().onClick(inventory, event));
	}

	@EventHandler
	public void onEvent(CraftItemEvent event) {
		GuiInventory inventory = getGui(event.getInventory());
		if (inventory == null) {
			return;
		}
		event.setCancelled(inventory.getHandler().onCraft(inventory, event));
	}

	@EventHandler
	public void onEvent(InventoryCreativeEvent event) {
		GuiInventory inventory = getGui(event.getInventory());
		if (inventory == null) {
			return;
		}
		event.setCancelled(inventory.getHandler().onCreative(inventory, event));
	}

	@EventHandler
	public void onEvent(InventoryDragEvent event) {
		GuiInventory inventory = getGui(event.getInventory());
		if (inventory == null) {
			return;
		}
		event.setCancelled(inventory.getHandler().onDrag(inventory, event));
	}

	@EventHandler
	public void onEvent(TradeSelectEvent event) {
		GuiInventory inventory = getGui(event.getInventory());
		if (inventory == null) {
			return;
		}
		event.setCancelled(inventory.getHandler().onTradeSelect(inventory, event));
	}

	@EventHandler
	public void onEvent(InventoryMoveItemEvent event) {
		GuiInventory initiator = getGui(event.getInitiator());
		if (initiator != null) {
			initiator.getHandler().onItemMove(initiator, event, MoveInventory.of(true, event.getSource() == event.getInitiator()));
		}
		GuiInventory destination = getGui(event.getDestination());
		if (destination != null) {
			destination.getHandler().onItemMove(destination, event, MoveInventory.of(false, event.getSource() == event.getDestination()));
		}
	}

}
