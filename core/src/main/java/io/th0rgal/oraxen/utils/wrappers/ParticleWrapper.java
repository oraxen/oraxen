package io.th0rgal.oraxen.utils.wrappers;

import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Registry;
import org.jetbrains.annotations.NotNull;

public class ParticleWrapper {

    @NotNull public static final Particle DUST = Registry.PARTICLE_TYPE.get(NamespacedKey.minecraft("dust"));
    @NotNull public static final Particle SPLASH = Registry.PARTICLE_TYPE.get(NamespacedKey.minecraft("splash"));
}
