package org.playuniverse.snowypine.listener;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerLocaleChangeEvent;
import org.playuniverse.snowypine.language.LanguageProvider;

public final class PlayerListener implements Listener {

	@EventHandler
	public void onLocaleChange(PlayerLocaleChangeEvent event) {
		LanguageProvider.updateLanguageOf(event.getPlayer(), event.getLocale());
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	public void onPlayerJoin(PlayerJoinEvent event) {
		LanguageProvider.updateLanguageOf(event.getPlayer());
	}

}
