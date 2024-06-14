package io.th0rgal.oraxen.api.events.custom_block.stringblock;

import io.th0rgal.oraxen.api.events.custom_block.OraxenCustomBlockDropLootEvent;
import io.th0rgal.oraxen.mechanics.provided.gameplay.custom_block.stringblock.StringBlockMechanic;
import io.th0rgal.oraxen.utils.drops.DroppedLoot;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class OraxenStringBlockDropLootEvent extends OraxenCustomBlockDropLootEvent {
    public OraxenStringBlockDropLootEvent(@NotNull StringBlockMechanic mechanic, @NotNull Block block, @NotNull Player player, @NotNull List<DroppedLoot> loots) {
        super(mechanic, block, player, loots);
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
