package io.th0rgal.oraxen.pack.dispatch;

import io.th0rgal.oraxen.configs.Settings;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerResourcePackStatusEvent;
import org.bukkit.event.player.PlayerToggleFlightEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PackLoadingManager implements Listener {

    private final Map<UUID, LoadingPlayer> frozenPlayers = new ConcurrentHashMap<>();

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerUpdatesPackStatus(PlayerResourcePackStatusEvent event) {
        Player player = event.getPlayer();
        switch (event.getStatus().name()) {
            case "ACCEPTED" -> frozenPlayers.computeIfAbsent(player.getUniqueId(), id -> new LoadingPlayer(player))
                    .updateFlight(Settings.DISABLE_MOVEMENT_ON_LOAD.toBool());
            // DOWNLOADED is intermediate and does not exist on the oldest supported API.
            case "DOWNLOADED" -> { }
            default -> release(player);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerMove(PlayerMoveEvent event) {
        LoadingPlayer loadingPlayer = frozenPlayers.get(event.getPlayer().getUniqueId());
        if (loadingPlayer == null) return;
        boolean disableMovement = Settings.DISABLE_MOVEMENT_ON_LOAD.toBool();
        loadingPlayer.updateFlight(disableMovement);
        if (disableMovement && event.getTo() != null) {
            if (event.getFrom().getX() != event.getTo().getX() || event.getFrom().getY() != event.getTo().getY() || event.getFrom().getZ() != event.getTo().getZ()) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player player && frozenPlayers.containsKey(player.getUniqueId()) && Settings.DISABLE_DAMAGE_ON_LOAD.toBool()) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        release(event.getPlayer());
        PackSender.forgetPack(event.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerToggleFlight(PlayerToggleFlightEvent event) {
        LoadingPlayer loadingPlayer = frozenPlayers.get(event.getPlayer().getUniqueId());
        if (loadingPlayer != null && loadingPlayer.temporaryFlight && Settings.DISABLE_MOVEMENT_ON_LOAD.toBool()) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerGameModeChange(PlayerGameModeChangeEvent event) {
        LoadingPlayer loadingPlayer = frozenPlayers.get(event.getPlayer().getUniqueId());
        // The new game mode takes ownership of flight permission after this event.
        if (loadingPlayer != null) loadingPlayer.restoreFlight();
    }

    private void release(Player player) {
        LoadingPlayer loadingPlayer = frozenPlayers.remove(player.getUniqueId());
        if (loadingPlayer != null) loadingPlayer.restoreFlight();
    }

    public void shutdown() {
        frozenPlayers.values().forEach(LoadingPlayer::restoreFlight);
        frozenPlayers.clear();
    }

    private static final class LoadingPlayer {
        private final Player player;
        private boolean temporaryFlight;

        private LoadingPlayer(Player player) {
            this.player = player;
        }

        private void updateFlight(boolean disableMovement) {
            if (!disableMovement) {
                restoreFlight();
            } else if (!player.getAllowFlight()) {
                temporaryFlight = true;
                player.setAllowFlight(true);
            }
        }

        private void restoreFlight() {
            if (!temporaryFlight) return;
            temporaryFlight = false;
            if (player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR) return;
            if (player.isFlying()) player.setFlying(false);
            player.setAllowFlight(false);
        }
    }
}
