package io.th0rgal.oraxen.api.events.custom_block.stringblock;

import io.th0rgal.oraxen.api.events.custom_block.OraxenBlockDamageEvent;
import io.th0rgal.oraxen.mechanics.provided.gameplay.custom_block.CustomBlockMechanic;
import io.th0rgal.oraxen.mechanics.provided.gameplay.custom_block.stringblock.StringBlockMechanic;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.jetbrains.annotations.NotNull;

/**
 * Event fired right before a player damages a StringBlock
 * If cancelled, the block will not be damaged.
 * @see StringBlockMechanic
 */
public class OraxenStringBlockDamageEvent extends OraxenBlockDamageEvent implements Cancellable {

    /**
     * @param mechanic The CustomBlockMechanic of this block
     * @param block    The block that was damaged
     * @param player   The player who damaged this block
     */
    public OraxenStringBlockDamageEvent(@NotNull CustomBlockMechanic mechanic, @NotNull Block block, @NotNull Player player) {
        super(mechanic, block, player);
    }

    /**
     * @return The StringBlockMechanic of this block
     */
    @NotNull
    @Override
    public StringBlockMechanic getMechanic() {
        return (StringBlockMechanic) super.getMechanic();
    }
}
