package io.th0rgal.oraxen.mechanics;

import io.th0rgal.oraxen.api.OraxenItems;
import io.th0rgal.oraxen.mechanics.provided.farming.bedrockbreak.BedrockBreakMechanic;
import io.th0rgal.oraxen.mechanics.provided.farming.bedrockbreak.BedrockBreakMechanicFactory;
import io.th0rgal.oraxen.utils.breaker.BreakerSystem;
import io.th0rgal.oraxen.utils.breaker.HardnessModifier;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

class BedrockBreakMechanicTest extends MechanicTestSupport {

    @Test
    void readsPeriodFromHardness() {
        BedrockBreakMechanic mechanic = new BedrockBreakMechanic(mechanicFactory(), mechanicSection("bedrockbreak", "delay", 10L, "hardness", 40L, "probability", 0.5));

        assertEquals(40L, mechanic.getPeriod());
    }

    @Test
    void reloadReplacesTheBedrockHardnessModifier() {
        Player player = mock(Player.class);
        Block block = mock(Block.class);
        when(block.getType()).thenReturn(Material.BEDROCK);
        ItemStack tool = mock(ItemStack.class);

        try (MockedStatic<OraxenItems> items = mockStatic(OraxenItems.class)) {
            items.when(() -> OraxenItems.getIdByItem(tool)).thenReturn("test_item");

            registerBedrockBreak(40L);
            MechanicsManager.unregisterMechanicFactory("bedrockbreak");
            registerBedrockBreak(10L);
            try {
                List<HardnessModifier> triggered = BreakerSystem.MODIFIERS.stream()
                        .filter(modifier -> modifier.isTriggered(player, block, tool)).toList();
                assertEquals(1, triggered.size());
                assertEquals(10L, triggered.get(0).getPeriod(player, block, tool));
            } finally {
                MechanicsManager.unregisterMechanicFactory("bedrockbreak");
            }
            assertTrue(BreakerSystem.MODIFIERS.stream().noneMatch(modifier -> modifier.isTriggered(player, block, tool)));
        }
    }

    private static void registerBedrockBreak(long hardness) {
        BedrockBreakMechanicFactory factory = new BedrockBreakMechanicFactory(new YamlConfiguration().createSection("bedrockbreak"));
        factory.parse(mechanicSection("bedrockbreak", "hardness", hardness, "probability", 1.0));
        MechanicsManager.registerMechanicFactory("bedrockbreak", factory, true);
    }
}
