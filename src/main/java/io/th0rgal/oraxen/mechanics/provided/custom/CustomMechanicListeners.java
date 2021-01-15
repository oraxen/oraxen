package io.th0rgal.oraxen.mechanics.provided.custom;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.mechanics.MechanicsManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.ArrayList;
import java.util.List;

public class CustomMechanicListeners {

    private final CustomMechanicFactory factory;

    public CustomMechanicListeners(CustomMechanicFactory factory) {
        this.factory = factory;
    }

    protected boolean registerListener(String eventName, CustomMechanicAction action, CustomMechanicCondition condition) {
        Listener listener;
        switch (eventName) {

            case "test":
                listener = null;
                break;

            default:
                return false;
        }
        MechanicsManager.registerListeners(OraxenPlugin.get(), listener);
        return true;
    }
}

abstract class AbstractListener implements Listener {

    protected final CustomMechanicAction action;
    protected final CustomMechanicCondition condition;

    AbstractListener(CustomMechanicAction action, CustomMechanicCondition condition) {
        this.action = action;
        this.condition = condition;
    }

}

class ClickListener extends AbstractListener {

    List<Action> actions = new ArrayList<>();

    ClickListener(String[] fields, CustomMechanicAction action, CustomMechanicCondition condition) {
        super(action, condition);
        if (fields[1].equals("right"))
            if (fields[2].equals("air"))
                actions.add(Action.RIGHT_CLICK_AIR);
            else if (fields[2].equals("block"))
                actions.add(Action.RIGHT_CLICK_BLOCK);
            else {
                actions.add(Action.RIGHT_CLICK_AIR);
                actions.add(Action.RIGHT_CLICK_BLOCK);
            }
        else if (fields[1].equals("left"))
            if (fields[2].equals("air"))
                actions.add(Action.LEFT_CLICK_AIR);
            else if (fields[2].equals("block"))
                actions.add(Action.LEFT_CLICK_BLOCK);
            else {
                actions.add(Action.LEFT_CLICK_AIR);
                actions.add(Action.LEFT_CLICK_BLOCK);
            }
        else {
            if (fields[2].equals("air")) {
                actions.add(Action.RIGHT_CLICK_AIR);
                actions.add(Action.LEFT_CLICK_AIR);
            } else if (fields[2].equals("block")) {
                actions.add(Action.RIGHT_CLICK_BLOCK);
                actions.add(Action.LEFT_CLICK_BLOCK);
            } else {
                actions.add(Action.RIGHT_CLICK_AIR);
                actions.add(Action.RIGHT_CLICK_BLOCK);
                actions.add(Action.LEFT_CLICK_AIR);
                actions.add(Action.LEFT_CLICK_BLOCK);
            }
        }
    }

    @EventHandler
    public void onClick(PlayerInteractEvent event) {
        if (actions.contains(event.getAction())) {
            CustomMechanicWrapper wrapper = new CustomMechanicWrapper();
            wrapper.setPlayer(event.getPlayer());
            condition.passes(wrapper);
            action.run(wrapper);
        }
    }
}