package io.th0rgal.oraxen.mechanics.provided.cosmetic.aura.aura;

import io.th0rgal.oraxen.mechanics.provided.cosmetic.aura.AuraMechanic;
import org.bukkit.entity.Player;

public class RingAura extends Aura {

    public RingAura(AuraMechanic mechanic) {
        super(mechanic);
    }

    @Override
    protected void spawnParticles(Player player) {
        for (double i = 0; i < 25; i++) {
            double advancement = Math.PI * 2D * i / 25;
            player.getWorld().spawnParticle(mechanic.particle, player.getLocation().add(
                    0.5D * Math.cos(advancement), 2, 0.5D * Math.sin(advancement)
            ), 1, 0, 0, 0, 0);
        }
    }

    @Override
    protected long getDelay() {
        return 10L;
    }
}
