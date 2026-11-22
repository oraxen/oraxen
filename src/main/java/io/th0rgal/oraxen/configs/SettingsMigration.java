package io.th0rgal.oraxen.configs;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Migrates settings only. Item and mechanic configurations must never pass through this migration. */
public final class SettingsMigration {
    private static final String CORE_SHADER_MIGRATION_PATH = "Pack.import.remove_core_shaders_from_imported_packs";
    private static final String INTERMEDIATE_CORE_SHADER_PATH = "pack.import.exclude.core-shaders";
    private static final String EXCLUDE_PATH = "pack.import.exclude";
    private static final String CORE_SHADER_GLOB = "**/assets/minecraft/shaders/core/**";
    private static final Map<String, String> PATHS = loadPaths();

    private SettingsMigration() {
    }

    private static Map<String, String> loadPaths() {
        Map<String, String> paths = new LinkedHashMap<>();
        try (var input = Objects.requireNonNull(SettingsMigration.class.getResourceAsStream("/settings-migration.tsv"));
             var reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
            for (String line : reader.lines().filter(line -> !line.startsWith("#")).toList()) {
                String[] parts = line.split("\t", 2);
                paths.put(parts[0], parts[1]);
            }
        } catch (Exception exception) {
            throw new ExceptionInInitializerError(exception);
        }
        return paths;
    }

    static String newPath(String oldPath) {
        for (var entry : PATHS.entrySet()) {
            if (oldPath.equals(entry.getKey()) || oldPath.startsWith(entry.getKey() + "."))
                return entry.getValue() + oldPath.substring(entry.getKey().length());
        }
        return oldPath;
    }

    /** Explicit new values win conflicts. Defaults must not have been inserted yet. */
    public static boolean migrate(YamlConfiguration settings) {
        String before = settings.saveToString();
        migrateCoreShaderExclusion(settings);
        // The intermediate format wins over the original format when both are present.
        List<String> oldPaths = new java.util.ArrayList<>(settings.getKeys(true));
        oldPaths.sort(java.util.Comparator.comparing(path -> !path.startsWith("pack.obfuscation")));
        for (String oldPath : oldPaths) {
            Object value = settings.get(oldPath);
            if (value instanceof ConfigurationSection section && !section.getKeys(false).isEmpty()) continue;
            String newPath = newPath(oldPath);
            if (newPath.equals(oldPath)) continue;
            if (isChildOfList(settings, oldPath) || isChildOfList(settings, newPath)) continue;
            if (!settings.contains(newPath, true)) {
                boolean inverted = oldPath.equals("Pack.generation.disable_mcmeta_generation");
                if (inverted) value = !settings.getBoolean(oldPath);
                if (value instanceof ConfigurationSection)
                    settings.createSection(newPath);
                else
                    settings.set(newPath, value);
                settings.setComments(newPath, inverted
                        ? List.of("Generate pack.mcmeta and overlays. Disable to use your own file.")
                        : settings.getComments(oldPath));
                settings.setInlineComments(newPath, inverted ? List.of() : settings.getInlineComments(oldPath));
            }
            settings.set(oldPath, null);
        }
        // Remove empty renamed containers without removing unrelated custom settings.
        List<String> keys = new java.util.ArrayList<>(settings.getKeys(true));
        java.util.Collections.reverse(keys);
        for (String path : keys) {
            ConfigurationSection section = settings.getConfigurationSection(path);
            if (section != null && section.getKeys(false).isEmpty()
                    && (!newPath(path).equals(path) || List.of("Plugin", "ConfigsTools", "Misc", "Pack")
                    .stream().anyMatch(root -> path.equals(root) || path.startsWith(root + "."))))
                settings.set(path, null);
        }
        return !before.equals(settings.saveToString());
    }

    private static void migrateCoreShaderExclusion(YamlConfiguration settings) {
        Object explicitExclusions = settings.get(EXCLUDE_PATH);
        if (explicitExclusions instanceof List<?>) {
            // A list is the new format, including an explicitly empty list. Do not replace it.
            removeLegacyKey(settings, CORE_SHADER_MIGRATION_PATH);
            return;
        }

        Boolean intermediate = booleanValue(settings, INTERMEDIATE_CORE_SHADER_PATH);
        Boolean legacy = booleanValue(settings, CORE_SHADER_MIGRATION_PATH);
        Boolean selected = intermediate != null ? intermediate : legacy;
        if (selected == null || !canReplaceExclusions(settings, intermediate != null)) return;

        String sourcePath = intermediate != null ? INTERMEDIATE_CORE_SHADER_PATH : CORE_SHADER_MIGRATION_PATH;
        List<String> comments = settings.getComments(sourcePath);
        List<String> inlineComments = settings.getInlineComments(sourcePath);

        // Remove both spellings before writing the list. The current boolean lives below the
        // destination path, so its now-empty section must be cleared first.
        removeLegacyKey(settings, INTERMEDIATE_CORE_SHADER_PATH);
        removeLegacyKey(settings, CORE_SHADER_MIGRATION_PATH);
        settings.set(EXCLUDE_PATH, selected ? List.of(CORE_SHADER_GLOB) : List.of());
        settings.setComments(EXCLUDE_PATH, comments);
        settings.setInlineComments(EXCLUDE_PATH, inlineComments);
    }

    private static Boolean booleanValue(YamlConfiguration settings, String path) {
        Object value = settings.get(path);
        return value instanceof Boolean enabled ? enabled : null;
    }

    private static boolean canReplaceExclusions(YamlConfiguration settings, boolean hasIntermediateBoolean) {
        Object exclusions = settings.get(EXCLUDE_PATH);
        if (exclusions == null) return true;
        if (!(exclusions instanceof ConfigurationSection section)) return false;
        if (section.getKeys(false).isEmpty()) return true;
        return hasIntermediateBoolean && section.getKeys(false).equals(java.util.Set.of("core-shaders"));
    }

    private static void removeLegacyKey(YamlConfiguration settings, String path) {
        if (!settings.contains(path, true) || isChildOfList(settings, path)) return;
        settings.set(path, null);

        String parentPath = path;
        int separator;
        while ((separator = parentPath.lastIndexOf('.')) > 0) {
            parentPath = parentPath.substring(0, separator);
            ConfigurationSection parent = settings.getConfigurationSection(parentPath);
            if (parent == null || !parent.getKeys(false).isEmpty()) break;
            settings.set(parentPath, null);
        }
    }

    private static boolean isChildOfList(YamlConfiguration settings, String path) {
        int separator = path.indexOf('.');
        while (separator >= 0) {
            String parentPath = path.substring(0, separator);
            if (settings.get(parentPath) instanceof List<?>) return true;
            separator = path.indexOf('.', separator + 1);
        }
        return false;
    }
}
