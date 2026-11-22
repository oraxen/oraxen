package io.th0rgal.oraxen.pack.generation;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.configs.ConfigsManager;
import io.th0rgal.oraxen.utils.logs.Logs;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.*;

class PackExclusionsTest {
    private OraxenPlugin plugin;
    private ConfigsManager configs;
    private YamlConfiguration settings;
    private MockedStatic<OraxenPlugin> pluginAccess;
    private MockedStatic<Logs> logs;

    @BeforeEach
    void setUp() {
        plugin = mock(OraxenPlugin.class);
        configs = mock(ConfigsManager.class);
        settings = new YamlConfiguration();
        when(plugin.getConfigsManager()).thenReturn(configs);
        when(configs.getSettings()).thenAnswer(ignored -> settings);
        pluginAccess = mockStatic(OraxenPlugin.class);
        pluginAccess.when(OraxenPlugin::get).thenReturn(plugin);
        logs = mockStatic(Logs.class);
    }

    @AfterEach
    void tearDown() {
        logs.close();
        pluginAccess.close();
    }

    @Test
    void matchesRootAndNestedPathsWithSegmentAndRecursiveWildcards() {
        PackExclusions exclusions = PackExclusions.compile(List.of(
                "**/*.md",
                "assets/*/textures/item/a?.png",
                "assets/**/models/*.json"), ignored -> fail("Valid rules should not warn"));

        assertTrue(exclusions.matches("README.MD"));
        assertTrue(exclusions.matches("docs/guide.md"));
        assertTrue(exclusions.matches("assets/minecraft/textures/item/a1.png"));
        assertFalse(exclusions.matches("assets/minecraft/textures/item/a12.png"));
        assertFalse(exclusions.matches("assets/minecraft/textures/item/nested/a1.png"));
        assertTrue(exclusions.matches("assets/models/item.json"));
        assertTrue(exclusions.matches("assets/minecraft/models/item.json"));
        assertTrue(exclusions.matches("assets/minecraft/deep/models/item.json"));
        assertFalse(exclusions.matches("assets/minecraft/deep/models/item/child.json"));
        assertTrue(exclusions.matches("assets\\minecraft\\models\\item.json"));
    }

    @Test
    void matchesWholePathsAndKeepsCompiledRulesAfterInputListChanges() {
        List<Object> configured = new ArrayList<>(List.of("*.md"));
        PackExclusions exclusions = PackExclusions.compile(configured, ignored -> fail("Valid rules should not warn"));

        configured.set(0, "*.zip");

        assertTrue(exclusions.matches("notes.md"));
        assertFalse(exclusions.matches("nested/notes.md"));
        assertFalse(exclusions.matches("notes.zip"));
    }

    @Test
    void skipsUnsupportedRulesAndWarnsOnceForTheConfiguration() {
        List<String> warnings = new ArrayList<>();
        List<Object> configured = Arrays.asList(
                "**/*.zip", "!**/keep.zip", "file[1].txt", "*.{txt,md}", "folder\\file.txt", 7, " ");

        PackExclusions exclusions = PackExclusions.compile(configured, warnings::add);

        assertTrue(exclusions.matches("assets/tools/archive.ZIP"));
        assertTrue(exclusions.matches("assets/tools/keep.zip"));
        assertEquals(1, warnings.size());
        assertTrue(warnings.getFirst().contains("Skipped invalid pack.import.exclude rules"));
    }

    @Test
    void loadWarnsOncePerSettingsObjectAndRefreshesForReplacementConfiguration() {
        settings.set("pack.import.exclude", Arrays.asList("!bad", "**/private.dat"));

        PackExclusions firstBuild = PackExclusions.load();
        PackExclusions repeatedLoad = PackExclusions.load();

        assertTrue(firstBuild.matches("private.dat"));
        assertTrue(repeatedLoad.matches("folder/private.dat"));
        logs.verify(() -> Logs.logWarning(contains("Skipped invalid pack.import.exclude rules")), times(1));

        settings = new YamlConfiguration();
        settings.set("pack.import.exclude", List.of("**/*.md", "!new-invalid"));
        PackExclusions refreshedBuild = PackExclusions.load();

        assertTrue(refreshedBuild.matches("notes/README.MD"));
        assertFalse(refreshedBuild.matches("folder/private.dat"));
        assertFalse(firstBuild.matches("notes/README.MD"));
        PackExclusions.load();
        logs.verify(() -> Logs.logWarning(contains("Skipped invalid pack.import.exclude rules")), times(2));
    }
}
