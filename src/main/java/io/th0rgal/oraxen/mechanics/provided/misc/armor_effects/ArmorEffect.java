package io.th0rgal.oraxen.mechanics.provided.misc.armor_effects;

import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;

public class ArmorEffect extends PotionEffect {
    private final PotionEffect potionEffect;
    private final boolean requiresFullSet;

    public ArmorEffect(@NotNull PotionEffect potionEffect, boolean requiresFullSet) {
        super(potionEffect.getType(), potionEffect.getDuration(), potionEffect.getAmplifier(), potionEffect.isAmbient(), potionEffect.hasParticles(), potionEffect.hasIcon());
        this.potionEffect = potionEffect;
        this.requiresFullSet = requiresFullSet;
    }

    public ArmorEffect(@NotNull PotionEffectType type, int duration, int amplifier, boolean ambient, boolean particles, boolean icon, boolean requiresFullSet) {
        super(type, duration, amplifier, ambient, particles, icon);
        this.potionEffect = new PotionEffect(type, duration, amplifier, ambient, particles, icon);
        this.requiresFullSet = requiresFullSet;
    }

    public PotionEffect getEffect() {
        return potionEffect;
    }

    public boolean requiresFullSet() {
        return requiresFullSet;
    }
}
