package io.th0rgal.oraxen.mechanics.provided.gameplay.block;

import io.papermc.paper.threadedregions.scheduler.EntityScheduler;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.compatibilities.CompatibilitiesManager;
import io.th0rgal.oraxen.utils.logs.Logs;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Server;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.mockito.MockedStatic;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

class BlockEventsTest {

    @Test
    void cooldownAppliesOnlyToItsAction() {
        Player player = mock(Player.class);
        when(player.getName()).thenReturn("testPlayer");
        when(player.getUniqueId()).thenReturn(UUID.randomUUID());
        YamlConfiguration config = new YamlConfiguration();
        config.set("events", List.of(Map.of("actions", List.of(
                Map.of("command", "say limited", "cooldown", "1h"),
                Map.of("command", "say unlimited")
        ))));
        BlockEvents events = new BlockEvents(config, "test");

        try (MockedStatic<CompatibilitiesManager> compatibilities = mockStatic(CompatibilitiesManager.class)) {
            events.run(player, Action.RIGHT_CLICK_BLOCK);
            events.run(player, Action.RIGHT_CLICK_BLOCK);

            verify(player).performCommand("say limited");
            verify(player, times(2)).performCommand("say unlimited");
        }
    }

    @Test
    void failingConditionDoesNotConsumeCooldown() {
        Player player = mock(Player.class);
        when(player.getName()).thenReturn("testPlayer");
        when(player.getUniqueId()).thenReturn(UUID.randomUUID());
        when(player.hasPermission("x")).thenReturn(false, true);
        YamlConfiguration config = new YamlConfiguration();
        config.set("events", List.of(Map.of("actions", List.of(
                Map.of("command", "say allowed", "condition", "player.hasPermission('x')", "cooldown", "1h")
        ))));
        BlockEvents events = new BlockEvents(config, "test");

        try (MockedStatic<CompatibilitiesManager> compatibilities = mockStatic(CompatibilitiesManager.class)) {
            events.run(player, Action.RIGHT_CLICK_BLOCK);
            verify(player, never()).performCommand("say allowed");
            events.run(player, Action.RIGHT_CLICK_BLOCK);
            verify(player).performCommand("say allowed");
        }
    }

    @Test
    void invalidCooldownBlocksAction() {
        Player player = mock(Player.class);
        YamlConfiguration config = new YamlConfiguration();
        config.set("events", List.of(Map.of("actions", List.of(
                Map.of("command", "say blocked", "cooldown", "soon")
        ))));

        try (MockedStatic<CompatibilitiesManager> compatibilities = mockStatic(CompatibilitiesManager.class);
             MockedStatic<Logs> logs = mockStatic(Logs.class)) {
            BlockEvents events = new BlockEvents(config, "test", "furniture.events");
            events.run(player, Action.RIGHT_CLICK_BLOCK);

            verify(player, never()).performCommand("say blocked");
            logs.verify(() -> Logs.logWarning("Invalid furniture.events cooldown 'soon' in test; blocking action."));
        }
    }

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

    @Test
    void breakAndClickTriggersRunOnlyTheirOwnActions() {
        Player player = mock(Player.class);
        when(player.getName()).thenReturn("testPlayer");
        YamlConfiguration config = new YamlConfiguration();
        config.set("events", List.of(
                Map.of("trigger", "break", "actions", List.of(
                        Map.of("command", "say broken <player>"),
                        Map.of("command", "say denied", "condition", "player.hasPermission('denied')"))),
                Map.of("trigger", "click", "type", "RIGHT", "click", "LEFT",
                        "actions", List.of(Map.of("command", "say clicked")))));
        BlockEvents events = new BlockEvents(config, "test");

        try (MockedStatic<CompatibilitiesManager> compatibilities = mockStatic(CompatibilitiesManager.class)) {
            assertFalse(events.hasLeftClickEvent());
            assertTrue(events.hasClickEvents());
            assertFalse(events.run(player, Action.LEFT_CLICK_BLOCK));
            assertTrue(events.run(player, Action.RIGHT_CLICK_BLOCK));
            verify(player).performCommand("say clicked");
            verify(player, never()).performCommand("say broken testPlayer");
            assertTrue(events.runBreak(player));
            verify(player).performCommand("say broken testPlayer");
            verify(player, never()).performCommand("say denied");
            verify(player, times(1)).performCommand("say clicked");
        }
    }

