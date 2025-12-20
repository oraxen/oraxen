package io.th0rgal.oraxen.mechanics.provided.cosmetic.aura;

import io.th0rgal.oraxen.mechanics.Mechanic;
import io.th0rgal.oraxen.mechanics.MechanicFactory;
import io.th0rgal.oraxen.mechanics.provided.cosmetic.aura.aura.Aura;
import io.th0rgal.oraxen.mechanics.provided.cosmetic.aura.aura.HelixAura;
import io.th0rgal.oraxen.mechanics.provided.cosmetic.aura.aura.RingAura;
import io.th0rgal.oraxen.mechanics.provided.cosmetic.aura.aura.SimpleAura;
import org.bukkit.Particle;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class AuraMechanic extends Mechanic {

    // Use thread-safe set for Folia compatibility (concurrent region thread access)
    public final Set<Player> players;
    public final Particle particle;
    private final Aura aura;

    public AuraMechanic(MechanicFactory mechanicFactory, ConfigurationSection section) {
        super(mechanicFactory, section);
        particle = Particle.valueOf(section.getString("particle"));
        switch (section.getString("type")) {
            case "simple" -> aura = new SimpleAura(this);
            case "ring" -> aura = new RingAura(this);
            case "helix" -> aura = new HelixAura(this);
            default -> aura = null;
        }
        players = ConcurrentHashMap.newKeySet();
    }

    public void add(Player player) {
        players.add(player);
        if (players.size() == 1 && aura != null)
            aura.start();
    }

    public void remove(Player player) {
        players.remove(player);
        if (players.isEmpty() && aura != null)
            aura.stop();
    }

    /**
     * Stops the aura task. Called during mechanic unload/reload.
     */
    public void stopAura() {
        if (aura != null) aura.stop();
        players.clear();
    }

}
