package io.th0rgal.oraxen.api.events;

import io.th0rgal.oraxen.mechanics.Mechanic;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A player interacted with a custom block or furniture.
 */
public abstract class OraxenInteractEvent<M extends Mechanic> extends OraxenMechanicEvent<M> {

    private final ItemStack itemInHand;
    private final EquipmentSlot hand;
    private final BlockFace blockFace;

    protected OraxenInteractEvent(@NotNull M mechanic, @NotNull Player player, @Nullable ItemStack itemInHand,
                                  @NotNull EquipmentSlot hand, @Nullable Block block, @Nullable BlockFace blockFace) {
        super(mechanic, block, player);
        this.itemInHand = itemInHand;
        this.hand = hand;
        this.blockFace = blockFace;
    }

    /**
     * @return the clicked face, or null when the furniture has no hitbox
     */
    @Nullable
    public BlockFace getBlockFace() {
        return blockFace;
    }

    /**
     * @return the item in hand during the interaction, or null when the hand was empty
     */
    @Nullable
    public ItemStack getItemInHand() {
        return itemInHand;
    }

    /**
     * @return the hand used to interact
     */
    @NotNull
    public EquipmentSlot getHand() {
        return hand;
    }
}
