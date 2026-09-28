package io.th0rgal.oraxen.api.events.stringblock;

import io.th0rgal.oraxen.api.events.OraxenBlockBreakEvent;
import io.th0rgal.oraxen.mechanics.provided.gameplay.stringblock.StringBlockMechanic;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class OraxenStringBlockBreakEvent extends OraxenBlockBreakEvent<StringBlockMechanic> {

    private static final HandlerList HANDLERS = new HandlerList();

    public OraxenStringBlockBreakEvent(@NotNull StringBlockMechanic mechanic, @NotNull Block block, @NotNull Player player) {
        super(mechanic, block, player, mechanic.getDrop(player.getInventory().getItemInMainHand()));
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