    @Test
    void omittedTriggerPreservesLegacyClickFilterAndBothDefault() {
        Player player = mock(Player.class);
        when(player.getName()).thenReturn("testPlayer");
        YamlConfiguration config = new YamlConfiguration();
        config.set("events", List.of(
                Map.of("click", "RIGHT", "actions", List.of(Map.of("command", "say right"))),
                Map.of("actions", List.of(Map.of("command", "say both")))));
        BlockEvents events = new BlockEvents(config, "test");

        try (MockedStatic<CompatibilitiesManager> compatibilities = mockStatic(CompatibilitiesManager.class)) {
            assertTrue(events.hasLeftClickEvent());
            assertFalse(events.runBreak(player));
            events.run(player, Action.LEFT_CLICK_BLOCK);
            verify(player, never()).performCommand("say right");
            events.run(player, Action.RIGHT_CLICK_BLOCK);
            verify(player).performCommand("say right");
            verify(player, times(2)).performCommand("say both");
        }
    }

    @Test
    void breakOnlyEventsDoNotCountAsLeftClicks() {
        YamlConfiguration config = new YamlConfiguration();
        config.set("events", List.of(Map.of("trigger", "break",
                "actions", List.of(Map.of("command", "say broken")))));
        BlockEvents events = new BlockEvents(config, "test", "furniture.events");

        assertFalse(events.isEmpty());
        assertFalse(events.hasClickEvents());
        assertFalse(events.hasLeftClickEvent());
        assertFalse(events.run(mock(Player.class), Action.LEFT_CLICK_BLOCK));
        assertFalse(events.run(mock(Player.class), Action.RIGHT_CLICK_BLOCK));
    }

    @Test
    void placeTriggerRunsOnlyOnPlacement() {
        Player player = mock(Player.class);
        when(player.getName()).thenReturn("testPlayer");
        YamlConfiguration config = new YamlConfiguration();
        config.set("events", List.of(
                Map.of("trigger", "PLACE", "actions", List.of(Map.of("command", "say placed <player>"))),
                Map.of("trigger", "break", "actions", List.of(Map.of("command", "say broken")))));
        BlockEvents events = new BlockEvents(config, "test");

        try (MockedStatic<CompatibilitiesManager> compatibilities = mockStatic(CompatibilitiesManager.class)) {
            assertFalse(events.hasClickEvents());
            assertFalse(events.hasLeftClickEvent());
            assertFalse(events.run(player, Action.RIGHT_CLICK_BLOCK));
            assertTrue(events.runBreak(player));
            verify(player, never()).performCommand("say placed testPlayer");
            assertTrue(events.runPlace(player));
            verify(player).performCommand("say placed testPlayer");
            verify(player, times(1)).performCommand("say broken");
        }
    }

