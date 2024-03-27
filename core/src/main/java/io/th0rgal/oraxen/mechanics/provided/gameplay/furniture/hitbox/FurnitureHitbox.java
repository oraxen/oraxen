package io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.hitbox;

import io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.FurnitureMechanic;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class FurnitureHitbox {
    public static final FurnitureHitbox EMPTY = new FurnitureHitbox(Set.of(), Set.of());
    private final Set<BarrierHitbox> barrierHitboxes;
    private final Set<InteractionHitbox> interactionHitboxes;

    public FurnitureHitbox(@NotNull ConfigurationSection hitboxSection) {
        Set<BarrierHitbox> barrierHitboxes = new HashSet<>();
        for (String barrierString : hitboxSection.getStringList("barrierHitboxes"))
            barrierHitboxes.add(new BarrierHitbox(barrierString));

        Set<InteractionHitbox> interactionHitboxes = new HashSet<>();
        for (String interactionString : hitboxSection.getStringList("interactionHitboxes"))
            interactionHitboxes.add(new InteractionHitbox(interactionString));

        this.barrierHitboxes = barrierHitboxes;
        this.interactionHitboxes = interactionHitboxes;
    }

    public FurnitureHitbox(Collection<BarrierHitbox> barrierHitboxes, Collection<InteractionHitbox> interactionHitboxes) {
        this.barrierHitboxes = new HashSet<>(barrierHitboxes);
        this.interactionHitboxes = new HashSet<>(interactionHitboxes);
    }

    public Set<BarrierHitbox> barrierHitboxes() {
        return barrierHitboxes;
    }

    public Set<InteractionHitbox> interactionHitboxes() {
        return interactionHitboxes;
    }

    public void handleHitboxes(Entity baseEntity, FurnitureMechanic mechanic, float yaw) {

    }
}
