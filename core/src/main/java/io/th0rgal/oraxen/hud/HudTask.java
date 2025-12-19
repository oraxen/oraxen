package io.th0rgal.oraxen.hud;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.utils.EntityUtils;
import io.th0rgal.oraxen.utils.SchedulerUtil;
import io.th0rgal.oraxen.utils.logs.Logs;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.List;

public class HudTask implements Runnable {

    private final HudManager manager = OraxenPlugin.get().getHudManager();
    private SchedulerUtil.ScheduledTask scheduledTask;

    private List<? extends Player> hudEnabledPlayers() {
        return Bukkit.getOnlinePlayers().stream().filter(manager::getHudState).toList();
    }

    public void start(long delay, long period) {
        scheduledTask = SchedulerUtil.runTaskTimer(delay, period, this);
    }

    public void cancel() {
        if (scheduledTask != null) {
            scheduledTask.cancel();
            scheduledTask = null;
        }
    }

    @Override
    public void run() {
        List<? extends Player> enabled = hudEnabledPlayers();
        for (Player player : enabled) {
            Hud hud = manager.hasActiveHud(player) ? manager.getActiveHud(player) : manager.getDefaultEnabledHuds().stream().findFirst().orElse(null);

            if (hud == null) {
                Logs.logWarning("[HUD] No HUD found for player " + player.getName());
                continue;
            }
            if (manager.getHudID(hud) == null) {
                Logs.logWarning("[HUD] HUD ID is null for player " + player.getName());
                continue;
            }
            if (hud.disableWhilstInWater() && EntityUtils.isUnderWater(player)) {
                continue;
            }
            if (!player.hasPermission(hud.getPerm())) {
                Logs.logWarning("[HUD] Player " + player.getName() + " doesn't have permission: " + hud.getPerm());
                continue;
            }

            manager.updateHud(player);
        }
    }
}
