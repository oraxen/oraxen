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

/** Renames settings only. Item and mechanic configurations must never pass through this migration. */
public final class SettingsMigration {
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
        for (String oldPath : List.copyOf(settings.getKeys(true))) {
            Object value = settings.get(oldPath);
            if (value instanceof ConfigurationSection) continue;
            String newPath = newPath(oldPath);
            if (newPath.equals(oldPath)) continue;
            if (!settings.contains(newPath, true)) {
                boolean inverted = oldPath.equals("Pack.generation.disable_mcmeta_generation");
                if (inverted) value = !settings.getBoolean(oldPath);
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
}
