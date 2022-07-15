package io.th0rgal.oraxen.hud;

import com.jeff_media.morepersistentdatatypes.DataType;
import io.th0rgal.oraxen.OraxenPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.persistence.PersistentDataContainer;

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
    public void onEnterWater(final PlayerMoveEvent event) {
        HudManager hudManager = OraxenPlugin.get().getHudManager();
        Player player = event.getPlayer();
        Block fromBlock = event.getFrom().getBlock();
        Block toBlock = Objects.requireNonNull(event.getTo()).getBlock();
        boolean fromBlockIsWater = event.getFrom().getBlock().getType() == Material.WATER;
        boolean toBlockIsWater = event.getTo().getBlock().getType() == Material.WATER;
        if (fromBlock.equals(toBlock)) return;
        Hud hud = hudManager.getActiveHudForPlayer(player);

        //TODO Improve this check.
        if (hud == null || !hud.isDisabledWhilstInWater() || !hudManager.getHudStateForPlayer(player)) return;
        if (!player.isInWater() && (fromBlockIsWater && !toBlockIsWater)) {
            hudManager.setHudStateForPlayer(player, true);
            hudManager.updateHud(player);
        } else if ((!fromBlockIsWater && toBlockIsWater) && player.isInWater()) {
            hudManager.setHudStateForPlayer(player, false);
            hudManager.disableHud(player);
        }
    }

    @EventHandler
    public void onGameModeChange(final PlayerGameModeChangeEvent event) {
        HudManager hudManager = OraxenPlugin.get().getHudManager();
        Player player = event.getPlayer();
        Hud hud = hudManager.getActiveHudForPlayer(player);

        if (hud != null && hud.getGameModes().contains(event.getNewGameMode())) {
            Bukkit.broadcastMessage("ss");
            hudManager.setHudStateForPlayer(player, true);
            hudManager.updateHud(player);
        } else if (hud != null && !hud.getGameModes().contains(event.getNewGameMode())) {
            Bukkit.broadcastMessage("sdasdasd");
            hudManager.setHudStateForPlayer(player, false);
            hudManager.disableHud(player);
        }
    }
}


