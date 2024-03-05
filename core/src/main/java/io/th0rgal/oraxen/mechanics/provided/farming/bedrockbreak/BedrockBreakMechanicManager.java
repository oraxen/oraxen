package io.th0rgal.oraxen.mechanics.provided.farming.bedrockbreak;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.BlockPosition;
import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.api.OraxenItems;
import io.th0rgal.oraxen.utils.breaker.BreakerSystem;
import io.th0rgal.oraxen.utils.breaker.HardnessModifier;
import net.kyori.adventure.text.Component;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.craftbukkit.v1_20_R3.block.CraftBlock;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class BedrockBreakMechanicManager {

    public BedrockBreakMechanicManager(BedrockBreakMechanicFactory factory) {
        BreakerSystem.MODIFIERS.add(new HardnessModifier() {

            @Override
            public boolean isTriggered(Player player, Block block, ItemStack tool) {
                if (block.getType() != Material.BEDROCK) return false;

                String itemID = OraxenItems.getIdByItem(tool);
                return !factory.isNotImplementedIn(itemID) && (!factory.isDisabledOnFirstLayer() || block.getY() != 0);

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
