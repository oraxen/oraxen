package io.th0rgal.oraxen.mechanics.provided.misc.custom.listeners;

import io.th0rgal.oraxen.api.OraxenItems;
import io.th0rgal.oraxen.mechanics.provided.misc.custom.fields.CustomEvent;
import io.th0rgal.oraxen.utils.actions.ClickAction;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import java.util.HashSet;
import java.util.Set;

public class ClickListener extends CustomListener {

    private final Set<Action> interactActions = new HashSet<>();

    public ClickListener(String itemID, long cooldown, CustomEvent event, ClickAction clickAction) {
        super(itemID, cooldown, event, clickAction);
        switch (event.getParams().get(0)) {
            case "right":
                if (event.getParams().get(1).equals("all")) {
                    interactActions.add(Action.RIGHT_CLICK_AIR);
                    interactActions.add(Action.RIGHT_CLICK_BLOCK);
                } else if (event.getParams().get(1).equals("block")) interactActions.add(Action.RIGHT_CLICK_BLOCK);
                else
                    interactActions.add(Action.RIGHT_CLICK_AIR);
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

            default:
                throw new IllegalStateException("Unexpected value: " + event.getParams().get(0));
        }
    }

    @EventHandler
    public void onClicked(PlayerInteractEvent event) {
        if (interactActions.contains(event.getAction())) {
            ItemStack item = event.getItem();
            if (!itemID.equals(OraxenItems.getIdByItem(item)))
                return;
            perform(event.getPlayer(), item);
        }
    }

}
