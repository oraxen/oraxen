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
            if (VersionUtil.isPaperServer()) {
                DUST_VALUE = Registry.PARTICLE_TYPE.get(NamespacedKey.minecraft("dust"));
                SPLASH_VALUE = Registry.PARTICLE_TYPE.get(NamespacedKey.minecraft("splash"));
            } else {
                // Fallback for non-Paper servers with direct enum values
                DUST_VALUE = Particle.valueOf("REDSTONE"); // equivalent to dust
                SPLASH_VALUE = Particle.valueOf("WATER_SPLASH");
            }
        } catch (Exception e) {
            // Fallback if Registry.PARTICLE_TYPE is not available or enum names are
            // different
            try {
                DUST_VALUE = Particle.valueOf("REDSTONE");
                SPLASH_VALUE = Particle.valueOf("WATER_SPLASH");
            } catch (IllegalArgumentException ex) {
                // If these don't exist, try common fallbacks
                try {
                    DUST_VALUE = Particle.valueOf("DUST");
                    SPLASH_VALUE = Particle.valueOf("SPLASH");
                } catch (IllegalArgumentException ex2) {
                    Logs.logError("Could not find appropriate particle types for this server version");
                    // Last resort fallback to ANY particle
                    DUST_VALUE = Particle.values()[0];
                    SPLASH_VALUE = Particle.values()[0];
                }
            }
        }
    }

    @NotNull
    public static final Particle DUST = Objects.requireNonNull(DUST_VALUE);
    @NotNull
    public static final Particle SPLASH = Objects.requireNonNull(SPLASH_VALUE);
}