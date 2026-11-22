package io.th0rgal.oraxen.mechanics.provided.gameplay.block;

import io.papermc.paper.threadedregions.scheduler.EntityScheduler;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BlockDurabilityTest {

    @Test
    void durabilityRuleWithoutChangesKeepsTheNextToolDamage() {
        Player player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(UUID.randomUUID());
        when(player.getGameMode()).thenReturn(GameMode.SURVIVAL);
        when(player.getScheduler()).thenReturn(mock(EntityScheduler.class));
        ItemStack tool = mock(ItemStack.class);
        when(tool.getType()).thenReturn(Material.DIAMOND_PICKAXE);
        when(tool.getAmount()).thenReturn(1);

        BlockDurability.applyConfigured(player, tool, new BlockBreaking.DurabilityAction(0, 0));

        PlayerItemDamageEvent damage = new PlayerItemDamageEvent(player, tool, 1, 1);
        assertFalse(BlockDurability.cancelPendingVanillaDamage(damage));
        assertFalse(damage.isCancelled());
    }
}
