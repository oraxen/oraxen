package io.th0rgal.oraxen.utils;

import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Registry;

import java.util.Objects;

public class ParticleWrapper {
    public static Particle redstone() {
        return VersionUtil.atOrAbove("1.20.5") ? type("dust") : type("redstone");
    }

    private static Particle type(String key) {
        NamespacedKey namespacedKey = Objects.requireNonNull(NamespacedKey.fromString(key));
        return Objects.requireNonNull(Registry.PARTICLE_TYPE.get(namespacedKey));
    }
}
