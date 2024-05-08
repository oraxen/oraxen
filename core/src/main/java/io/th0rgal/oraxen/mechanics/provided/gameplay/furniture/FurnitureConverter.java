package io.th0rgal.oraxen.mechanics.provided.gameplay.furniture;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.utils.BlockHelpers;
import io.th0rgal.oraxen.utils.logs.Logs;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.jetbrains.annotations.NotNull;

public class FurnitureConverter {

    private static final NamespacedKey BARRIER_KEY = new NamespacedKey(OraxenPlugin.get(), "barriers");
    private static final NamespacedKey INTERACTION_KEY = new NamespacedKey(OraxenPlugin.get(), "interaction");

    public static void convert(@NotNull Entity baseEntity) {

        // Remove legacy Barrier-hitbox
        for (int xOffset = -16; xOffset <= 16; xOffset++) {
            for (int yOffset = -16; yOffset <= 16; yOffset++) {
                for (int zOffset = -16; zOffset <= 16; zOffset++) {
                    Block barrier = baseEntity.getLocation().add(xOffset, yOffset, zOffset).getBlock();
                    if (barrier.getType() != Material.BARRIER) continue;
                    if (!BlockHelpers.getPDC(barrier).has(BARRIER_KEY)) continue;

                    Logs.debug("removed legacy barrier for " + baseEntity.getUniqueId() + " at " + barrier.getLocation());
                    barrier.setType(Material.AIR);
                }
            }
        }

        // Remove legacy Interaction-hitbox
        baseEntity.getNearbyEntities(1, 1, 1).stream().filter(e -> e.getPersistentDataContainer().has(INTERACTION_KEY))
                .forEach(e -> {
                    e.remove();
                    Logs.debug("Removed legacy interaction for " + baseEntity.getUniqueId());
                });

        //TODO Remove legacy seats when seats become packet-based
    }
}
