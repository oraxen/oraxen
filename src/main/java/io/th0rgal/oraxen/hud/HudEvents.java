package io.th0rgal.oraxen.hud;

import com.jeff_media.morepersistentdatatypes.DataType;
import io.th0rgal.oraxen.OraxenPlugin;
import org.apache.commons.lang3.Range;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityAirChangeEvent;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.persistence.PersistentDataContainer;
import org.checkerframework.common.value.qual.IntRange;

import java.util.Objects;

public class HudEvents implements Listener {

    @EventHandler
    public void onJoin(final PlayerJoinEvent event) {
        HudManager hudManager = OraxenPlugin.get().getHudManager();
        final Player player = event.getPlayer();
        final PersistentDataContainer pdc = player.getPersistentDataContainer();
        Hud hud = hudManager.getActiveHudForPlayer(player) != null
                ? hudManager.getActiveHudForPlayer(player) : hudManager.getDefaultEnabledHuds().stream().findFirst().orElse(null);
        String hudId = hudManager.getHudID(hud);

        if (hud == null || hudId == null) return;
        if (!player.hasPermission(hud.getPerm())) return;
        if (!hudManager.getHudStateForPlayer(player)) return;

        pdc.set(hudManager.hudDisplayKey, DataType.STRING, hudManager.getHudID(hud));
        pdc.set(hudManager.hudToggleKey, DataType.BOOLEAN, true);
        hudManager.updateHud(player);
    }

    @EventHandler
    public void onEnterWater(final EntityAirChangeEvent event) {
        if (event.getEntityType() != EntityType.PLAYER) return;

        HudManager hudManager = OraxenPlugin.get().getHudManager();
        Player player = (Player) event.getEntity();
        Hud hud = hudManager.getActiveHudForPlayer(player);

        if (hud == null || !hud.isDisabledWhilstInWater() || !hudManager.getHudStateForPlayer(player)) return;
        if (Range.between(0, player.getMaximumAir()).contains(event.getAmount())) {
            hudManager.setHudStateForPlayer(player, true);
            hudManager.updateHud(player);
        } else {
            hudManager.setHudStateForPlayer(player, false);
            hudManager.disableHud(player);
        }
    }

    @EventHandler
    public void onGameModeChange(final PlayerGameModeChangeEvent event) {
        HudManager hudManager = OraxenPlugin.get().getHudManager();
        Player player = event.getPlayer();
        Hud hud = hudManager.getActiveHudForPlayer(player);
        if (hud == null || hud.enableInSpectatorMode()) return;
        if (player.getGameMode() == GameMode.SPECTATOR) {
            hudManager.setHudStateForPlayer(player, false);
            hudManager.disableHud(player);
        } else {
            hudManager.setHudStateForPlayer(player, true);
            hudManager.updateHud(player);
        }
    }
}


