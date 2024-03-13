package io.th0rgal.oraxen.hud;

import com.jeff_media.morepersistentdatatypes.DataType;
import io.th0rgal.oraxen.OraxenPlugin;
import org.bukkit.GameMode;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityAirChangeEvent;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.persistence.PersistentDataContainer;

public class HudEvents implements Listener {

    @EventHandler
    public void onJoin(final PlayerJoinEvent event) {
        HudManager hudManager = OraxenPlugin.get().getHudManager();
        final Player player = event.getPlayer();
        final PersistentDataContainer pdc = player.getPersistentDataContainer();
        Hud hud = hudManager.hasActiveHud(player)
                ? hudManager.getActiveHud(player) : hudManager.getDefaultEnabledHuds().stream().findFirst().orElse(null);
        String hudId = hudManager.getHudID(hud);

        if (hud == null || hudId == null) return;
        if (!player.hasPermission(hud.getPerm())) return;
        if (!hudManager.getHudState(player)) return;

        pdc.set(hudManager.hudDisplayKey, DataType.STRING, hudManager.getHudID(hud));
        pdc.set(hudManager.hudToggleKey, DataType.BOOLEAN, true);
        hudManager.updateHud(player);
    }

    @EventHandler
    public void onEnterWater(final EntityAirChangeEvent event) {
        if (event.getEntityType() != EntityType.PLAYER) return;

        HudManager hudManager = OraxenPlugin.get().getHudManager();
        Player player = (Player) event.getEntity();
        Hud hud = hudManager.getActiveHud(player);

        if (hud == null || !hud.isDisabledWhilstInWater() || !hudManager.getHudState(player)) return;
        if (event.getAmount() < player.getMaximumAir()) {
            hudManager.setHudState(player, true);
            hudManager.updateHud(player);
        } else {
            hudManager.setHudState(player, false);
            hudManager.disableHud(player);
        }
    }

    @EventHandler
    public void onGameModeChange(final PlayerGameModeChangeEvent event) {
        HudManager hudManager = OraxenPlugin.get().getHudManager();
        Player player = event.getPlayer();
        Hud hud = hudManager.getActiveHud(player);
        if (hud == null) return;

        if (player.getGameMode() == GameMode.SPECTATOR && !hud.enableInSpectatorMode()) {
            hudManager.setHudState(player, false);
            hudManager.disableHud(player);
        } else {
            hudManager.setHudState(player, true);
            hudManager.updateHud(player);
        }
    }
}


