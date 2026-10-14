package io.th0rgal.oraxen.configs;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.glyphs.AnimatedGlyph;
import io.th0rgal.oraxen.fonts.FontManager;
import io.th0rgal.oraxen.glyphs.Glyph;
import io.th0rgal.oraxen.utils.logs.Logs;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedStatic;

import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class GlyphAscentWarningTest {

    private static final String warning = "One or more glyphs have an ascent greater than their height, which breaks rendering for all glyphs.";
    private static final String guidance = "To fix this, adjust the following glyphs so that their ascent is less than or equal to their height.";

    @TempDir
    Path folder;

    @AfterEach
    void resetAnimatedGlyphCodepoints() {
        AnimatedGlyph.resetCodepointCounter();
    }

    @Test
    void groupsRegularAndAnimatedGlyphsAcrossFilesAndResetsOnReload() throws Exception {
        Path glyphs = Files.createDirectory(folder.resolve("glyphs"));
        Files.writeString(glyphs.resolve("first.yml"), """
                invalid:
                  char: A
                  ascent: 9
                  height: 8
                equal:
                  char: B
                  ascent: 8
                  height: 8
                lower:
                  char: C
                  ascent: 7
                  height: 8
                """);
        Path nested = Files.createDirectory(glyphs.resolve("nested"));
        Files.writeString(nested.resolve("second.yml"), """
                animated:
                  texture: animated.png
                  ascent: 10
                  height: 8
                  animation:
                    frames: 2
                """);

        ConfigsManager manager = manager(glyphs);
        OraxenPlugin plugin = mock(OraxenPlugin.class);
        when(plugin.getDataFolder()).thenReturn(folder.toFile());
        try (MockedStatic<OraxenPlugin> plugins = mockStatic(OraxenPlugin.class);
                MockedStatic<Logs> logs = mockStatic(Logs.class)) {
            plugins.when(OraxenPlugin::get).thenReturn(plugin);

            for (int reload = 0; reload < 2; reload++) {
                ConfigsManager.GlyphParseOutput output = manager.parseAllGlyphConfigs();
                assertEquals(3, output.glyphs().size());
                assertEquals(1, output.animatedGlyphs().size());
                logs.verify(() -> Logs.logError(warning));
                logs.verify(() -> Logs.logWarning(guidance));
                logs.verify(() -> Logs.logWarning("- <#529ced>invalid</#529ced> in <#529ced>glyphs/first.yml</#529ced>"));
                logs.verify(() -> Logs.logWarning("- <#529ced>animated</#529ced> in <#529ced>glyphs/nested/second.yml</#529ced>"));
                logs.verifyNoMoreInteractions();
                logs.clearInvocations();
            }
        }
    }

    @Test
    void legacyParserReportsInvalidGlyphsWithPackFolderPresent() throws Exception {
        Path glyphs = Files.createDirectory(folder.resolve("glyphs"));
        Files.createDirectory(folder.resolve("pack"));
        Files.writeString(glyphs.resolve("legacy.yml"), """
                invalid:
                  char: A
                  texture: minecraft:stone.png
                  ascent: 9
                  height: 8
                """);
        ConfigsManager manager = manager(glyphs);
        OraxenPlugin plugin = mock(OraxenPlugin.class);
        when(plugin.getDataFolder()).thenReturn(folder.toFile());
        try (MockedStatic<OraxenPlugin> plugins = mockStatic(OraxenPlugin.class);
                MockedStatic<Logs> logs = mockStatic(Logs.class)) {
            plugins.when(OraxenPlugin::get).thenReturn(plugin);

            assertEquals(1, manager.parseGlyphConfigs().size());
            logs.verify(() -> Logs.logError(warning));
            logs.verify(() -> Logs.logWarning(guidance));
            logs.verify(() -> Logs.logWarning("- <#529ced>invalid</#529ced> in <#529ced>glyphs/legacy.yml</#529ced>"));
            logs.verifyNoMoreInteractions();
        }
    }

    @Test
    void bitmapWarningsIdentifyFontEntryAndRespectInheritedDimensionsInBothParsers() throws Exception {
        Path glyphs = Files.createDirectory(folder.resolve("glyphs"));
        Files.writeString(glyphs.resolve("bitmap.yml"), """
                inherited:
                  char: A
                  ascent: 8
                  height: 8
                  bitmap:
                    id: icons
                    row: 1
                    column: 1
                valid:
                  char: B
                  ascent: 10
                  height: 8
                  bitmap:
                    id: valid_icons
                    row: 1
                    column: 1
                missing:
                  char: C
                  ascent: 10
                  height: 8
                  bitmap:
                    id: missing_icons
                    row: 1
                    column: 1
                """);
        ConfigsManager manager = manager(glyphs);
        OraxenPlugin plugin = mock(OraxenPlugin.class);
        when(plugin.getDataFolder()).thenReturn(folder.toFile());
        Map<String, FontManager.GlyphBitMap> previousBitmaps = FontManager.glyphBitMaps;
        try (MockedStatic<OraxenPlugin> plugins = mockStatic(OraxenPlugin.class);
                MockedStatic<Logs> logs = mockStatic(Logs.class)) {
            plugins.when(OraxenPlugin::get).thenReturn(plugin);
            FontManager.glyphBitMaps = Map.of(
                    "icons", new FontManager.GlyphBitMap("icons.png", 1, 1, 10, 8),
                    "valid_icons", new FontManager.GlyphBitMap("valid.png", 1, 1, 8, 8));

            for (boolean legacy : new boolean[] { false, true }) {
                Collection<Glyph> output = legacy ? manager.parseGlyphConfigs() : manager.parseAllGlyphConfigs().glyphs();
                assertEquals(3, output.size());
                logs.verify(() -> Logs.logError(warning));
                logs.verify(() -> Logs.logWarning(guidance));
                logs.verify(() -> Logs.logWarning("- <#529ced>inherited</#529ced> in <#529ced>font.yml (bitmaps.icons)</#529ced>"));
                logs.verify(() -> Logs.logWarning("- <#529ced>missing</#529ced> in <#529ced>glyphs/bitmap.yml</#529ced>"));
                logs.verifyNoMoreInteractions();
                logs.clearInvocations();
            }
        } finally {
            FontManager.glyphBitMaps = previousBitmaps;
        }
    }

    @Test
    void validGlyphsDoNotLogAscentWarning() throws Exception {
        Path glyphs = Files.createDirectory(folder.resolve("glyphs"));
        Files.writeString(glyphs.resolve("valid.yml"), """
                valid:
                  char: A
                  ascent: 8
                  height: 8
                """);
        ConfigsManager manager = manager(glyphs);
        OraxenPlugin plugin = mock(OraxenPlugin.class);
        when(plugin.getDataFolder()).thenReturn(folder.toFile());
        try (MockedStatic<OraxenPlugin> plugins = mockStatic(OraxenPlugin.class);
                MockedStatic<Logs> logs = mockStatic(Logs.class)) {
            plugins.when(OraxenPlugin::get).thenReturn(plugin);

            assertEquals(1, manager.parseAllGlyphConfigs().glyphs().size());
            logs.verifyNoInteractions();
        }
    }

    private ConfigsManager manager(Path glyphs) throws Exception {
        ConfigsManager manager = mock(ConfigsManager.class, CALLS_REAL_METHODS);
        Field field = ConfigsManager.class.getDeclaredField("glyphsFolder");
        field.setAccessible(true);
        field.set(manager, glyphs.toFile());
        return manager;
    }
}
