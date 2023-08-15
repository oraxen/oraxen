package io.th0rgal.oraxen.mechanics.provided.misc.custom.listeners;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.mechanics.provided.misc.custom.fields.CustomEvent;
import io.th0rgal.oraxen.utils.actions.ClickAction;
import io.th0rgal.oraxen.utils.timers.Timer;
import io.th0rgal.oraxen.utils.timers.TimersFactory;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;

public abstract class CustomListener implements Listener {

    protected final String itemID;
    protected final TimersFactory timers;

    protected final CustomEvent event;
    protected final ClickAction clickAction;

    protected CustomListener(String itemID, long cooldown, CustomEvent event, ClickAction clickAction) {
        this.itemID = itemID;
        this.timers = new TimersFactory(cooldown * 50);
        this.event = event;
        this.clickAction = clickAction;
    }

    public void register() {
        Bukkit.getPluginManager().registerEvents(this, OraxenPlugin.get());
    }

    public void unregister() {
        HandlerList.unregisterAll(this);
    }

    public void perform(Player player, ItemStack itemStack) {
        if (!clickAction.canRun(player)) return;

        final Timer playerTimer = timers.getTimer(player);

        if (!playerTimer.isFinished()) {
            playerTimer.sendToPlayer(player);
            return;
        }

        playerTimer.reset();

        clickAction.performActions(player);

        if (event.isOneUsage()) {
            itemStack.setAmount(itemStack.getAmount() - 1);
        }
    }

}
