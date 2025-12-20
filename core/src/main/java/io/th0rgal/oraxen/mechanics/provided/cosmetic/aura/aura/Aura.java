package io.th0rgal.oraxen.mechanics.provided.cosmetic.aura.aura;

import io.th0rgal.oraxen.mechanics.provided.cosmetic.aura.AuraMechanic;
import io.th0rgal.oraxen.utils.SchedulerUtil;
import org.bukkit.entity.Player;

public abstract class Aura {

    protected final AuraMechanic mechanic;
    private SchedulerUtil.ScheduledTask scheduledTask;

    protected Aura(AuraMechanic mechanic) {
        this.mechanic = mechanic;
    }

    protected abstract void spawnParticles(Player player);

    protected abstract long getDelay();

    public void start() {
        // Use async timer to iterate players, then schedule particle spawning
        // on each player's region thread for Folia compatibility
        scheduledTask = SchedulerUtil.runTaskTimerAsync(0L, getDelay(), () -> {
            for (Player player : mechanic.players) {
                SchedulerUtil.runForEntity(player, () -> spawnParticles(player));
            }
        });
    }

    public void stop() {
        if (scheduledTask != null) {
            scheduledTask.cancel();
            scheduledTask = null;
        }
    }
}
