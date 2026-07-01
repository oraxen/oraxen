package io.th0rgal.oraxen.mechanics.provided.misc.custom.listeners;

import io.th0rgal.oraxen.api.OraxenItems;
import io.th0rgal.oraxen.mechanics.provided.misc.custom.fields.CustomEvent;
import io.th0rgal.oraxen.utils.actions.ClickAction;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class ClickListener extends CustomListener {

    private final Set<Action> interactActions = new HashSet<>();

    public ClickListener(String itemID, long cooldown, CustomEvent event, ClickAction clickAction) {
        super(itemID, cooldown, event, clickAction);
        List<String> params = event.getParams();
        if (params.size() > 2)
            throw new IllegalArgumentException("CLICK events accept at most two parameters: CLICK:<right|left|all>:<air|block|all>");

        String button = params.isEmpty() ? "all" : params.get(0).toLowerCase(Locale.ROOT);
        String target = params.size() < 2 ? "all" : params.get(1).toLowerCase(Locale.ROOT);

        switch (button) {
            case "right": addRightClickActions(target); break;
            case "left": addLeftClickActions(target); break;
            case "all":
                addRightClickActions(target);
                addLeftClickActions(target);
                break;
            default:
                throw new IllegalArgumentException("Unexpected CLICK button: " + params.get(0));
        }
    }

    private void addRightClickActions(String target) {
        switch (target) {
            case "all":
                interactActions.add(Action.RIGHT_CLICK_AIR);
                interactActions.add(Action.RIGHT_CLICK_BLOCK);
                break;
            case "block":
                interactActions.add(Action.RIGHT_CLICK_BLOCK);
                break;
            case "air":
                interactActions.add(Action.RIGHT_CLICK_AIR);
                break;
            default:
                throw new IllegalArgumentException("Unexpected CLICK target: " + target);
        }
    }

    private void addLeftClickActions(String target) {
        switch (target) {
            case "all":
                interactActions.add(Action.LEFT_CLICK_AIR);
                interactActions.add(Action.LEFT_CLICK_BLOCK);
                break;
            case "block":
                interactActions.add(Action.LEFT_CLICK_BLOCK);
                break;
            case "air":
                interactActions.add(Action.LEFT_CLICK_AIR);
                break;
            default:
                throw new IllegalArgumentException("Unexpected CLICK target: " + target);
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
