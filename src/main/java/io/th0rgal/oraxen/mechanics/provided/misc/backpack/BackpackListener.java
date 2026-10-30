package io.th0rgal.oraxen.mechanics.provided.misc.backpack;

import com.jeff_media.morepersistentdatatypes.DataType;
import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.StorageGui;
import io.th0rgal.oraxen.api.OraxenItems;
import io.th0rgal.oraxen.utils.AdventureUtils;
import io.th0rgal.oraxen.utils.InventoryUtils;
import net.kyori.adventure.sound.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static io.th0rgal.oraxen.mechanics.provided.misc.backpack.BackpackMechanic.BACKPACK_KEY;

public class BackpackListener implements Listener {

    private final BackpackMechanicFactory factory;
    private final Map<UUID, StorageGui> openBackpacks = new ConcurrentHashMap<>();

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
        openBackpacks.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (event.getHand() == EquipmentSlot.OFF_HAND) return;
        if (event.useItemInHand() == Event.Result.DENY) return;
        if (event.getClickedBlock() != null && isBackpack(event.getItem()))
            event.setUseInteractedBlock(Event.Result.DENY);
        event.setUseItemInHand(Event.Result.ALLOW);
        openBackpack(event.getPlayer());
    }

    @EventHandler
    public void onPlayerSwapHandItems(final PlayerSwapHandItemsEvent event) {
        Player player = event.getPlayer();
        if (!isBackpack(event.getOffHandItem()) && !isBackpack(event.getMainHandItem())) return;
        if (!(InventoryUtils.topInventoryForPlayer(player).getHolder() instanceof StorageGui)) return;
        // The swap event already holds copies with the old backpack contents.
        event.setCancelled(true);
        player.closeInventory();
    }

    // Prevent dropping backpacks while the backpack GUI is open to avoid duplication
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerDropItem(final PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        if (!isBackpack(event.getItemDrop().getItemStack())) return;
        if (InventoryUtils.topInventoryForPlayer(player).getHolder() instanceof StorageGui) {
            event.setCancelled(true);
        }
    }

    // Refresh close backpack if open to refresh with picked up items
    @EventHandler(ignoreCancelled = true)
    public void onPickupItem(final EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        StorageGui gui = openBackpacks.get(player.getUniqueId());
        if (gui == null || InventoryUtils.topInventoryForPlayer(player).getHolder() != gui) return;
        closeBackpack(player);
        openBackpack(player);
    }

    private StorageGui createGUI(BackpackMechanic mechanic, ItemStack backpack, int backpackSlot) {
        ItemMeta backpackMeta = backpack.getItemMeta();
        if (!isBackpack(backpack) || backpackMeta == null) return null;
        PersistentDataContainer pdc = backpackMeta.getPersistentDataContainer();
        StorageGui gui = Gui.storage().title(AdventureUtils.MINI_MESSAGE.deserialize(mechanic.getTitle())).rows(mechanic.getRows()).create();

        gui.disableItemDrop();
        gui.disableItemSwap();

        gui.setPlayerInventoryAction(event -> {
            if (isBackpack(event.getCurrentItem()) || isBackpack(event.getCursor())
                    || event.isShiftClick() && mechanic.isBlocked(event.getCurrentItem())) event.setCancelled(true);
        });

        gui.setDefaultClickAction(event -> {
            if (isBackpack(event.getCurrentItem()) || isBackpack(event.getCursor())) event.setCancelled(true);
        });

        gui.setDefaultTopClickAction(event -> {
            int hotbarButton = event.getHotbarButton();
            ItemStack hotbarItem = hotbarButton >= 0
                    ? event.getWhoClicked().getInventory().getItem(hotbarButton) : null;
            ItemStack offhandItem = event.getClick() == ClickType.SWAP_OFFHAND
                    ? event.getWhoClicked().getInventory().getItemInOffHand() : null;
            if (mechanic.isBlocked(event.getCursor()) || mechanic.isBlocked(hotbarItem)
                    || mechanic.isBlocked(offhandItem))
                event.setCancelled(true);
        });

        gui.setDragAction(event -> {
            if (isBackpack(event.getOldCursor()) || mechanic.isBlocked(event.getOldCursor())) event.setCancelled(true);
        });

        gui.setOutsideClickAction(event -> {
            if (isBackpack(event.getCursor())) event.setCancelled(true);
        });

        gui.setOpenGuiAction(event -> {
            Player player = (Player) event.getPlayer();
            openBackpacks.put(player.getUniqueId(), gui);
            ItemStack[] contents = pdc.get(BACKPACK_KEY, DataType.ITEM_STACK_ARRAY);
            if (contents != null) gui.getInventory().setContents(contents);
            if (mechanic.hasOpenSound())
                AdventureUtils.playSound(player, mechanic.getOpenSound(), Sound.Source.MASTER, mechanic.getVolume(), mechanic.getPitch());
        });

        gui.setCloseGuiAction(event -> {
            Player player = (Player) event.getPlayer();
            openBackpacks.remove(player.getUniqueId(), gui);
            // A cancelled drop can restore a different ItemStack to this slot.
            ItemStack currentBackpack = player.getInventory().getItem(backpackSlot);
            if (currentBackpack == null || !currentBackpack.isSimilar(backpack)) return;
            ItemMeta currentMeta = currentBackpack.getItemMeta();
            if (currentMeta == null) return;
            currentMeta.getPersistentDataContainer().set(BACKPACK_KEY, DataType.ITEM_STACK_ARRAY, gui.getInventory().getContents());
            currentBackpack.setItemMeta(currentMeta);
            player.getInventory().setItem(backpackSlot, currentBackpack);
            if (mechanic.hasCloseSound())
                AdventureUtils.playSound(player.getLocation(), mechanic.getCloseSound(), Sound.Source.MASTER, mechanic.getVolume(), mechanic.getPitch());
        });

        return gui;
    }

    private boolean isBackpack(ItemStack item) {
        return item  != null && factory.getMechanic(OraxenItems.getIdByItem(item)) != null;
    }

    private void openBackpack(Player player) {
        ItemStack itemInHand = player.getInventory().getItemInMainHand();
        BackpackMechanic mechanic = (BackpackMechanic) factory.getMechanic(OraxenItems.getIdByItem(itemInHand));
        StorageGui gui = createGUI(mechanic, itemInHand.clone(), player.getInventory().getHeldItemSlot());
        if (gui == null) return;
        gui.open(player);
    }

    private void closeBackpack(Player player) {
        InventoryHolder holder = InventoryUtils.topInventoryForPlayer(player).getHolder();
        if (!isBackpack(player.getInventory().getItemInMainHand())) return;
        if (holder instanceof StorageGui)
            player.closeInventory();
    }
}
