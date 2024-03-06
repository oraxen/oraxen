package io.th0rgal.oraxen.mechanics.provided.misc.armor_effects;

import fr.euphyllia.energie.model.SchedulerType;
import io.th0rgal.oraxen.OraxenPlugin;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class ArmorEffectsTask implements Runnable {

    @Override
    public void run() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            OraxenPlugin.getScheduler().runTask(SchedulerType.SYNC, player, schedulerTaskInter -> {
                ArmorEffectsMechanic.addEffects(player);
            }, null);
        }
    }
}
