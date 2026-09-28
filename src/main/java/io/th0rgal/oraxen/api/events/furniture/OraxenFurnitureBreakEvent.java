package io.th0rgal.oraxen.api.events.furniture;

import io.th0rgal.oraxen.api.events.OraxenBaseEntityBreakEvent;
import io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.FurnitureMechanic;
import io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.evolution.GrowthStage;
import io.th0rgal.oraxen.utils.drops.Drop;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class OraxenFurnitureBreakEvent extends OraxenBaseEntityBreakEvent<FurnitureMechanic> {

    private static final HandlerList HANDLERS = new HandlerList();

    public OraxenFurnitureBreakEvent(@NotNull FurnitureMechanic mechanic, @NotNull Entity baseEntity,
                                     @NotNull Player player, @Nullable Block block) {
        super(mechanic, baseEntity, player, block, dropForCurrentStage(mechanic, baseEntity));
    }

    /**
     * Staged furniture drops the current growth stage's loot. Everything else
     * uses the mechanic's own drop.
     */
    private static Drop dropForCurrentStage(FurnitureMechanic mechanic, Entity baseEntity) {
        if (!mechanic.hasGrowthStages()) return mechanic.getDrop();

        Integer stageIndex = baseEntity.getPersistentDataContainer()
                .get(FurnitureMechanic.STAGE_INDEX_KEY, PersistentDataType.INTEGER);
        if (stageIndex == null) return mechanic.getDrop();

        GrowthStage currentStage = mechanic.getGrowthStage(stageIndex);
        if (currentStage == null || currentStage.getDrop() == null) return mechanic.getDrop();
        return currentStage.getDrop();
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
