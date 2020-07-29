package io.th0rgal.oraxen.command.commands;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import io.th0rgal.oraxen.event.command.OraxenCommandEvent;

public class CommandListener implements Listener {
    
    @EventHandler
    public void onCommandCreation(OraxenCommandEvent event) {
        
        event.add(Reload.build());
        
    }

}
