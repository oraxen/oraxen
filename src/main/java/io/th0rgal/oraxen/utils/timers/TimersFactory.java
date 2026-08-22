package io.th0rgal.oraxen.utils.timers;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.bukkit.entity.Player;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

public class TimersFactory {

    /**
     * Grace period kept on top of the timer delay before an idle player's timer is evicted.
     * Evicting a timer resets its cooldown, so entries are only dropped once the cooldown
     * they track can no longer be running.
     */
    private static final Duration EVICTION_GRACE = Duration.ofMinutes(1);

    private final long delay;
    private final ConcurrentMap<UUID, Timer> timersPerUUID;

    public TimersFactory(int delay) {
        this((long) delay);
    }

    public TimersFactory(long delay) {
        this(delay, EVICTION_GRACE);
    }

    TimersFactory(long delay, Duration evictionGrace) {
        this.delay = delay;
        // The long/TimeUnit overload is used instead of the Duration one so this stays compatible
        // with the older Guava versions shipped by the oldest supported server versions.
        long nonNegativeDelay = Math.max(delay, 0L);
        long graceMillis = evictionGrace.toMillis();
        long expireAfterAccessMillis = nonNegativeDelay > Long.MAX_VALUE - graceMillis
                ? Long.MAX_VALUE
                : nonNegativeDelay + graceMillis;
        Cache<UUID, Timer> cache = CacheBuilder.newBuilder()
                .expireAfterAccess(expireAfterAccessMillis, TimeUnit.MILLISECONDS)
                .build();
        this.timersPerUUID = cache.asMap();
    }

    public Timer getTimer(Player player) {
        UUID playerUniqueID = player.getUniqueId();
        return timersPerUUID.computeIfAbsent(playerUniqueID, uuid -> new Timer(delay));
    }

    int cachedTimerCount() {
        return timersPerUUID.size();
    }

}
