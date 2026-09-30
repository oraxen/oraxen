package io.th0rgal.oraxen.utils.breaker;

import org.bukkit.event.player.PlayerInteractEvent;

/**
 * Marks the left-click {@link PlayerInteractEvent} that {@link BreakerSystem} fires while a
 * custom break is already underway. Oraxen click actions have already run on the player's
 * real interact, so listeners should ignore this copy.
 */
public final class SyntheticBlockInteract {

    private static final ThreadLocal<Boolean> ACTIVE = new ThreadLocal<>();

    private SyntheticBlockInteract() {
    }

    public static void call(PlayerInteractEvent event) {
        ACTIVE.set(Boolean.TRUE);
        try {
            event.callEvent();
        } finally {
            ACTIVE.remove();
        }
    }

    public static boolean isActive() {
        return Boolean.TRUE.equals(ACTIVE.get());
    }
}