    @Test
    void multilineJavaActionUsesPlayerAndServerAndRespectsConditionsAndCooldown() throws Exception {
        String yaml = """
                events:
                  - trigger: click
                    type: RIGHT
                    actions:
                      - java: |
                          int reward = player.isSneaking() ? 2 : 1;
                          player.giveExp(reward);
                          player.sendMessage(net.kyori.adventure.text.Component.text(server.getName()));
                        condition: 'player.hasPermission("reward")'
                        cooldown: 1h
                """;
        YamlConfiguration config = new YamlConfiguration();
        config.loadFromString(yaml);
        Player player = mock(Player.class);
        Server server = mock(Server.class);
        when(player.getUniqueId()).thenReturn(UUID.randomUUID());
        when(player.isSneaking()).thenReturn(true);
        when(player.hasPermission("reward")).thenReturn(false, true);
        when(server.getName()).thenReturn("Paper");

        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(Bukkit::getServer).thenReturn(server);
            bukkit.when(() -> Bukkit.isOwnedByCurrentRegion(player)).thenReturn(true);
            BlockEvents events = new BlockEvents(config, "test", "furniture.events");

            assertFalse(events.run(player, Action.LEFT_CLICK_BLOCK));
            assertTrue(events.run(player, Action.RIGHT_CLICK_BLOCK));
            verify(player, never()).giveExp(anyInt());
            assertTrue(events.run(player, Action.RIGHT_CLICK_BLOCK));
            assertTrue(events.run(player, Action.RIGHT_CLICK_BLOCK));
            verify(player).giveExp(2);
            verify(player).sendMessage(Component.text("Paper"));

            config.loadFromString(yaml.replace("? 2 : 1", "? 3 : 1"));
            BlockEvents reloaded = new BlockEvents(config, "test", "furniture.events");
            assertTrue(reloaded.run(player, Action.RIGHT_CLICK_BLOCK));
            verify(player).giveExp(3);
            verify(player, times(2)).sendMessage(Component.text("Paper"));
        }
    }

    @Test
    void badJavaActionsDoNotStopLaterActions() throws Exception {
        YamlConfiguration config = new YamlConfiguration();
        config.loadFromString("""
                events:
                  - actions:
                      - java: |
                          int good = 1;
                          player.noSuchMethod();
                      - java: 'player.giveExp(10);'
                        command: say mixed
                      - java: 42
                      - java: '  '
                      - java: 'throw new IllegalStateException("script failed");'
                      - command: say still ran
                """);
        Player player = mock(Player.class);
        Logger logger = mock(Logger.class);
        when(player.getName()).thenReturn("testPlayer");

        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class);
             MockedStatic<CompatibilitiesManager> compatibilities = mockStatic(CompatibilitiesManager.class);
             MockedStatic<Logs> logs = mockStatic(Logs.class)) {
            bukkit.when(Bukkit::getLogger).thenReturn(logger);
            bukkit.when(() -> Bukkit.isOwnedByCurrentRegion(player)).thenReturn(true);
            BlockEvents events = new BlockEvents(config, "test", "furniture.events");

            logs.verify(() -> Logs.logWarning("Failed to compile Java action in furniture.events of test; skipping action."));
            verify(logger).warning(argThat((String message) -> message.contains("Line 2")));
            logs.verify(() -> Logs.logWarning("Java action in furniture.events of test cannot be combined with another action type; skipping action."));
            logs.verify(() -> Logs.logWarning("Invalid Java action in furniture.events of test; expected a non-blank string."), times(2));

            assertTrue(events.run(player, Action.RIGHT_CLICK_BLOCK));
            logs.verify(() -> Logs.logWarning("Failed to run Java action in furniture.events of test."));
            verify(player, never()).giveExp(10);
            verify(player, never()).performCommand("say mixed");
            verify(player).performCommand("say still ran");
        }
    }

    @Test
    void matchingActionsStayOrderedWhenScheduledOnPlayer() throws Exception {
        YamlConfiguration config = new YamlConfiguration();
        config.loadFromString("""
                events:
                  - trigger: break
                    actions:
                      - command: say before
                      - java: 'player.giveExp(1);'
                  - trigger: break
                    actions:
                      - command: say after
                  - trigger: click
                    actions:
                      - command: say clicked
                """);
        Player player = mock(Player.class);
        EntityScheduler scheduler = mock(EntityScheduler.class);
        ScheduledTask task = mock(ScheduledTask.class);
        AtomicReference<Consumer<ScheduledTask>> callback = new AtomicReference<>();
        when(player.getName()).thenReturn("testPlayer");
        when(player.getScheduler()).thenReturn(scheduler);
        when(scheduler.run(isNull(), any(), isNull())).thenAnswer(invocation -> {
            callback.set(invocation.getArgument(1));
            return task;
        });

        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class);
             MockedStatic<OraxenPlugin> plugin = mockStatic(OraxenPlugin.class);
             MockedStatic<CompatibilitiesManager> compatibilities = mockStatic(CompatibilitiesManager.class)) {
            bukkit.when(() -> Bukkit.isOwnedByCurrentRegion(player)).thenReturn(false);
            BlockEvents events = new BlockEvents(config, "test");

            assertTrue(events.run(player, Action.RIGHT_CLICK_BLOCK));
            verify(player).performCommand("say clicked");
            verifyNoInteractions(scheduler);

            assertTrue(events.runBreak(player));
            verify(player, never()).performCommand("say before");
            verify(player, never()).giveExp(1);
            verify(player, never()).performCommand("say after");
            assertTrue(callback.get() != null);
            callback.get().accept(task);

            InOrder order = inOrder(player);
            order.verify(player).performCommand("say before");
            order.verify(player).giveExp(1);
            order.verify(player).performCommand("say after");
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
