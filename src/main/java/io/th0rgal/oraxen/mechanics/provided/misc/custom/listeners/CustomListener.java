package io.th0rgal.oraxen.mechanics.provided.misc.custom.listeners;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.mechanics.provided.misc.custom.fields.CustomAction;
import io.th0rgal.oraxen.mechanics.provided.misc.custom.fields.CustomCondition;
import io.th0rgal.oraxen.mechanics.provided.misc.custom.fields.CustomEvent;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public abstract class CustomListener implements Listener {

    protected final String itemID;
    protected final CustomEvent event;
    protected final List<CustomCondition> conditions;
    protected final List<CustomAction> actions;

    public CustomListener(String itemID, CustomEvent event,
                          List<CustomCondition> conditions, List<CustomAction> actions) {
        this.itemID = itemID;
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

        for (CustomAction action : actions)
            switch (action.type) {
                case COMMAND -> {
                    CommandSender sender;
                    switch (action.getParams().get(0)) {
                        case "player" -> sender = player;
                        case "console" -> sender = Bukkit.getConsoleSender();
                        default -> sender = null;
                    }
                    Bukkit.dispatchCommand(sender, action.getParams().get(1)
                            .replace("<player>", player.getName()));
                }

                case MESSAGE -> OraxenPlugin.get().getAudience().sender(player)
                        .sendMessage(MiniMessage.get().parse(action.getParams().get(0)));

                case ACTIONBAR -> {
                    OraxenPlugin.get().getAudience().sender(player)
                            .sendActionBar(MiniMessage.get().parse(action.getParams().get(0)));
                }

                default -> throw new IllegalStateException("Unexpected value: " + action);
            }

        if (event.isOneUsage())
            itemStack.setAmount(itemStack.getAmount() - 1);
    }

}
