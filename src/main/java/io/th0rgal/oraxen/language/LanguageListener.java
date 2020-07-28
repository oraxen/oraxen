package io.th0rgal.oraxen.language;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class LanguageListener implements Listener {

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        LanguageProvider.updateLanguageOf(event.getPlayer());
    }

}
