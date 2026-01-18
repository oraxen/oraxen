package io.th0rgal.oraxen.pack.dispatch;

import io.th0rgal.oraxen.config.Settings;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerResourcePackStatusEvent;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PackLoadingManager implements Listener {

    private final Set<UUID> frozenPlayers = ConcurrentHashMap.newKeySet();

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerUpdatesPackStatus(PlayerResourcePackStatusEvent event) {
        Player player = event.getPlayer();
        switch (event.getStatus()) {
            case ACCEPTED -> frozenPlayers.add(player.getUniqueId());
            case SUCCESSFULLY_LOADED, DECLINED, FAILED_DOWNLOAD -> frozenPlayers.remove(player.getUniqueId());
            default -> {
                 // Check for "DOWNLOADED" status (1.20.3+) using string comparison to avoid
                 // runtime errors on older server versions if compiled against newer API.
                 // We don't want to unfreeze the player at this intermediate stage.
                 String statusName = event.getStatus().name();
                 if (!statusName.equals("DOWNLOADED")) {
                     frozenPlayers.remove(player.getUniqueId());
                 }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerMove(PlayerMoveEvent event) {
        if (frozenPlayers.contains(event.getPlayer().getUniqueId()) && Settings.DISABLE_MOVEMENT_ON_LOAD.toBool()) {
            if (event.getFrom().getX() != event.getTo().getX() || event.getFrom().getY() != event.getTo().getY() || event.getFrom().getZ() != event.getTo().getZ()) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player player && frozenPlayers.contains(player.getUniqueId()) && Settings.DISABLE_DAMAGE_ON_LOAD.toBool()) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        frozenPlayers.remove(event.getPlayer().getUniqueId());
    }
}
