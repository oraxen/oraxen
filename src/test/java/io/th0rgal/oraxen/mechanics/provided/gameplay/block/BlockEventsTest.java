package io.th0rgal.oraxen.mechanics.provided.gameplay.block;

import io.th0rgal.oraxen.compatibilities.CompatibilitiesManager;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Server;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

class BlockEventsTest {

    @Test
    void conditionsFilterActionsIndividuallyAndAllowNegation() {
        Player player = mock(Player.class);
        when(player.getName()).thenReturn("testPlayer");
        when(player.hasPermission("allowed")).thenReturn(true);
        when(player.getGameMode()).thenReturn(GameMode.SURVIVAL);
        YamlConfiguration config = new YamlConfiguration();
        config.set("events", List.of(Map.of("click", "RIGHT", "actions", List.of(
                Map.of("command", "say allowed", "condition", "player.hasPermission('allowed')"),
                Map.of("command", "say denied", "condition", "!#player.hasPermission('allowed')"),
                Map.of("command", "say survival", "condition", "!#player.gamemode.name() == 'ADVENTURE'"),
                Map.of("command", "say crowded", "condition", "#server.getOnlinePlayers().size() > 10")
        ))));
        BlockEvents events = new BlockEvents(config, "test");
        Server server = mock(Server.class);
        doReturn(java.util.Collections.nCopies(11, player)).when(server).getOnlinePlayers();

        try (MockedStatic<CompatibilitiesManager> compatibilities = mockStatic(CompatibilitiesManager.class);
             MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(Bukkit::getServer).thenReturn(server);
            events.run(player, Action.RIGHT_CLICK_BLOCK);
            verify(player).performCommand("say allowed");
            verify(player).performCommand("say survival");
            verify(player).performCommand("say crowded");
            verify(player, never()).performCommand("say denied");
            assertTrue(events.hasLeftClickEvent() == false);
        }
    }

    @Test
    void opPlayerDispatchesAsRealPlayerAndRestoresOp() {
        Player player = mock(Player.class);
        when(player.getName()).thenReturn("testPlayer");
        BlockEvents events = opPlayerEvents();

        try (MockedStatic<CompatibilitiesManager> compatibilities = mockStatic(CompatibilitiesManager.class);
             MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(() -> Bukkit.dispatchCommand(player, "say hello")).thenAnswer(invocation -> {
                assertSame(player, invocation.getArgument(0));
                verify(player).setOp(true);
                return true;
            });

            events.run(player, Action.RIGHT_CLICK_BLOCK);

            bukkit.verify(() -> Bukkit.dispatchCommand(player, "say hello"));
            verify(player).setOp(false);
        }
    }

    @Test
    void opPlayerRestoresOpWhenCommandThrows() {
        Player player = mock(Player.class);
        when(player.getName()).thenReturn("testPlayer");
        BlockEvents events = opPlayerEvents();

        try (MockedStatic<CompatibilitiesManager> compatibilities = mockStatic(CompatibilitiesManager.class);
             MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(() -> Bukkit.dispatchCommand(player, "say hello"))
                    .thenThrow(new IllegalArgumentException("command failed"));

            assertThrows(IllegalArgumentException.class, () -> events.run(player, Action.RIGHT_CLICK_BLOCK));

            verify(player).setOp(true);
            verify(player).setOp(false);
        }
    }

    private static BlockEvents opPlayerEvents() {
        YamlConfiguration config = new YamlConfiguration();
        config.set("events", List.of(Map.of(
                "click", "RIGHT",
                "actions", List.of(Map.of("command", "say hello", "executor", "OP-PLAYER"))
        )));
        return new BlockEvents(config, "test", "furniture.events");
    }
}
