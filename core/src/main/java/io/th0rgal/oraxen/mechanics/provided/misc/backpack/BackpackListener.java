package io.th0rgal.oraxen.mechanics.provided.misc.backpack;

import com.jeff_media.morepersistentdatatypes.DataType;
import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.StorageGui;
import io.th0rgal.oraxen.api.OraxenItems;
import io.th0rgal.oraxen.utils.AdventureUtils;
import io.th0rgal.oraxen.utils.InventoryUtils;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;

import static io.th0rgal.oraxen.mechanics.provided.misc.backpack.BackpackMechanic.BACKPACK_KEY;

public class BackpackListener implements Listener {

    private final BackpackMechanicFactory factory;

    public BackpackListener(BackpackMechanicFactory factory) {
        this.factory = factory;
    }

    // Cancels placing backpacks if the base material is a block
    @EventHandler(priority = EventPriority.MONITOR)
    public void onBlockPlace(final BlockPlaceEvent event) {
        if (isBackpack(event.getPlayer().getInventory().getItemInMainHand()))
            event.setCancelled(true);
    }

    @EventHandler
    public void onPlayerDisconnect(final PlayerQuitEvent event) {
        closeBackpack(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (event.getHand() == EquipmentSlot.OFF_HAND) return;
        if (event.useItemInHand() == Event.Result.DENY) return;
        event.setUseItemInHand(Event.Result.ALLOW);
        openBackpack(event.getPlayer());
    }

    @EventHandler
    public void onPlayerSwapHandItems(final PlayerSwapHandItemsEvent event) {
        Player player = event.getPlayer();
        if (!isBackpack(event.getOffHandItem()) && !isBackpack(event.getMainHandItem())) return;
        if (!(InventoryUtils.topInventoryForPlayer(player).getHolder() instanceof StorageGui gui)) return;
        gui.close(player, true);
    }

    // Refresh close backpack if open to refresh with picked up items
    @EventHandler(ignoreCancelled = true)
    public void onPickupItem(final EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (!(InventoryUtils.topInventoryForPlayer(player).getHolder() instanceof Gui)) return;
        closeBackpack(player);
        openBackpack(player);
    }

    private StorageGui createGUI(BackpackMechanic mechanic, ItemStack backpack) {
        ItemMeta backpackMeta = backpack.getItemMeta();
        if (!isBackpack(backpack) || backpackMeta == null) return null;
        PersistentDataContainer pdc = backpackMeta.getPersistentDataContainer();
        StorageGui gui = Gui.storage().title(AdventureUtils.MINI_MESSAGE.deserialize(mechanic.getTitle())).rows(mechanic.getRows()).create();

        gui.disableItemDrop();
        gui.disableItemSwap();

        gui.setPlayerInventoryAction(event -> {
            if (isBackpack(event.getCurrentItem()) || isBackpack(event.getCursor())) event.setCancelled(true);
        });

        gui.setDefaultClickAction(event -> {
            if (isBackpack(event.getCurrentItem()) || isBackpack(event.getCursor())) event.setCancelled(true);
        });

        gui.setDragAction(event -> {
            if (isBackpack(event.getCursor())) event.setCancelled(true);
        });

        gui.setOutsideClickAction(event -> {
            if (isBackpack(event.getCursor())) event.setCancelled(true);
        });

        gui.setOpenGuiAction(event -> {
            Player player = (Player) event.getPlayer();
            ItemStack[] contents = pdc.get(BACKPACK_KEY, DataType.ITEM_STACK_ARRAY);
            if (contents != null) gui.getInventory().setContents(contents);
            if (mechanic.hasOpenSound())
                player.playSound(player.getLocation(), mechanic.getOpenSound(), mechanic.getVolume(), mechanic.getPitch());
        });

        gui.setCloseGuiAction(event -> {
            Player player = (Player) event.getPlayer();
            pdc.set(BACKPACK_KEY, DataType.ITEM_STACK_ARRAY, gui.getInventory().getContents());
            backpack.setItemMeta(backpackMeta);
            if (mechanic.hasCloseSound())
                player.getWorld().playSound(player.getLocation(), mechanic.getCloseSound(), mechanic.getVolume(), mechanic.getPitch());
        });

        return gui;
    }

    private boolean isBackpack(ItemStack item) {
        return item  != null && factory.getMechanic(OraxenItems.getIdByItem(item)) != null;
    }

    private void openBackpack(Player player) {
        ItemStack itemInHand = player.getInventory().getItemInMainHand();
        BackpackMechanic mechanic = (BackpackMechanic) factory.getMechanic(OraxenItems.getIdByItem(itemInHand));
        StorageGui gui = createGUI(mechanic, itemInHand);
        if (gui == null) return;
        gui.open(player);
    }

    private void closeBackpack(Player player) {
        InventoryHolder holder = InventoryUtils.topInventoryForPlayer(player).getHolder();
        if (!isBackpack(player.getInventory().getItemInMainHand())) return;
        if (holder instanceof StorageGui gui)
            gui.close(player, true);
    }
}
