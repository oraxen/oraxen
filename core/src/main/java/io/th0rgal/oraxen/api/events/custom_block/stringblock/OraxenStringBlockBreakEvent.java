package io.th0rgal.oraxen.api.events.custom_block.stringblock;

import io.th0rgal.oraxen.api.events.custom_block.OraxenBlockBreakEvent;
import io.th0rgal.oraxen.mechanics.provided.gameplay.custom_block.stringblock.StringBlockMechanic;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.jetbrains.annotations.NotNull;

public class OraxenStringBlockBreakEvent extends OraxenBlockBreakEvent implements Cancellable {

    public OraxenStringBlockBreakEvent(@NotNull StringBlockMechanic mechanic, @NotNull Block block, @NotNull Player player) {
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
