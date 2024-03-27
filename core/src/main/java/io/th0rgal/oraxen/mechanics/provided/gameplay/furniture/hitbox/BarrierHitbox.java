package io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.hitbox;

import io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.BlockLocation;

public class BarrierHitbox {
    private final BlockLocation location;

    public BarrierHitbox(Object hitboxObject) {
        BarrierHitbox hitbox;
        if (hitboxObject instanceof String string) {
            hitbox = new BarrierHitbox(string);
            this.location = hitbox.location;
        } else this.location = BlockLocation.ZERO;
    }

    public BarrierHitbox(String hitboxString) {
        this.location = new BlockLocation(hitboxString);

    }

}
