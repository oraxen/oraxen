package io.th0rgal.oraxen.mechanics.provided.cosmetic.aura.aura;

import io.th0rgal.oraxen.mechanics.provided.cosmetic.aura.AuraMechanic;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class HelixAura extends Aura {

    public HelixAura(AuraMechanic mechanic) {
        super(mechanic);
    }

    // One aura instance is shared by every player wearing the item, and
    // spawnParticles runs concurrently on each player's region thread (Folia);
    // keep the helix phase per player instead of racing on a shared field.
    private final Map<UUID, Double> phases = new ConcurrentHashMap<>();

    @Override
    protected void spawnParticles(Player player) {
        double y = phases.merge(player.getUniqueId(), 0.085, (previous, increment) -> (previous % Math.PI) + increment);
        int radius = 2;
        double x = radius * Math.cos(2 * y);
        double z = radius * Math.sin(2 * y);
        Location location = player.getLocation().clone().add(x / (1 + y * y * 0.75), y, z / (1 + y * y * 0.75));
        player.getWorld().spawnParticle(mechanic.particle, location, 0, 0, 0, 0, 1);
    }

    @Override
    protected void onPlayerRemoved(Player player) {
        phases.remove(player.getUniqueId());
    }

    @Override
    protected void onStopped() {
        phases.clear();
    }

    @Override
    protected long getDelay() {
        return 1L;
    }
}
