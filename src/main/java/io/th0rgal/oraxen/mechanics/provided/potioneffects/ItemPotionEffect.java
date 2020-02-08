package io.th0rgal.oraxen.mechanics.provided.potioneffects;

import org.bukkit.potion.PotionEffect;

public class ItemPotionEffect {

    enum Position {
        HELD,
        WORN
    }

    private PotionEffect potionEffect;
    private Position position;

    public ItemPotionEffect(PotionEffect potionEffect, Position position) {
        this.potionEffect = potionEffect;
        this.position = position;
    }

    public PotionEffect getPotionEffect() {
        return potionEffect;
    }

    public Position getPosition() {
        return position;
    }
}
