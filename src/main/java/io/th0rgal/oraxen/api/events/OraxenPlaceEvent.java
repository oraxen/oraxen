package io.th0rgal.oraxen.api.events;

import io.th0rgal.oraxen.mechanics.Mechanic;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A player placed a custom block or furniture. Adds the held item and hand
 * to the damage-event fields.
 */
public abstract class OraxenPlaceEvent<M extends Mechanic> extends OraxenBlockDamageEvent<M> {

    private final ItemStack itemInHand;
    private final EquipmentSlot hand;

    protected OraxenPlaceEvent(@NotNull M mechanic, @NotNull Block block, @NotNull Player player,
                               @Nullable ItemStack itemInHand, @NotNull EquipmentSlot hand) {
        super(mechanic, block, player);
        this.itemInHand = itemInHand;
        this.hand = hand;
    }

    /**
     * @return the mechanic that was placed
     */
    @NotNull
    @Override
    public M getMechanic() {
        return super.getMechanic();
    }

    /**
     * @return the player who placed it
     */
    @NotNull
    @Override
    public Player getPlayer() {
        return super.getPlayer();
    }

    /**
     * @return the block that was placed
     */
    @NotNull
    @Override
    public Block getBlock() {
        return super.getBlock();
    }

    /**
     * @return the item in the player's hand when they placed it, or null when the caller had none
     */
    @Nullable
    public ItemStack getItemInHand() {
        return itemInHand;
    }

    /**
     * @return the hand used to place it
     */
    @NotNull
    public EquipmentSlot getHand() {
        return hand;
    }
}
