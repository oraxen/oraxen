package io.th0rgal.oraxen.mechanics.provided.commands;

import io.th0rgal.oraxen.items.OraxenItems;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

public class CommandsMechanicListener implements Listener {

    private final CommandsMechanicFactory factory;

    public CommandsMechanicListener(CommandsMechanicFactory factory) {
        this.factory = factory;
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onInteract(PlayerInteractEvent event) {
        Action action = event.getAction();
        if (action != Action.LEFT_CLICK_AIR
                && action != Action.RIGHT_CLICK_AIR
                && action != Action.LEFT_CLICK_BLOCK
                && action != Action.RIGHT_CLICK_BLOCK)
            return;

        ItemStack item = event.getItem();
        if (item == null)
            return;

        String itemID = OraxenItems.getIdByItem(item);
        if (factory.isNotImplementedIn(itemID))
            return;

        CommandsMechanic mechanic = (CommandsMechanic) factory.getMechanic(itemID);
        Player player = event.getPlayer();
        String playerName = player.getName();
        mechanic.getCommands().perform(player);
    }

}
