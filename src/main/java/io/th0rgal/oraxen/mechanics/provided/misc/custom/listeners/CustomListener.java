package io.th0rgal.oraxen.mechanics.provided.misc.custom.listeners;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.mechanics.provided.misc.custom.fields.CustomAction;
import io.th0rgal.oraxen.mechanics.provided.misc.custom.fields.CustomCondition;
import io.th0rgal.oraxen.mechanics.provided.misc.custom.fields.CustomEvent;
import org.bukkit.Bukkit;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;

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
        System.out.println("CREATING OBJECT WITH ACTIONS:");
    }

    public void register() {
        Bukkit.getPluginManager().registerEvents(this, OraxenPlugin.get());
        for (CustomAction action : actions)
            System.out.println("registered: " + action.getParams());
    }

    public void unregister() {
        HandlerList.unregisterAll(this);
    }

}
