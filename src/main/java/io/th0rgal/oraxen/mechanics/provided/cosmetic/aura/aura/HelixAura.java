package io.th0rgal.oraxen.mechanics.provided.cosmetic.aura.aura;

import io.th0rgal.oraxen.mechanics.provided.cosmetic.aura.AuraMechanic;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public class HelixAura extends Aura {

    public HelixAura(AuraMechanic mechanic) {
        super(mechanic);
    }

    private float y = 1;

    @Override
    protected void spawnParticles(Player player) {
        y += 0.085;
        y %= 3.5;
        int radius = 2;
        float x = (float) (radius * Math.cos(y * 8));
        float z = (float) (radius * Math.sin(y * 8));
        Location loc = player.getLocation().clone().add(x / (1.5 * y), y - 1, z / (1.5 * y));
        player.spawnParticle(mechanic.particle,
                loc, 0, 0, 0, 0, 1);
    }

    @Override
    protected long getDelay() {
        return 1L;
    }
}
