package io.th0rgal.oraxen.pack.receive;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.configs.ConfigsManager;
import io.th0rgal.oraxen.utils.logs.Logs;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.MockedStatic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

class PackActionTest {
    private static final String ACTIONS_PATH = "pack.receive.denied.actions";
    private MockedStatic<OraxenPlugin> pluginAccess;
    private MockedStatic<Logs> logs;

    @BeforeEach
    void setup() {
        OraxenPlugin plugin = mock(OraxenPlugin.class);
        ConfigsManager configs = mock(ConfigsManager.class);
        YamlConfiguration language = new YamlConfiguration();
        language.set("general.prefix", "");
        when(plugin.getConfigsManager()).thenReturn(configs);
        when(configs.getLanguage()).thenReturn(language);
        pluginAccess = mockStatic(OraxenPlugin.class);
        pluginAccess.when(OraxenPlugin::get).thenReturn(plugin);
        logs = mockStatic(Logs.class);
    }

    @AfterEach
    void cleanup() {
        logs.close();
        pluginAccess.close();
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = "POPUP")
    void invalidOrMissingMessageTypeFallsBackToChat(String type) {
        YamlConfiguration settings = new YamlConfiguration();
        settings.set(ACTIONS_PATH + ".message.enabled", true);
        settings.set(ACTIONS_PATH + ".message.type", type);
        settings.set(ACTIONS_PATH + ".message.content", "Accept the pack");

        PackAction action = new PackAction(settings.getConfigurationSection(ACTIONS_PATH), TagResolver.empty());

        assertEquals("CHAT", String.valueOf(action.getMessageType()));
        logs.verify(() -> Logs.logWarning(contains(ACTIONS_PATH + ".message.type")));
    }
}
