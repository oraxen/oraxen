package io.th0rgal.oraxen.mechanics.provided.cosmetic.aura.aura;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.api.scheduler.AdaptedTask;
import io.th0rgal.oraxen.api.scheduler.AdaptedTaskRunnable;
import io.th0rgal.oraxen.mechanics.MechanicsManager;
import io.th0rgal.oraxen.mechanics.provided.cosmetic.aura.AuraMechanic;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

public abstract class Aura {

    protected final AuraMechanic mechanic;
    private AdaptedTaskRunnable runnable;

    protected Aura(AuraMechanic mechanic) {
        this.mechanic = mechanic;
    }

    AdaptedTaskRunnable getRunnable() {
        return new AdaptedTaskRunnable() {
            @Override
            public void run() {
                for (Player player : mechanic.players) {
                   OraxenPlugin.get().getScheduler().runEntityTask(player, () -> Aura.this.spawnParticles(player), null);
                }
            }
        };
    }

    protected abstract void spawnParticles(Player player);

    protected abstract long getDelay();

    public void start() {
        runnable = getRunnable();
        AdaptedTask task = runnable.runTaskTimerAsynchronously(0L, getDelay());
        MechanicsManager.registerTask(mechanic.getFactory().getMechanicID(), task);
    }

    public void stop() {
        runnable.getAdaptedTask().cancel();
    }


}
