package io.th0rgal.oraxen.mechanics.provided.cosmetic.aura.aura;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.mechanics.MechanicsManager;
import io.th0rgal.oraxen.mechanics.provided.cosmetic.aura.AuraMechanic;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

public abstract class Aura {

    protected final AuraMechanic mechanic;
    private BukkitRunnable runnable;

    protected Aura(AuraMechanic mechanic) {
        this.mechanic = mechanic;
    }

    BukkitRunnable getRunnable() {
        return new BukkitRunnable() {
            @Override
            public void run() {
                mechanic.players.forEach(Aura.this::spawnParticles);
            }
        };
    }

    protected abstract void spawnParticles(Player player);

    protected abstract long getDelay();

    public void start() {
        runnable = getRunnable();
        BukkitTask task = runnable.runTaskTimerAsynchronously(OraxenPlugin.get(), 0L, getDelay());
        MechanicsManager.registerTask(mechanic.getFactory().getMechanicID(), task);
    }

    public void stop() {
        runnable.cancel();
    }


}
