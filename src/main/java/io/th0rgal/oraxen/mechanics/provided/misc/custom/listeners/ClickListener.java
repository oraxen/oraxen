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
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ClickListener extends CustomListener {

    private final Set<Action> interactActions = new HashSet<>();

    public ClickListener(String itemID, CustomEvent event,
                         List<CustomCondition> conditions, List<CustomAction> actions) {
        super(itemID, event, conditions, actions);
        switch (event.getParams().get(0)) {
            case "right":
                if (event.getParams().get(1).equals("all")) {
                    interactActions.add(Action.RIGHT_CLICK_AIR);
                    interactActions.add(Action.RIGHT_CLICK_BLOCK);
                } else if (event.getParams().get(1).equals("block")) {
                    interactActions.add(Action.RIGHT_CLICK_BLOCK);
                } else {
                    interactActions.add(Action.RIGHT_CLICK_AIR);
                }
                break;

            case "left":
                if (event.getParams().get(1).equals("all")) {
                    interactActions.add(Action.LEFT_CLICK_AIR);
                    interactActions.add(Action.LEFT_CLICK_BLOCK);
                } else if (event.getParams().get(1).equals("block"))
                    interactActions.add(Action.LEFT_CLICK_BLOCK);
                else
                    interactActions.add(Action.LEFT_CLICK_AIR);

                break;

            case "all":
                if (event.getParams().get(1).equals("all")) {
                    interactActions.add(Action.RIGHT_CLICK_AIR);
                    interactActions.add(Action.RIGHT_CLICK_BLOCK);
                    interactActions.add(Action.LEFT_CLICK_AIR);
                    interactActions.add(Action.LEFT_CLICK_BLOCK);
                } else if (event.getParams().get(1).equals("block")) {
                    interactActions.add(Action.RIGHT_CLICK_BLOCK);
                    interactActions.add(Action.LEFT_CLICK_BLOCK);
                } else {
                    interactActions.add(Action.RIGHT_CLICK_AIR);
                    interactActions.add(Action.LEFT_CLICK_AIR);
                }
                break;
        }
    }

    @EventHandler
    public void onClicked(PlayerInteractEvent event) {
        if (interactActions.contains(event.getAction())) {
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

}
