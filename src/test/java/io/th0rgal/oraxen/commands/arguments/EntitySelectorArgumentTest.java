package io.th0rgal.oraxen.commands.arguments;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.BlockCommandSender;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.Collection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

class EntitySelectorArgumentTest {

    @Test
    void playerSelectorsStillResolveWhenVanillaSelectorsAreUnsupported() {
        Player sender = mock(Player.class);
        Player other = mock(Player.class);
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(() -> Bukkit.selectEntities(any(CommandSender.class), anyString()))
                    .thenThrow(new UnsupportedOperationException());
            bukkit.when(Bukkit::getOnlinePlayers).then(invocation -> List.of(sender, other));

            OraxenArgument.ParseResult<Collection<Player>> all =
                    new EntitySelectorArgument.ManyPlayers("targets").parse(sender, List.of("@a"), 0);
            OraxenArgument.ParseResult<Player> self =
                    new EntitySelectorArgument.OnePlayer("target").parse(sender, List.of("@s"), 0);

            assertTrue(all.success());
            assertEquals(List.of(sender, other), List.copyOf(all.value()));
            assertTrue(self.success());
            assertEquals(sender, self.value());
        }
    }

    @Test
    void nearestPlayerSelectorResolvesFromACommandBlockWhenVanillaSelectorsAreUnsupported() {
        World world = mock(World.class);
        Block block = mock(Block.class);
        when(block.getLocation()).thenReturn(new Location(world, 0, 64, 0));
        BlockCommandSender commandBlock = mock(BlockCommandSender.class);
        when(commandBlock.getBlock()).thenReturn(block);
        // The far player is closer to the block's corner, the near player is closer to its center.
        Player near = player(world, new Location(world, 1.5, 64, 0));
        Player far = player(world, new Location(world, -0.6, 64, 0));
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(() -> Bukkit.selectEntities(any(CommandSender.class), anyString()))
                    .thenThrow(new UnsupportedOperationException());
            bukkit.when(Bukkit::getOnlinePlayers).then(invocation -> List.of(far, near));
            bukkit.when(() -> Bukkit.isOwnedByCurrentRegion(any(Entity.class))).thenReturn(true);

            OraxenArgument.ParseResult<Player> nearest =
                    new EntitySelectorArgument.OnePlayer("target").parse(commandBlock, List.of("@p"), 0);

            assertTrue(nearest.success());
            assertEquals(near, nearest.value());
        }
    }

    private static Player player(World world, Location location) {
        Player player = mock(Player.class);
        when(player.getWorld()).thenReturn(world);
        when(player.getLocation()).thenReturn(location);
        return player;
    }
}
