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

import java.util.HashSet;
import java.util.Set;

public class AuraMechanic extends Mechanic {

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
        players = new HashSet<>();
    }

    public void add(Player player) {
        players.add(player);
        if (players.size() == 1)
            aura.start();
    }

    public void remove(Player player) {
        players.remove(player);
        if (players.isEmpty())
            aura.stop();
    }

}
