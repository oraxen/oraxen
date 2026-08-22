package io.th0rgal.oraxen.mechanics.provided.cosmetic.aura.aura;

import io.th0rgal.oraxen.mechanics.provided.cosmetic.aura.AuraMechanic;
import io.th0rgal.oraxen.utils.SchedulerUtil;
import org.bukkit.entity.Player;

import java.util.concurrent.atomic.AtomicLong;

public abstract class Aura {

    protected final AuraMechanic mechanic;
    private final AtomicLong runGeneration = new AtomicLong();
    private SchedulerUtil.ScheduledTask scheduledTask;

    protected Aura(AuraMechanic mechanic) {
        this.mechanic = mechanic;
    }

    protected abstract void spawnParticles(Player player);

    protected abstract long getDelay();

    /** Hook for subclasses tracking per-player state; called when a player stops wearing the aura. */
    protected void onPlayerRemoved(Player player) {
    }

    /** Hook for subclasses tracking per-player state; called when the aura task stops. */
    protected void onStopped() {
    }

    public void start() {
        // Use async timer to iterate players, then schedule particle spawning
        // on each player's region thread for Folia compatibility.
        long generation = runGeneration.incrementAndGet();
        scheduledTask = SchedulerUtil.runTaskTimerAsync(0L, getDelay(), () -> {
            for (Player player : mechanic.players) {
                SchedulerUtil.runForEntity(player, () -> {
                    if (runGeneration.get() != generation || !mechanic.players.contains(player)) return;
                    spawnParticles(player);
                });
            }
        });
    }

    public void stop() {
        runGeneration.incrementAndGet();
        if (scheduledTask != null) {
            scheduledTask.cancel();
            scheduledTask = null;
        }
        onStopped();
    }

    public void removePlayer(Player player) {
        onPlayerRemoved(player);
    }
}
