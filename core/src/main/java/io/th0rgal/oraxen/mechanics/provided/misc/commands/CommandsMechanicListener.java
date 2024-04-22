package io.th0rgal.oraxen.mechanics.provided.misc.commands;

import io.th0rgal.oraxen.config.Message;
import io.th0rgal.oraxen.utils.AdventureUtils;
import io.th0rgal.oraxen.utils.timers.Timer;
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
        Player player = event.getPlayer();
        Action action = event.getAction();
        ItemStack item = event.getItem();
        CommandsMechanic mechanic = factory.getMechanic(item);
        if (action == Action.PHYSICAL || item == null || mechanic == null) return;

        if (!mechanic.hasPermission(player)) {
            Message.NO_PERMISSION.send(player, AdventureUtils.tagResolver("permission", mechanic.getPermission()));
            return;
        }

        Timer playerTimer = mechanic.getTimer(player);
        if (!playerTimer.isFinished()) {
            playerTimer.sendToPlayer(player);
            return;
        }

        playerTimer.reset();

        mechanic.getCommands().perform(player);

        if (mechanic.isOneUsage())
            item.setAmount(item.getAmount() - 1);
    }

}
