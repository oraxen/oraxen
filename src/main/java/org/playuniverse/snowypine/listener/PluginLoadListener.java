package org.playuniverse.snowypine.listener;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.server.PluginEnableEvent;
import org.playuniverse.snowypine.Snowypine;
import org.playuniverse.snowypine.compatibility.CompatibilityHandler;
import org.bukkit.event.server.PluginDisableEvent;

public class PluginLoadListener implements Listener {

	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	public void onEnable(PluginEnableEvent event) {
		Snowypine.SETTINGS.updatePlugin(event.getPlugin(), true);
		CompatibilityHandler.handleSettingsUpdate(Snowypine.SETTINGS);
	}

	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	public void onDisable(PluginDisableEvent event) {
		Snowypine.SETTINGS.updatePlugin(event.getPlugin(), false);
		CompatibilityHandler.handleSettingsUpdate(Snowypine.SETTINGS);
	}

}
