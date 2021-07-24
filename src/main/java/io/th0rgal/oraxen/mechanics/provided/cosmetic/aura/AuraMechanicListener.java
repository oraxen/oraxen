package io.th0rgal.oraxen.mechanics.provided.cosmetic.aura;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.util.HashMap;
import java.util.Map;

public class AuraMechanicListener implements Listener {

    private final AuraMechanicFactory factory;
    private final Map<Player, AuraMechanic> registeredPlayers;

    public AuraMechanicListener(AuraMechanicFactory factory) {
        this.factory = factory;
        registeredPlayers = new HashMap<>();
    }

    @EventHandler(priority = EventPriority.LOW)
    public void register(Player player, AuraMechanic mechanic) {
        registeredPlayers.remove(player);
    }

}
