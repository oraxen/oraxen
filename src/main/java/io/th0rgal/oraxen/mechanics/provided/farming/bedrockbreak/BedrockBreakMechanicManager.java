package io.th0rgal.oraxen.mechanics.provided.farming.bedrockbreak;

import io.th0rgal.oraxen.api.OraxenItems;
import io.th0rgal.oraxen.utils.breaker.BreakerSystem;
import io.th0rgal.oraxen.utils.breaker.HardnessModifier;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class BedrockBreakMechanicManager {

    public BedrockBreakMechanicManager(BedrockBreakMechanicFactory factory) {
        BreakerSystem.MODIFIERS.add(new HardnessModifier() {

            @Override
            public boolean isTriggered(Player player, Block block, ItemStack tool) {
                if (block.getType() != Material.BEDROCK) return false;

                String itemID = OraxenItems.getIdByItem(tool);
                boolean disableFirstLayer = !factory.isDisabledOnFirstLayer() || block.getY() > (block.getWorld().getMinHeight());
                return !factory.isNotImplementedIn(itemID) && disableFirstLayer;

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
