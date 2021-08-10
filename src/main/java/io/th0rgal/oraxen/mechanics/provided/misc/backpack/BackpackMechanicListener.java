package io.th0rgal.oraxen.mechanics.provided.misc.backpack;

import com.github.stefvanschie.inventoryframework.gui.type.ChestGui;
import de.tr7zw.nbtapi.NBTCompound;
import de.tr7zw.nbtapi.NBTItem;
import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.config.Message;
import io.th0rgal.oraxen.items.OraxenItems;
import io.th0rgal.oraxen.utils.Utils;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.apache.commons.lang.ArrayUtils;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

public class BackpackMechanicListener implements Listener {

    private final BackpackMechanicFactory factory;

    public BackpackMechanicListener(BackpackMechanicFactory factory) {
        this.factory = factory;
    }

    private static boolean isBackpackOpen(Player player) {
        return player.getOpenInventory().getTopInventory().getHolder() instanceof BackpackGui;
    }

    private static void saveBackpack(ItemStack backpack, BackpackGui gui) {
        NBTItem nbtItem = new NBTItem(backpack, true);
        if (!nbtItem.hasKey("inventory"))
            nbtItem.addCompound("inventory");
        NBTCompound inventory = nbtItem.getCompound("inventory");
        for (int i = 0; i < gui.getInventory().getSize(); i++)
            inventory.setItemStack(String.valueOf(i), gui.getInventory().getItem(i));
    }

    private static String parseComponentString(String miniString, ItemStack backpack) {
        return Utils.LEGACY_COMPONENT_SERIALIZER.serialize(MiniMessage.get()
                .parse(miniString, ArrayUtils.addAll(OraxenPlugin.get().getFontManager().getMiniMessagePlaceholders(), new String[]{"itemname", backpack.getItemMeta().getDisplayName()})));
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if ((event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) || event.getHand() == EquipmentSlot.OFF_HAND)
            return;
        ItemStack item = event.getItem();
        String id = OraxenItems.getIdByItem(item);
        if (factory.isNotImplementedIn(id)) {
            return;
        }
        if (item.getAmount() > 1 || item.getAmount() < 1) {
            Message.MECHANICS_BACKPACK_STACKED.send(event.getPlayer());
            return;
        }

        event.setCancelled(true);

        BackpackMechanic mechanic = (BackpackMechanic) factory.getMechanic(id);

        createGUI(event.getPlayer(), mechanic, item);
        if(mechanic.hasOpenSound())
            event.getPlayer().playSound(event.getPlayer().getLocation(), mechanic.getOpenSound(), mechanic.getVolume(), mechanic.getPitch());
    }

    @EventHandler
    public void onPlayerItemHeld(PlayerItemHeldEvent event) {
        if (isBackpackOpen(event.getPlayer()))
            event.setCancelled(true);
    }

    @EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        if (isBackpackOpen(event.getPlayer()) && isBackpack(event.getItemDrop().getItemStack()))
            event.setCancelled(true);
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        ItemStack backpack = event.getPlayer().getInventory().getItemInMainHand();
        if (isBackpack(backpack))
            if (event.getInventory().getHolder() instanceof BackpackGui gui)
                saveBackpack(backpack, gui);
    }

    private boolean isBackpack(ItemStack itemStack) {
        String id = OraxenItems.getIdByItem(itemStack);
        return id != null && !factory.isNotImplementedIn(id);
    }

    private BackpackGui createGUI(Player player, BackpackMechanic mechanic, ItemStack backpack) {
        BackpackGui gui = new BackpackGui(mechanic, backpack);

        gui.setOnBottomClick(event -> {
            if (backpack.equals(event.getCurrentItem()) || backpack.equals(event.getCursor()))
                event.setCancelled(true);
            if (isBackpack(event.getCurrentItem()))
                event.setCancelled(true);
        });

        gui.setOnBottomDrag(event -> {
            if (backpack.equals(event.getCursor()) || backpack.equals(event.getOldCursor()))
                event.setCancelled(true);
        });

        gui.setOnGlobalClick(event -> {
            if(event.getAction() != InventoryAction.NOTHING)
                saveBackpack(backpack, gui);
        });

        gui.show(player);

        NBTItem nbtItem = new NBTItem(backpack);

        if (nbtItem.hasKey("inventory")) {
            NBTCompound inventory = nbtItem.getCompound("inventory");

            for (int i = 0; i < gui.getInventory().getSize(); i++)
                if (inventory.hasKey(String.valueOf(i)))
                    gui.getInventory().setItem(i, NBTItem.convertNBTtoItem(inventory.getCompound(String.valueOf(i))));
        }


        return gui;
    }

    private static class BackpackGui extends ChestGui {
        public BackpackGui(BackpackMechanic mechanic, ItemStack backpack) {
            super(mechanic.getRows(), parseComponentString(mechanic.getTitle(), backpack));
        }
    }
}
