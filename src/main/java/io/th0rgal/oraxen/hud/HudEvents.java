package io.th0rgal.oraxen.hud;

import com.jeff_media.morepersistentdatatypes.DataType;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.persistence.PersistentDataContainer;

public class HudEvents implements Listener {

    private final HudManager manager;

    public HudEvents(HudManager manager) {
        this.manager = manager;
    }

    //TODO This probably isnt needed anymore but could be useful in some cases.
    @EventHandler
    public void onJoin(final PlayerJoinEvent event) {
        final Player player = event.getPlayer();
        final PersistentDataContainer pdc = player.getPersistentDataContainer();
        Hud hud = manager.getActiveHudForPlayer(player) != null ? manager.getActiveHudForPlayer(player) : manager.getDefaultEnabledHuds().stream().findFirst().orElse(null);
        String hudId = manager.getHudID(hud);

        if (hud == null || hudId == null) return;
        if (!player.hasPermission(hud.getHudPerm())) return;
        if (!manager.getHudStateForPlayer(player)) return;
        pdc.set(manager.hudDisplayKey, DataType.STRING, manager.getHudID(hud));
        pdc.set(manager.hudToggleKey, DataType.BOOLEAN, true);
        manager.updateHud(player);
    }

    @EventHandler
    public void onEnterWater(final PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (event.getTo() == null) return;
        boolean fromBlockIsWater = event.getFrom().getBlock().getType() == Material.WATER;
        boolean toBlockIsWater = event.getTo().getBlock().getType() == Material.WATER;
        Hud hud = manager.getActiveHudForPlayer(player);

        //TODO Improve this check.
        if (hud == null || !hud.isDisabledWhilstInWater() || !manager.getHudStateForPlayer(player)) return;
        if (!player.isInWater() && (fromBlockIsWater && !toBlockIsWater)) {
            manager.setHudStateForPlayer(player, true);
            manager.updateHud(player);
        } else if ((!fromBlockIsWater && toBlockIsWater) && player.isInWater()) {
            manager.setHudStateForPlayer(player, false);
            manager.disableHud(player);
        }
    }

    @EventHandler
    public void onGameModeChange(final PlayerGameModeChangeEvent event) {
        Player player = event.getPlayer();
        Hud hud = manager.getActiveHudForPlayer(player);

        if (hud != null && hud.getGameModes().contains(event.getNewGameMode())) {
            manager.setHudStateForPlayer(player, true);
            manager.updateHud(player);
        }
    }
}


