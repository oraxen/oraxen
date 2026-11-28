package io.th0rgal.oraxen.pack.dispatch;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.configs.ConfigsManager;
import io.th0rgal.oraxen.configs.Settings;
import io.th0rgal.oraxen.pack.upload.hosts.HostingProvider;
import io.th0rgal.oraxen.utils.SchedulerUtil;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerJoinEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.MockedStatic;

import java.util.stream.Stream;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

class BukkitPackSenderTest {
    private final YamlConfiguration settings = new YamlConfiguration();
    private MockedStatic<OraxenPlugin> pluginAccess;
    private MockedStatic<SchedulerUtil> scheduler;

    @BeforeEach
    void setup() {
        OraxenPlugin plugin = mock(OraxenPlugin.class);
        ConfigsManager configs = mock(ConfigsManager.class);
        when(plugin.getConfigsManager()).thenReturn(configs);
        when(configs.getSettings()).thenReturn(settings);
        pluginAccess = mockStatic(OraxenPlugin.class);
        pluginAccess.when(OraxenPlugin::get).thenReturn(plugin);
        scheduler = mockStatic(SchedulerUtil.class);
        settings.set(Settings.SEND_PACK.getPath(), true);
        settings.set(Settings.SEND_PACK_MODE.getPath(), "JOIN");
        settings.set(Settings.SEND_PACK_PROMPT.getPath(), "");
        settings.set(Settings.SEND_PACK_MANDATORY.getPath(), true);
    }

    @AfterEach
    void cleanup() {
        Settings.invalidateCache();
        scheduler.close();
        pluginAccess.close();
    }

    static Stream<Arguments> delays() {
        return Stream.of(
                Arguments.of(5, 100L),
                Arguments.of(5.0, 100L),
                Arguments.of("5s", 100L),
                Arguments.of("100 ticks", 100L),
                Arguments.of("2500ms", 50L));
    }

    @ParameterizedTest
    @MethodSource("delays")
    void schedulesTheJoinDispatchAfterTheConfiguredDelay(Object delay, long expectedTicks) {
        settings.set(Settings.SEND_PACK_DELAY.getPath(), delay);
        Settings.invalidateCache();
        Player player = mock(Player.class);
        PlayerJoinEvent event = mock(PlayerJoinEvent.class);
        when(event.getPlayer()).thenReturn(player);

        new BukkitPackSender(mock(HostingProvider.class)).onPlayerConnect(event);

        scheduler.verify(() -> SchedulerUtil.runForEntityLater(eq(player), eq(expectedTicks), any(Runnable.class), isNull()));
    }
}
