package io.th0rgal.oraxen.mechanics.provided.misc.backpack;

import com.jeff_media.morepersistentdatatypes.DataType;
import dev.triumphteam.gui.components.GuiType;
import dev.triumphteam.gui.guis.Gui;
import io.th0rgal.oraxen.config.Message;
import io.th0rgal.oraxen.items.OraxenItems;
import io.th0rgal.oraxen.utils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;

import java.util.Arrays;
import java.util.Objects;

import static io.th0rgal.oraxen.mechanics.provided.misc.backpack.BackpackMechanic.BACKPACK_KEY;

public class BackpackListener implements Listener {

    private final BackpackMechanicFactory factory;

    public BackpackListener(BackpackMechanicFactory factory) {
        this.factory = factory;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (event.getHand() == EquipmentSlot.OFF_HAND) return;

        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();
        String id = OraxenItems.getIdByItem(item);
        if (factory.isNotImplementedIn(id)) return;

        if (item.getAmount() != 1) {
            Message.MECHANICS_BACKPACK_STACKED.send(player);
            return;
        }
        event.setCancelled(true);
        BackpackMechanic mechanic = (BackpackMechanic) factory.getMechanic(id);
        createGUI(mechanic, item).open(player);
        if (mechanic.hasOpenSound())
            player.playSound(player.getLocation(), mechanic.getOpenSound(), mechanic.getVolume(), mechanic.getPitch());
    }

    private Gui createGUI(BackpackMechanic mechanic, ItemStack backpack) {
        PersistentDataContainer pdc = Objects.requireNonNull(backpack.getItemMeta()).getPersistentDataContainer();
        Gui gui = Gui.gui().type(GuiType.CHEST).title(Utils.MINI_MESSAGE.deserialize(mechanic.getTitle())).rows(mechanic.getRows()).create();

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
            ItemStack[] contents = pdc.get(BACKPACK_KEY, DataType.ITEM_STACK_ARRAY);
            if (contents != null) gui.getInventory().setContents(contents);
        });

        gui.setCloseGuiAction(event -> {
            Player player = (Player) event.getPlayer();
            Bukkit.broadcastMessage("Content: " + Arrays.toString(gui.getInventory().getContents()));
            Bukkit.broadcastMessage("hasBackpackKey: " + pdc.has(BACKPACK_KEY, DataType.ITEM_STACK_ARRAY));
            pdc.set(BACKPACK_KEY, DataType.ITEM_STACK_ARRAY, gui.getInventory().getContents());
            Bukkit.broadcastMessage("hasBackpackKey2: " + pdc.has(BACKPACK_KEY, DataType.ITEM_STACK_ARRAY));
            if (mechanic.hasCloseSound())
                player.getWorld().playSound(player.getLocation(), mechanic.getCloseSound(), mechanic.getVolume(), mechanic.getPitch());
        });

        return gui;
    }

    private boolean isBackpack(ItemStack item) {
        return item  != null && item.hasItemMeta() && Objects.requireNonNull(item.getItemMeta()).getPersistentDataContainer().has(BACKPACK_KEY, DataType.ITEM_STACK_ARRAY);
    }
}
