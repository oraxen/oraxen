package io.th0rgal.oraxen.mechanics.provided.bedrockbreak;

import io.th0rgal.oraxen.items.OraxenItems;
import io.th0rgal.oraxen.utils.breaker.BreakerSystem;
import io.th0rgal.oraxen.utils.breaker.HardnessModifier;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitScheduler;

import java.util.HashMap;
import java.util.Map;

public class BedrockBreakMechanicManager {

    private final Map<Location, BukkitScheduler> breakerPerLocation = new HashMap<>();


    public BedrockBreakMechanicManager(BedrockBreakMechanicFactory factory) {
        BreakerSystem.MODIFIERS.add(new HardnessModifier() {

            @Override
            public boolean isTriggered(Player player, Block block, ItemStack tool) {
                if (block.getType() != Material.BEDROCK)
                    return false;

                String itemID = OraxenItems.getIdByItem(tool);
                return !factory.isNotImplementedIn(itemID) && (!factory.isDisabledOnFirstLayer() || block.getY() != 0);

            }

            @Override
            public void breakBlock(Player player, Block block, ItemStack tool) {
                String itemID = OraxenItems.getIdByItem(tool);
                BedrockBreakMechanic mechanic = (BedrockBreakMechanic) factory.getMechanic(itemID);
                World world = block.getWorld();
                Location location = block.getLocation();
                if (mechanic.bernouilliTest())
                    world.dropItemNaturally(location, new ItemStack(Material.BEDROCK));
                world.playSound(location, Sound.ENTITY_WITHER_BREAK_BLOCK, 1F, 0.05F);
                world.spawnParticle(Particle.BLOCK_CRACK,
                        location, 25, 0.5D, 0.5D, 0.5D,
                        block.getBlockData());
                block.breakNaturally();
            }

            @Override
            public long getPeriod(Player player, Block block, ItemStack tool) {
                String itemID = OraxenItems.getIdByItem(tool);
                BedrockBreakMechanic mechanic = (BedrockBreakMechanic) factory.getMechanic(itemID);
                return mechanic.getPeriod();
            }
        });
    }

}