package io.th0rgal.oraxen.utils.wrappers;

import io.th0rgal.oraxen.utils.VersionUtil;
import io.th0rgal.oraxen.utils.logs.Logs;
import net.kyori.adventure.key.Key;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.Particle;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class ParticleWrapper {
    private static Particle DUST_VALUE;
    private static Particle SPLASH_VALUE;

    static {
        try {
            // Only use Registry for Paper servers on 1.20.5+ where Registry.PARTICLE_TYPE exists
            if (VersionUtil.isPaperServer() && VersionUtil.atOrAbove("1.20.5")) {
                try {
                    Particle dust = Registry.PARTICLE_TYPE.get(NamespacedKey.minecraft("dust"));
                    Particle splash = Registry.PARTICLE_TYPE.get(NamespacedKey.minecraft("splash"));
                    if (dust == null || splash == null) {
                        throw new IllegalStateException("Registry lookup returned null");
                    }
                    DUST_VALUE = dust;
                    SPLASH_VALUE = splash;
                } catch (Exception registryException) {
                    // Registry approach failed, fall back to direct enum access
                    DUST_VALUE = getParticleByName("DUST", "REDSTONE");
                    SPLASH_VALUE = getParticleByName("SPLASH", "WATER_SPLASH");
                }
            } else {
                // For older versions or non-Paper servers, use direct enum access
                DUST_VALUE = getParticleByName("DUST", "REDSTONE");
                SPLASH_VALUE = getParticleByName("SPLASH", "WATER_SPLASH");
            }
        } catch (IllegalArgumentException | IncompatibleClassChangeError e) {
            Logs.logError("Failed to initialize particle types, using fallback particles: " + e.getMessage());
            e.printStackTrace();
            // Last resort fallback to any available particle
            Particle[] particles = Particle.values();
            if (particles.length == 0) {
                throw new IllegalStateException("No Particle enum constants available");
            }
            DUST_VALUE = particles[0];
            SPLASH_VALUE = particles.length > 1 ? particles[1] : particles[0];
        }
    }

    /**
     * Helper method to get particle by trying multiple names in order
     */
    private static Particle getParticleByName(String... names) {
        for (String name : names) {
            try {
                return Particle.valueOf(name.toUpperCase());
            } catch (IllegalArgumentException ignored) {
                // Try next name
            }
        }
        // If no names work, return first available particle as fallback
        Particle[] particles = Particle.values();
        if (particles.length > 0) {
            return particles[0];
        }
        throw new IllegalStateException("No Particle enum constants available - cannot resolve particle by names: " + java.util.Arrays.toString(names));
    }

    @NotNull
    public static final Particle DUST = Objects.requireNonNull(DUST_VALUE);
    @NotNull
    public static final Particle SPLASH = Objects.requireNonNull(SPLASH_VALUE);
}