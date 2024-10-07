package io.th0rgal.oraxen.mechanics.provided.misc.armor_effects;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.api.scheduler.AdaptedTaskRunnable;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class ArmorEffectsTask extends AdaptedTaskRunnable {

    @Override
    public void run() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            OraxenPlugin.get().getScheduler().runEntityTask(player, () -> {
                ArmorEffectsMechanic.addEffects(player);
            }, null);
        }
    }
}
