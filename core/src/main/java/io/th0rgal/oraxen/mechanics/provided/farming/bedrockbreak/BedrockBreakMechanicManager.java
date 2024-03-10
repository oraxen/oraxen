package io.th0rgal.oraxen.mechanics.provided.farming.bedrockbreak;

import fr.euphyllia.energie.model.SchedulerType;
import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.api.OraxenItems;
import io.th0rgal.oraxen.utils.breaker.BreakerSystem;
import io.th0rgal.oraxen.utils.breaker.HardnessModifier;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class BedrockBreakMechanicManager {

    public BedrockBreakMechanicManager(BedrockBreakMechanicFactory factory) {
        BreakerSystem.MODIFIERS.add(new HardnessModifier() {

            @Override
            public boolean isTriggered(Player player, Block block, ItemStack tool) {
                CompletableFuture<Boolean> future = new CompletableFuture<>();
                OraxenPlugin.getScheduler().runTask(SchedulerType.SYNC, block.getLocation(), schedulerTaskInter -> {
                    if (block.getType() != Material.BEDROCK) {
                        future.complete(false);
                        return;
                    }

                    String itemID = OraxenItems.getIdByItem(tool);
                    future.complete(!factory.isNotImplementedIn(itemID) && (!factory.isDisabledOnFirstLayer() || block.getY() != 0));
                });
                try {
                    return future.get();
                } catch (InterruptedException | ExecutionException e) {
                    return false;
                }
            }

            @Override
            public void breakBlock(Player player, Block block, ItemStack tool) {
                String itemID = OraxenItems.getIdByItem(tool);
                BedrockBreakMechanic mechanic = (BedrockBreakMechanic) factory.getMechanic(itemID);
                World world = block.getWorld();
                Location loc = block.getLocation();

                if (mechanic == null) return;
                if (mechanic.bernouilliTest())
                    world.dropItemNaturally(loc, new ItemStack(Material.BEDROCK));

                block.breakNaturally(true);
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
