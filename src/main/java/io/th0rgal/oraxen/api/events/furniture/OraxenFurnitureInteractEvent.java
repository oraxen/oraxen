package io.th0rgal.oraxen.api.events.furniture;

import io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.FurnitureMechanic;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class OraxenFurnitureInteractEvent extends Event implements Cancellable {

    private final FurnitureMechanic mechanic;
    private final Entity baseEntity;
    private final Player player;
    private final Block block;
    private final BlockFace blockFace;
    private final ItemStack itemInHand;
    private final EquipmentSlot hand;
    private boolean isCancelled;
    private static final HandlerList HANDLERS = new HandlerList();

    public OraxenFurnitureInteractEvent(@NotNull final FurnitureMechanic mechanic, @NotNull final Entity baseEntity, @NotNull final Player player, @Nullable final ItemStack itemInHand, @NotNull final EquipmentSlot hand) {
        this(mechanic, baseEntity, player, itemInHand, hand, null, null);
    }

    public OraxenFurnitureInteractEvent(@NotNull final FurnitureMechanic mechanic, @NotNull final Entity baseEntity, @NotNull final Player player, @Nullable final ItemStack itemInHand, @NotNull final EquipmentSlot hand, @Nullable final Block block, @Nullable final BlockFace blockFace) {
        this.player = player;
        this.mechanic = mechanic;
        this.baseEntity = baseEntity;
        this.block = block;
        this.blockFace = blockFace;
        this.itemInHand = itemInHand;
        this.hand = hand;
        this.isCancelled = false;
    }

    /**
     * @return The FurnitureMechanic of the furniture that was interacted with
     */
    @NotNull
    public FurnitureMechanic getMechanic() {
        return mechanic;
    }

    /**
     * @return The player who interacted with the furniture
     */
    @NotNull
    public Player getPlayer() {
        return player;
    }

    /**
     * @return The block that was interacted with, null if the furniture has no hitbox
     */
    @Nullable
    public Block getBlock() {
        return block;
    }

    /**
     * @return The base entity of the furniture
     */
    @NotNull
    public Entity getBaseEntity() {
        return baseEntity;
    }

    /**
     * @return The Interaction type entity, if supported by the server version
     * @apiNote This will return as a normal Entity, not of type Interaction, or null.
     * This is due to backwards compatibility with old server versions.
     * You can safely cast any entity returned by this method to Interaction.
     */
    @Nullable
    public Entity getInteractionEntity() { return mechanic.getInteractionEntity(baseEntity); }

    /**
     * @return The block face that was interacted with, null if the furniture has no hitbox
     */
    @Nullable
    public BlockFace getBlockFace() {
        return blockFace;
    }

    /**
     * @return The item in the player's hand when they interacted with the furniture
     */
    public ItemStack getItemInHand() {
        return itemInHand;
    }

    /**
     * @return The hand the player interacted with the furniture with
     */
    public EquipmentSlot getHand() {
        return hand;
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
        return getHandlerList();
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
