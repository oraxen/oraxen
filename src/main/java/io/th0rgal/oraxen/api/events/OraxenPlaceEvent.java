package io.th0rgal.oraxen.api.events;

import io.th0rgal.oraxen.mechanics.Mechanic;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A player placed a custom block or furniture.
 * The held item is null when the caller had none, which string and chorus placement allow.
 */
public abstract class OraxenPlaceEvent<M extends Mechanic> extends OraxenMechanicEvent<M> {

    private final ItemStack itemInHand;
    private final EquipmentSlot hand;

    protected OraxenPlaceEvent(@NotNull M mechanic, @NotNull Block block, @NotNull Player player,
                               @Nullable ItemStack itemInHand, @NotNull EquipmentSlot hand) {
        super(mechanic, block, player);
        this.itemInHand = itemInHand;
        this.hand = hand;
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
