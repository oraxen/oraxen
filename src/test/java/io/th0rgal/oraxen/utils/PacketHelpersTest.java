package io.th0rgal.oraxen.utils;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.configs.ConfigsManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.parallel.Isolated;
import org.mockito.MockedStatic;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

@Isolated
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PacketHelpersTest {

    private MockedStatic<OraxenPlugin> pluginAccess;

    @BeforeAll
    void setupConfigs() {
        OraxenPlugin plugin = mock(OraxenPlugin.class);
        ConfigsManager configs = mock(ConfigsManager.class);
        YamlConfiguration language = new YamlConfiguration();
        language.set("general.prefix", "");
        when(plugin.getConfigsManager()).thenReturn(configs);
        when(configs.getLanguage()).thenReturn(language);
        when(configs.getSettings()).thenReturn(new YamlConfiguration());
        pluginAccess = mockStatic(OraxenPlugin.class);
        pluginAccess.when(OraxenPlugin::get).thenReturn(plugin);
        AdventureUtils.MINI_MESSAGE.deserialize("");
    }

    @AfterAll
    void closeConfigs() {
        pluginAccess.close();
    }

    @Test
    void plainBackslashesArePreserved() {
        assertSameText("C:\\temp");
        assertSameText("C:\\new\\users \"quoted\"");
        assertSameText("\\\\server\\share");
        assertSameText("trailing \\");
    }

    @Test
    void miniMessageEscapesAndTagsArePreserved() {
        assertBothPaths(Component.text("<red>literal"), Component.text("\\<red>literal"));
        assertBothPaths(
                Component.text("label").hoverEvent(HoverEvent.showText(Component.text("it's here"))),
                Component.text("<hover:show_text:'it\\'s here'>label</hover>"));
    }

    @Test
    void existingStylesAndChildrenArePreserved() {
        Component input = Component.text("<red>C:\\temp</red>", NamedTextColor.BLUE)
                .append(Component.translatable("container.chest"));
        Component expected = Component.text("", NamedTextColor.BLUE)
                .append(Component.text("C:\\temp", NamedTextColor.RED))
                .append(Component.translatable("container.chest"));
        assertBothPaths(expected, input);
    }

    @Test
    void legacyColorsAreParsedWithoutRemovingBackslashes() {
        assertBothPaths(Component.text("C:\\temp", NamedTextColor.GREEN), Component.text("&aC:\\temp"));
    }

    private void assertSameText(String text) {
        assertBothPaths(Component.text(text), Component.text(text));
    }

    private void assertBothPaths(Component expected, Component input) {
        assertAll(
                () -> assertEquals(expected.compact(), PacketHelpers.translateTitle(input).compact()),
                () -> assertEquals(expected.compact(), AdventureUtils.GSON_SERIALIZER.deserialize(
                        PacketHelpers.translateJson(AdventureUtils.GSON_SERIALIZER.serialize(input))).compact()));
    }
}
