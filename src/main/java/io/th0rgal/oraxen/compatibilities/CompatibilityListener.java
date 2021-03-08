package io.th0rgal.oraxen.compatibilities;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.OraxenVersion;

import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.event.server.PluginEnableEvent;

public class CompatibilityListener implements Listener {

    public CompatibilityListener() {
        Bukkit.getPluginManager().registerEvents(this, OraxenPlugin.get());
    }

    @EventHandler
    public void onPluginEnable(PluginEnableEvent event) {
        OraxenVersion.PLUGIN.updatePlugin(event.getPlugin(), true);
        CompatibilitiesManager.enableCompatibility(event.getPlugin().getName());
    }

    @EventHandler
    public void onPluginDisable(PluginDisableEvent event) {
        OraxenVersion.PLUGIN.updatePlugin(event.getPlugin(), false);
        CompatibilitiesManager.disableCompatibility(event.getPlugin().getName());
    }

}
