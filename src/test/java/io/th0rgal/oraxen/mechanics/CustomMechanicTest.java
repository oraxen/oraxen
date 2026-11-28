package io.th0rgal.oraxen.mechanics;

import io.th0rgal.oraxen.mechanics.provided.misc.custom.CustomMechanic;
import io.th0rgal.oraxen.mechanics.provided.misc.custom.CustomMechanicFactory;
import io.th0rgal.oraxen.utils.actions.ClickAction;
import io.th0rgal.oraxen.utils.logs.Logs;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.PluginManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CustomMechanicTest extends MechanicTestSupport {

    private final PluginManager pluginManager = mock(PluginManager.class);
    private PluginManager previousPluginManager;
    private MockedStatic<ClickAction> clickActions;

    @BeforeEach
    void mockActionsAndListeners() {
        clickActions = mockStatic(ClickAction.class);
        clickActions.when(() -> ClickAction.from(any(ConfigurationSection.class))).thenReturn(mock(ClickAction.class));
        previousPluginManager = Bukkit.getPluginManager();
        when(Bukkit.getServer().getPluginManager()).thenReturn(pluginManager);
    }

    @AfterEach
    void restorePluginManager() {
        CustomMechanic.unregisterListeners();
        clickActions.close();
        when(Bukkit.getServer().getPluginManager()).thenReturn(previousPluginManager);
    }

    @Test
    void emptyConfigRegistersNoCustomListeners() {
        CustomMechanic mechanic = new CustomMechanic(mechanicFactory(), mechanicSection("custom"));

        assertEquals("test_item", mechanic.getItemID());
    }

    @Test
    void invalidEventsAreSkippedWithAnErrorNamingTheItemAndValidValues() {
        ConfigurationSection section = mechanicSection("custom",
                "misspelled", Map.of("event", "CLIK", "actions", List.of("[message] hi")),
                "only_colons", Map.of("event", "::", "actions", List.of("[message] hi")),
                "bad_click", Map.of("event", "CLICK:middle", "actions", List.of("[message] hi")));

        try (MockedStatic<Logs> logs = mockStatic(Logs.class)) {
            new CustomMechanic(mechanicFactory(), section);

            logs.verify(() -> Logs.logError(argThat(message -> message.contains("misspelled")
                    && message.contains("test_item") && message.contains("BREAK, CLICK"))));
            logs.verify(() -> Logs.logError(argThat(message -> message.contains("only_colons")
                    && message.contains("BREAK, CLICK"))));
            logs.verify(() -> Logs.logError(argThat(message -> message.contains("bad_click")
                    && message.contains("right, left, all"))));
        }
        verify(pluginManager, never()).registerEvents(any(), any());
    }

    @Test
    void unregisteringTheFactoryUnregistersLoadedListeners() {
        new CustomMechanic(mechanicFactory(), mechanicSection("custom",
                "on_click", Map.of("event", "CLICK", "actions", List.of("[message] hi"))));
        ArgumentCaptor<Listener> registered = ArgumentCaptor.forClass(Listener.class);
        verify(pluginManager).registerEvents(registered.capture(), any());

        try (MockedStatic<HandlerList> handlers = mockStatic(HandlerList.class)) {
            new CustomMechanicFactory(mechanicSection("custom")).onUnregister();

            handlers.verify(() -> HandlerList.unregisterAll(registered.getValue()));
        }
    }
}
