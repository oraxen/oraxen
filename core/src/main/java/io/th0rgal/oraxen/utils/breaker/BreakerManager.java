package io.th0rgal.oraxen.utils.breaker;

import io.th0rgal.oraxen.mechanics.provided.gameplay.custom_block.CustomBlockMechanic;
import io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.FurnitureMechanic;
import org.bukkit.block.Block;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;

public interface BreakerManager {

    void startFurnitureBreak(Player player, ItemDisplay baseEntity, FurnitureMechanic mechanic, Block block);
    void startBlockBreak(Player player, Block block, CustomBlockMechanic mechanic);
    void stopBlockBreak(Player player);
}
