package io.th0rgal.oraxen.mechanics.provided.cosmetic.aura.aura;

import fr.euphyllia.energie.model.SchedulerTaskInter;
import fr.euphyllia.energie.model.SchedulerType;
import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.mechanics.provided.cosmetic.aura.AuraMechanic;
import org.bukkit.entity.Player;

public abstract class Aura {

    protected final AuraMechanic mechanic;
    private SchedulerTaskInter runnable;

    protected Aura(AuraMechanic mechanic) {
        this.mechanic = mechanic;
    }

    Runnable getRunnable() {
        return () -> mechanic.players.forEach(Aura.this::spawnParticles);
    }

    protected abstract void spawnParticles(Player player);

    protected abstract long getDelay();

    public void start() {
        runnable = OraxenPlugin.getScheduler().runAtFixedRate(SchedulerType.ASYNC, schedulerTaskInter -> {
            this.getRunnable().run();
        }, 0L, getDelay());
    }

    public void stop() {
        runnable.cancel();
    }


}
