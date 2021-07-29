package io.th0rgal.oraxen.mechanics.provided.misc.custom.listeners;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.items.OraxenItems;
import io.th0rgal.oraxen.mechanics.provided.misc.custom.fields.CustomAction;
import io.th0rgal.oraxen.mechanics.provided.misc.custom.fields.CustomCondition;
import io.th0rgal.oraxen.mechanics.provided.misc.custom.fields.CustomEvent;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class DropListener extends CustomListener {


    public DropListener(String itemID, CustomEvent event,
                        List<CustomCondition> conditions, List<CustomAction> actions) {
        super(itemID, event, conditions, actions);
    }

    @EventHandler
    public void onDropped(PlayerInteractEvent event) {
        ItemStack item = event.getItem();
        if (!itemID.equals(OraxenItems.getIdByItem(item)))
            return;
        Player player = event.getPlayer();
        for (CustomCondition condition : conditions)
            switch (condition.type) {
                case HAS_PERMISSION -> {
                    if (!player.hasPermission(condition.getParams().get(0)))
                        return;
                }
            }

        for (CustomAction action : actions) {
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
            }
        }

    }

}
