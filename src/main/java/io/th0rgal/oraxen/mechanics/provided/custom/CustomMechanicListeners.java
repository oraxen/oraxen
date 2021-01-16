package io.th0rgal.oraxen.mechanics.provided.custom;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.items.OraxenItems;
import io.th0rgal.oraxen.mechanics.MechanicFactory;
import io.th0rgal.oraxen.mechanics.MechanicsManager;
import io.th0rgal.oraxen.utils.logs.Logs;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class CustomMechanicListeners {

    protected boolean registerListener(MechanicFactory factory,
                                       String eventName,
                                       CustomMechanicAction action,
                                       CustomMechanicCondition condition) {
        Listener listener;
        String[] fields = eventName.split(":");
        switch (fields[0]) {
            case "click":
                listener = new ClickListener(factory, fields, action, condition);
                break;

            default:
                return false;
        }
        MechanicsManager.registerListeners(OraxenPlugin.get(), listener);
        return true;
    }
}

abstract class AbstractListener implements Listener {

    private final MechanicFactory factory;
    protected final CustomMechanicAction action;
    protected final CustomMechanicCondition condition;

    AbstractListener(MechanicFactory factory, CustomMechanicAction action, CustomMechanicCondition condition) {
        this.factory = factory;
        this.action = action;
        this.condition = condition;
    }

    protected boolean isNotImplemented(ItemStack itemStack) {
        String itemId = OraxenItems.getIdByItem(itemStack);
        if (itemId == null)
            return true;
        ItemMeta itemMeta = itemStack.getItemMeta();
        return factory.isNotImplementedIn(itemId);
    }

}

class ClickListener extends AbstractListener {

    List<Action> actions = new ArrayList<>();

    ClickListener(MechanicFactory factory, String[] fields, CustomMechanicAction action, CustomMechanicCondition condition) {
        super(factory, action, condition);
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
        if (isNotImplemented(event.getItem()))
            return;
        if (actions.contains(event.getAction())) {
            CustomMechanicWrapper wrapper = new CustomMechanicWrapper();
            wrapper.setPlayer(event.getPlayer());
            condition.passes(wrapper);
            action.run(wrapper);
        }
    }
}