package io.th0rgal.oraxen.mechanics.provided.gameplay.block;

import org.bukkit.configuration.ConfigurationSection;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BlockBreakingTest {

    @Test
    void absentBreakingConfigDoesNotOverrideVanillaHardness() {
        ConfigurationSection section = mock(ConfigurationSection.class);
        when(section.getList("breaking")).thenReturn(null);

        BlockBreaking breaking = new BlockBreaking(section, "custom_block");

        assertFalse(breaking.hasHardness(null));
        assertNotNull(breaking.drop(null));
    }

    @Test
    void invalidBreakingConfigDoesNotCreateFallbackHardnessRule() {
        ConfigurationSection section = mock(ConfigurationSection.class);
        doReturn(List.of("invalid")).when(section).getList("breaking");

        BlockBreaking breaking = new BlockBreaking(section, "custom_block");

        assertFalse(breaking.hasHardness(null));
        assertNotNull(breaking.drop(null));
    }

    @Test
    void zeroHardnessDoesNotOverrideVanillaHardness() {
        ConfigurationSection section = mock(ConfigurationSection.class);
        doReturn(List.of(Map.of("else", true, "hardness", 0))).when(section).getList("breaking");

        BlockBreaking breaking = new BlockBreaking(section, "custom_block");

        assertFalse(breaking.hasHardness(null));
    }

    @Test
    void fallbackRuleCanConfigureDurabilityAction() {
        ConfigurationSection section = mock(ConfigurationSection.class);
        doReturn(List.of(Map.of("else", true, "durability", Map.of("add", 0, "remove", 2))))
                .when(section).getList("breaking");

        BlockBreaking breaking = new BlockBreaking(section, "custom_block");
        BlockBreaking.DurabilityAction action = breaking.durabilityAction(null);

        assertNotNull(action);
        assertEquals(0, action.add());
        assertEquals(2, action.remove());
    }
}
