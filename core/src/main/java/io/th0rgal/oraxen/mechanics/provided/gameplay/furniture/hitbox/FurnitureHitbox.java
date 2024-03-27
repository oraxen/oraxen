package io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.hitbox;

import org.bukkit.configuration.ConfigurationSection;

import java.util.HashSet;
import java.util.Set;

public class FurnitureHitbox {
    private final Set<BarrierHitbox> barrierHitboxes;
    private final Set<InteractionHitbox> interactionHitboxes;

    public FurnitureHitbox(ConfigurationSection hitboxSection) {
        Set<BarrierHitbox> barrierHitboxes = new HashSet<>();
        for (String barrierString : hitboxSection.getStringList("barrierHitboxes"))
            barrierHitboxes.add(new BarrierHitbox(barrierString));

        Set<InteractionHitbox> interactionHitboxes = new HashSet<>();
        for (String interactionString : hitboxSection.getStringList("interactionHitboxes"))
            interactionHitboxes.add(new InteractionHitbox(interactionString));

        this.barrierHitboxes = barrierHitboxes;
        this.interactionHitboxes = interactionHitboxes;
    }

    public Set<BarrierHitbox> barrierHitboxes() {
        return barrierHitboxes;
    }

    public Set<InteractionHitbox> interactionHitboxes() {
        return interactionHitboxes;
    }
}
