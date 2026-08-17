package io.th0rgal.oraxen.utils.wrappers;

import io.th0rgal.oraxen.utils.VersionUtil;
import io.th0rgal.oraxen.utils.logs.Logs;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.Particle;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class ParticleWrapper {
    private static Particle DUST_VALUE;
    private static Particle SPLASH_VALUE;
    private static Particle BLOCK_VALUE;
    private static Particle ITEM_VALUE;
    private static Particle DUST_COLOR_TRANSITION_VALUE;

    static {
        try {
            DUST_VALUE = resolveParticle("dust", "DUST", "REDSTONE");
            SPLASH_VALUE = resolveParticle("splash", "SPLASH", "WATER_SPLASH");
            BLOCK_VALUE = resolveParticle("block", "BLOCK", "BLOCK_CRACK");
            ITEM_VALUE = resolveParticle("item", "ITEM", "ITEM_CRACK");
            DUST_COLOR_TRANSITION_VALUE = resolveParticle("dust_color_transition", "DUST_COLOR_TRANSITION");
        } catch (IllegalArgumentException | IncompatibleClassChangeError e) {
            Logs.logError("Failed to initialize particle types, using fallback particles: " + e.getMessage());
            e.printStackTrace();
            // Last resort fallback to any available particle
            Particle[] particles = Particle.values();
            if (particles.length == 0) {
                throw new IllegalStateException("No Particle enum constants available");
            }
            if (DUST_VALUE == null) DUST_VALUE = particles[0];
            if (SPLASH_VALUE == null) SPLASH_VALUE = particles.length > 1 ? particles[1] : particles[0];
            if (BLOCK_VALUE == null) BLOCK_VALUE = particles[0];
            if (ITEM_VALUE == null) ITEM_VALUE = particles[0];
            if (DUST_COLOR_TRANSITION_VALUE == null) DUST_COLOR_TRANSITION_VALUE = DUST_VALUE;
        }
    }

    /**
     * Resolves a particle without referencing enum constants that were renamed in 1.20.5.
     * Servers on 1.20.5+ go through the registry, older ones fall back to
     * {@link Particle#valueOf(String)} with the modern name first and the legacy name second.
     *
     * @param key   the vanilla particle key, e.g. {@code block}
     * @param names candidate enum names, most recent first
     */
    private static Particle resolveParticle(String key, String... names) {
        // Use Registry on 1.20.5+ where Registry.PARTICLE_TYPE exists
        if (VersionUtil.atOrAbove("1.20.5")) {
            try {
                Particle particle = Registry.PARTICLE_TYPE.get(NamespacedKey.minecraft(key));
                if (particle != null) {
                    return particle;
                }
            } catch (Exception | LinkageError ignored) {
                // Registry approach failed, fall back to direct enum access
            }
        }
        return getParticleByName(names);
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
    @NotNull
    public static final Particle BLOCK = Objects.requireNonNull(BLOCK_VALUE);
    @NotNull
    public static final Particle ITEM = Objects.requireNonNull(ITEM_VALUE);
    @NotNull
    public static final Particle DUST_COLOR_TRANSITION = Objects.requireNonNull(DUST_COLOR_TRANSITION_VALUE);
}
