package io.th0rgal.oraxen.api.events.furniture;

import io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.FurnitureMechanic;
import io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.evolution.GrowthStage;
import io.th0rgal.oraxen.utils.drops.Drop;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class OraxenFurnitureBreakEvent extends Event implements Cancellable {

    boolean isCancelled;
    private final FurnitureMechanic mechanic;
    private final Block block;
    private final Player player;
    private final Entity baseEntity;
    private Drop drop;
    private static final HandlerList HANDLERS = new HandlerList();

    public OraxenFurnitureBreakEvent(@NotNull final FurnitureMechanic mechanic, @NotNull final Entity baseEntity, @NotNull final Player player, @Nullable final Block block) {
        this.block = block;
        this.mechanic = mechanic;
        this.player = player;
        this.baseEntity = baseEntity;
        
        // Determine drop based on current growth stage (if using staged evolution)
        this.drop = resolveDropForCurrentStage(mechanic, baseEntity);
    }

    /**
     * Resolves the correct drop based on the furniture's current growth stage.
     * For staged evolution, returns the stage-specific drop.
     * For legacy or non-evolving furniture, returns the mechanic's default drop.
     */
    private static Drop resolveDropForCurrentStage(FurnitureMechanic mechanic, Entity baseEntity) {
        if (!mechanic.hasGrowthStages()) {
            return mechanic.getDrop();
        }
        
        // Get current stage index from entity
        Integer stageIndex = baseEntity.getPersistentDataContainer()
                .get(FurnitureMechanic.STAGE_INDEX_KEY, PersistentDataType.INTEGER);
        
        if (stageIndex == null) {
            return mechanic.getDrop();
        }
        
        GrowthStage currentStage = mechanic.getGrowthStage(stageIndex);
        if (currentStage == null || currentStage.getDrop() == null) {
            return mechanic.getDrop();
        }
        
        return currentStage.getDrop();
    }

    /**
     * @return The FurnitureMechanic of this Furniture
     */
    @NotNull
    public FurnitureMechanic getMechanic() {
        return mechanic;
    }

    /**
     * @return The player that broke the furniture
     */
    @NotNull
    public Player getPlayer() {
        return player;
    }

    /**
     * Returns the block that was broken.
     * @return block that was broken or null if it was an entity
     */
    @Nullable
    public Block getBlock() {
        return block;
    }

    /**
     * @return The ItemFrame the furniture is inmechanic
     */
    @NotNull
    public Entity getBaseEntity() {
        return baseEntity;
    }

    /**
     * @return The drop of the furniture
     */
    @NotNull
    public Drop getDrop() {
        return drop;
    }

    /**
     * Set the drop of the furniture
    * @param drop the new drop
    */
    public void setDrop(@NotNull final Drop drop) {
        this.drop = drop;
    }


    @Override
    public boolean isCancelled() {
        return isCancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        isCancelled = cancel;
    }

    @NotNull
    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }

}
