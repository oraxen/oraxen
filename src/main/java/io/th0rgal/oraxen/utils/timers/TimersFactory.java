package io.th0rgal.oraxen.utils.timers;

import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class TimersFactory {

    private final long delay;
    private final TimeUnit timeUnit;
    private final Map<UUID, Timer> timersPerUUID = new HashMap<>();

    public TimersFactory(int delay) {
        this(delay, TimeUnit.MILLISECONDS);
    }

    public TimersFactory(long delay) {
        this(delay, TimeUnit.MILLISECONDS);
    }

    public TimersFactory(int delay, TimeUnit timeUnit) {
        this.delay = delay;
        this.timeUnit = timeUnit;
    }

    public TimersFactory(long delay, TimeUnit timeUnit) {
        this.delay = delay;
        this.timeUnit = timeUnit;
    }

    public Timer getTimer(Player player) {
        UUID playerUniqueID = player.getUniqueId();
        if (!timersPerUUID.containsKey(playerUniqueID))
            timersPerUUID.put(playerUniqueID, new Timer(delay, timeUnit));
        return timersPerUUID.get(playerUniqueID);
    }

}
