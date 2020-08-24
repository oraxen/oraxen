package io.th0rgal.oraxen.command.commands;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import io.th0rgal.oraxen.event.command.OraxenCommandEvent;

public class CommandListener implements Listener {

    @EventHandler
    public void onCommandCreation(OraxenCommandEvent event) {

        event
            .add(Help.info())
            .add(Debug.info())
            .add(Give.info())
            .add(ItemPanel.info())
            .add(Pack.info())
            .add(Recipe.info())
            .add(Reload.info())
            .add(Repair.info());

    }

}
