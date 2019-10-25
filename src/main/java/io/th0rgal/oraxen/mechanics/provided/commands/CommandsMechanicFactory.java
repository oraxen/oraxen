package io.th0rgal.oraxen.mechanics.provided.commands;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.items.OraxenItems;
import io.th0rgal.oraxen.mechanics.Mechanic;
import io.th0rgal.oraxen.mechanics.MechanicFactory;
import io.th0rgal.oraxen.mechanics.MechanicsManager;
import io.th0rgal.oraxen.utils.Logs;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.bukkit.inventory.ItemStack;

public class CommandsMechanicFactory extends MechanicFactory {

    public CommandsMechanicFactory(ConfigurationSection section) {
        super(section);
        Logs.logError("X");
        MechanicsManager.registerListeners(OraxenPlugin.get(), new CommandsItemListener(this));
    }

    @Override
    public Mechanic parse(ConfigurationSection itemMechanicConfiguration) {
        Mechanic mechanic = new CommandsMechanic(this, itemMechanicConfiguration);
        addToImplemented(mechanic);
        return mechanic;
    }
}

class CommandsItemListener implements Listener {

    private CommandsMechanicFactory factory;

    public CommandsItemListener(CommandsMechanicFactory factory) {
        this.factory = factory;
    }

    @EventHandler(priority = EventPriority.NORMAL)
    private void onInteract(PlayerInteractEvent event) {
        Logs.logError("-1");
        if (event.getAction() != Action.LEFT_CLICK_AIR
                && event.getAction() != Action.RIGHT_CLICK_AIR
                && event.getAction() != Action.LEFT_CLICK_BLOCK
                && event.getAction() != Action.RIGHT_CLICK_BLOCK)
            return;

        Logs.logError("0");

        ItemStack item = event.getItem();
        if (item == null)
            return;

        Logs.logError("1");

        String itemID = OraxenItems.getIdByItem(item);
        if (factory.isNotImplementedIn(itemID))
            return;

        Logs.logError("2");

        CommandsMechanic mechanic = (CommandsMechanic) factory.getMechanic(itemID);
        Player player = event.getPlayer();
        String playerName = player.getName();

        if (mechanic.hasConsoleCommands())
            for (String command : mechanic.getConsoleCommands())
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                        command.replace("%p%", playerName));

        if (mechanic.hasPlayerCommands())
            for (String command : mechanic.getConsoleCommands())
                Bukkit.dispatchCommand(player,
                        command.replace("%p%", playerName));

        if (mechanic.hasOppedPlayerCommands())
            for (String command : mechanic.getOppedPlayerCommands()) {
                boolean wasOp = player.isOp();
                player.setOp(true);
                Bukkit.dispatchCommand(player,
                        command.replace("%p%", playerName));
                player.setOp(wasOp);
            }

    }


}