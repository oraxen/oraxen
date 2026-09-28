package io.th0rgal.oraxen.api.events;

import io.th0rgal.oraxen.mechanics.Mechanic;
import io.th0rgal.oraxen.utils.drops.Drop;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public abstract class OraxenBlockBreakEvent<M extends Mechanic> extends OraxenBreakEvent<M> {

    protected OraxenBlockBreakEvent(@NotNull M mechanic, @NotNull Block block, @NotNull Player player, @NotNull Drop drop) {
        super(mechanic, block, player, drop);
    }

    @NotNull
    @Override
    public Block getBlock() {
        return super.getBlock();
    }
}
