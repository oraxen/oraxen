package io.th0rgal.oraxen.api.events.furniture;

import io.th0rgal.oraxen.api.events.OraxenBaseEntityDamageEvent;
import io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.FurnitureMechanic;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Fired right before a player damages furniture.
 * Cancelling the event stops the damage.
 */
public class OraxenFurnitureDamageEvent extends OraxenBaseEntityDamageEvent<FurnitureMechanic> {

    private static final HandlerList HANDLERS = new HandlerList();

    public OraxenFurnitureDamageEvent(@NotNull FurnitureMechanic mechanic, @NotNull Entity baseEntity,
                                      @NotNull Player player, @Nullable Block block) {
        super(mechanic, baseEntity, player, block);
    }

    public OraxenFurnitureDamageEvent(@NotNull FurnitureMechanic mechanic, @NotNull Entity baseEntity, @NotNull Player player) {
        this(mechanic, baseEntity, player, null);
    }

    @NotNull
    @Override
    public FurnitureMechanic getMechanic() {
        return super.getMechanic();
    }

    @NotNull
    @Override
    public HandlerList getHandlers() {
        return getHandlerList();
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
