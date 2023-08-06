package io.th0rgal.oraxen.mechanics.provided.cosmetic.aura.aura;

import io.th0rgal.oraxen.mechanics.provided.cosmetic.aura.AuraMechanic;
import org.bukkit.entity.Player;

public class SimpleAura extends Aura {
    public SimpleAura(AuraMechanic mechanic) {
        super(mechanic);
    }

    @Override
    protected void spawnParticles(Player player) {
        player.getWorld().spawnParticle(mechanic.particle, player.getLocation().add(0, 1, 0), 2);
    }

    @Override
    protected long getDelay() {
        return 15L;
    }
}
