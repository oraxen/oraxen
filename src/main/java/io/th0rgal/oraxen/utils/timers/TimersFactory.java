package io.th0rgal.oraxen.utils.timers;

import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class TimersFactory {

    private final long delay;
    private final Map<UUID, Timer> timersPerUUID = new HashMap<>();

    public TimersFactory(int delay) {
        this.delay = delay;
    }

    public TimersFactory(long delay) {
       this.delay = delay;
    }

    public Timer getTimer(Player player) {
        UUID playerUniqueID = player.getUniqueId();
        return timersPerUUID.computeIfAbsent(playerUniqueID, uuid -> new Timer(delay));
    }

}
