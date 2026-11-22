package io.th0rgal.oraxen.mechanics.provided.cosmetic.aura;

import io.th0rgal.oraxen.mechanics.Mechanic;
import io.th0rgal.oraxen.mechanics.MechanicFactory;
import io.th0rgal.oraxen.mechanics.provided.cosmetic.aura.aura.Aura;
import io.th0rgal.oraxen.mechanics.provided.cosmetic.aura.aura.HelixAura;
import io.th0rgal.oraxen.mechanics.provided.cosmetic.aura.aura.RingAura;
import io.th0rgal.oraxen.mechanics.provided.cosmetic.aura.aura.SimpleAura;
import io.th0rgal.oraxen.utils.logs.Logs;
import io.th0rgal.oraxen.utils.wrappers.ParticleWrapper;
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
    
    // Lock object for atomic add/remove + start/stop operations
    private final Object auraLock = new Object();

    public AuraMechanic(MechanicFactory mechanicFactory, ConfigurationSection section) {
        super(mechanicFactory, section);
        String particleName = section.getString("particle");
        String type = section.getString("type");
        particle = particleName == null || particleName.isBlank() ? null : ParticleWrapper.resolve(particleName);
        if (particle == null) {
            Logs.logError("Aura mechanic of item " + getItemID() + " has no particle set, the aura is disabled");
            aura = null;
        } else switch (type == null ? "" : type) {
            case "simple" -> aura = new SimpleAura(this);
            case "ring" -> aura = new RingAura(this);
            case "helix" -> aura = new HelixAura(this);
            default -> {
                Logs.logError("Aura mechanic of item " + getItemID() + " has unknown type '" + type
                        + "', expected simple, ring or helix. The aura is disabled");
                aura = null;
            }
        }
        players = ConcurrentHashMap.newKeySet();
    }

    public void add(Player player) {
        // Synchronize to make add + size check atomic for Folia thread safety
        synchronized (auraLock) {
            boolean wasEmpty = players.isEmpty();
            players.add(player);
            if (wasEmpty && aura != null)
                aura.start();
        }
    }

    public void remove(Player player) {
        // Synchronize to make remove + isEmpty check atomic for Folia thread safety
        synchronized (auraLock) {
            players.remove(player);
            if (aura != null) {
                aura.removePlayer(player);
                if (players.isEmpty())
                    aura.stop();
            }
        }
    }

    /**
     * Stops the aura task. Called during mechanic unload/reload.
     */
    public void stopAura() {
        synchronized (auraLock) {
            if (aura != null) aura.stop();
            players.clear();
        }
    }

}
