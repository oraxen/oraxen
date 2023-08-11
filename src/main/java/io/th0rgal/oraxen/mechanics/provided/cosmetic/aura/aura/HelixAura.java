package io.th0rgal.oraxen.mechanics.provided.cosmetic.aura.aura;

import io.th0rgal.oraxen.mechanics.provided.cosmetic.aura.AuraMechanic;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public class HelixAura extends Aura {

    public HelixAura(AuraMechanic mechanic) {
        super(mechanic);
    }

    private double y = 0;

    @Override
    protected void spawnParticles(Player player) {
        y %= Math.PI;
        y += 0.085;
        int radius = 2;
        double x = radius * Math.cos(2 * y);
        double z = radius * Math.sin(2 * y);
        Location location = player.getLocation().clone().add(x / (1 + y * y * 0.75), y, z / (1 + y * y * 0.75));
        player.getWorld().spawnParticle(mechanic.particle, location, 0, 0, 0, 0, 1);
    }

    @Override
    protected long getDelay() {
        return 1L;
    }
}
