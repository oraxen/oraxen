package io.th0rgal.oraxen.mechanics.provided.misc.armor_effects;

import io.th0rgal.oraxen.utils.SchedulerUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class ArmorEffectsTask implements Runnable {

    private SchedulerUtil.ScheduledTask scheduledTask;

    public SchedulerUtil.ScheduledTask start(long initialDelay, long period) {
        scheduledTask = SchedulerUtil.runTaskTimer(initialDelay, period, this);
        return scheduledTask;
    }

    public void cancel() {
        if (scheduledTask != null) {
            scheduledTask.cancel();
            scheduledTask = null;
        }
    }

    @Override
    public void run() {
        // Schedule on each player's region thread for Folia compatibility
        for (Player player : Bukkit.getOnlinePlayers()) {
            SchedulerUtil.runForEntity(player, () -> ArmorEffectsMechanic.addEffects(player));
        }
    }
}
