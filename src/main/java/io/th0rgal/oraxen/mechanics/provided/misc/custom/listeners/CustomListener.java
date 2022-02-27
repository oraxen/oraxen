package io.th0rgal.oraxen.mechanics.provided.misc.custom.listeners;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.mechanics.provided.misc.custom.fields.CustomAction;
import io.th0rgal.oraxen.mechanics.provided.misc.custom.fields.CustomCondition;
import io.th0rgal.oraxen.mechanics.provided.misc.custom.fields.CustomEvent;
import io.th0rgal.oraxen.utils.Utils;
import io.th0rgal.oraxen.utils.timers.Timer;
import io.th0rgal.oraxen.utils.timers.TimersFactory;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public abstract class CustomListener implements Listener {

    protected final String itemID;
    protected final TimersFactory timers;

    protected final CustomEvent event;
    protected final List<CustomCondition> conditions;
    protected final List<CustomAction> actions;

    public CustomListener(String itemID, long cooldown, CustomEvent event,
                          List<CustomCondition> conditions, List<CustomAction> actions) {
        this.itemID = itemID;
        this.timers = new TimersFactory(cooldown * 50);
        this.event = event;
        this.conditions = conditions;
        this.actions = actions;
    }

    public void register() {
        Bukkit.getPluginManager().registerEvents(this, OraxenPlugin.get());
    }

    public void unregister() {
        HandlerList.unregisterAll(this);
    }

    public void perform(Player player, ItemStack itemStack) {

        for (CustomCondition condition : conditions)
            switch (condition.type) {
                case HAS_PERMISSION -> {
                    if (!player.hasPermission(condition.getParams().get(0)))
                        return;
                }
                default -> throw new IllegalStateException("Unexpected value: " + condition.type);
            }

        Timer playerTimer = timers.getTimer(player);
        if (!playerTimer.isFinished()) {
            playerTimer.sendToPlayer(player);
            return;
        }

        playerTimer.reset();

        for (CustomAction action : actions)
            switch (action.type) {
                case COMMAND -> {
                    final CommandSender sender = switch (action.getParams().get(0)) {
                        case "player" -> player;
                        case "console" -> Bukkit.getConsoleSender();
                        default -> null;
                    };

                    if (sender == null) {
                        throw new IllegalStateException("Unexpected command executor type " + action.getParams().get(0));
                    }

                    Bukkit.dispatchCommand(sender, action.getParams().get(1)
                            .replace("<player>", player.getName()));
                }

                case MESSAGE -> OraxenPlugin.get().getAudience().sender(player)
                        .sendMessage(Utils.MINI_MESSAGE.parse(action.getParams().get(0)));

                case ACTIONBAR -> {
                    OraxenPlugin.get().getAudience().sender(player)
                            .sendActionBar(Utils.MINI_MESSAGE.parse(action.getParams().get(0)));
                }

                default -> throw new IllegalStateException("Unexpected value: " + action);
            }

        if (event.isOneUsage())
            itemStack.setAmount(itemStack.getAmount() - 1);
    }

}
