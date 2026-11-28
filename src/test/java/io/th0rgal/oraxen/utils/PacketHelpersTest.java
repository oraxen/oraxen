package io.th0rgal.oraxen.utils;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.configs.ConfigsManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentIteratorType;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.parallel.Isolated;
import org.mockito.MockedStatic;

import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
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
    void backslashesKeepInlineAndExistingColors() {
        Component expected = Component.text("C:\\temp", NamedTextColor.GREEN);
        assertBothPaths(expected, Component.text("<green>C:\\temp</green>"));
        assertBothPaths(expected, expected);
    }

    @Test
    void structuredJsonKeepsEscapedBackslashes() {
        Component input = Component.translatable("example.key", Component.text("C:\\new\\users"));
        assertBothPaths(input, input);
    }

    @Test
    void existingResetAndCompactRgbStillWork() {
        assertBothPaths(Component.text("Hello"), Component.text("<reset>Hello", NamedTextColor.BLUE));
        assertBothPaths(Component.text("Hello", TextColor.color(0xff0000)), Component.text("&#ff0000Hello"));
    }

    @Test
    void legacyDecorationDoesNotSplitMiniMessageGradient() {
        assertBothPaths(Component.text("<gradient:red:blue>A &lB</gradient>"), formatted -> {
            assertEquals("A B", AdventureUtils.PLAIN_TEXT.serialize(formatted));
            for (Component part : formatted.iterable(ComponentIteratorType.DEPTH_FIRST)) {
                if (part instanceof TextComponent text && text.content().equals("B")) {
                    assertEquals(NamedTextColor.BLUE, text.color());
                    assertEquals(TextDecoration.State.TRUE, text.decoration(TextDecoration.BOLD));
                    return;
                }
            }
            fail("Expected a styled B in the gradient");
        });
    }

    private void assertSameText(String text) {
        assertBothPaths(Component.text(text), Component.text(text));
    }

    private void assertBothPaths(Component expected, Component input) {
        assertBothPaths(input, actual -> assertEquals(expected.compact(), actual.compact()));
    }

    private void assertBothPaths(Component input, Consumer<Component> assertion) {
        assertAll(
                () -> assertion.accept(PacketHelpers.translateTitle(input)),
                () -> assertion.accept(AdventureUtils.GSON_SERIALIZER.deserialize(
                        PacketHelpers.translateJson(AdventureUtils.GSON_SERIALIZER.serialize(input)))));
    }
}
