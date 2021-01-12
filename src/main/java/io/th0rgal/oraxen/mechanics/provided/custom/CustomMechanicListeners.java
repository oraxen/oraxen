package io.th0rgal.oraxen.mechanics.provided.custom;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.mechanics.MechanicsManager;
import org.bukkit.event.Listener;

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