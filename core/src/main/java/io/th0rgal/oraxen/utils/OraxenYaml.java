package io.th0rgal.oraxen.utils;

import io.th0rgal.oraxen.config.Settings;
import io.th0rgal.oraxen.utils.logs.Logs;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.Locale;

public class OraxenYaml extends YamlConfiguration {

    @Nullable
    public static ConfigurationSection getConfigurationSection(@Nullable ConfigurationSection section,
            @NotNull String path) {
        if (section == null)
            return null;

        ConfigurationSection exact = section.getConfigurationSection(path);
        if (exact != null)
            return exact;

        Object value = getIgnoreCase(section, path);
        return value instanceof ConfigurationSection configurationSection ? configurationSection : null;
    }

    public static boolean isConfigurationSection(@Nullable ConfigurationSection section, @NotNull String path) {
        return getConfigurationSection(section, path) != null;
    }

    public static boolean contains(@Nullable ConfigurationSection section, @NotNull String path) {
        return getIgnoreCase(section, path) != null;
    }

    public static boolean getBoolean(@Nullable ConfigurationSection section, @NotNull String path) {
        return getBoolean(section, path, false);
    }

    public static boolean getBoolean(@Nullable ConfigurationSection section, @NotNull String path,
            boolean defaultValue) {
        Object value = getIgnoreCase(section, path);
        if (value instanceof Boolean bool)
            return bool;
        if (value instanceof String string)
            return Boolean.parseBoolean(string);
        return defaultValue;
    }

    public static int getInt(@Nullable ConfigurationSection section, @NotNull String path) {
        return getInt(section, path, 0);
    }

    public static int getInt(@Nullable ConfigurationSection section, @NotNull String path, int defaultValue) {
        Object value = getIgnoreCase(section, path);
        if (value instanceof Number number)
            return number.intValue();
        if (value instanceof String string) {
            try {
                return Integer.parseInt(string);
            } catch (NumberFormatException ignored) {
                return defaultValue;
            }
        }
        return defaultValue;
    }

    @Nullable
    public static String getString(@Nullable ConfigurationSection section, @NotNull String path) {
        Object value = getIgnoreCase(section, path);
        return value != null ? String.valueOf(value) : null;
    }

    @Nullable
    public static Object getIgnoreCase(@Nullable ConfigurationSection section, @NotNull String path) {
        if (section == null)
            return null;

        if (path.isBlank())
            return null;

        String[] parts = path.split("\\.", -1);
        if (parts.length == 0)
            return null;

        for (String part : parts) {
            if (part.isEmpty())
                return null;
        }

        Object exact = section.get(path);
        if (exact != null)
            return exact;

        ConfigurationSection current = section;
        for (int i = 0; i < parts.length; i++) {
            String actualKey = getActualKey(current, parts[i]);
            if (actualKey == null)
                return null;

            if (i == parts.length - 1)
                return current.get(actualKey);

            current = current.getConfigurationSection(actualKey);
            if (current == null)
                return null;
        }

        return null;
    }

    @Nullable
    public static String getActualKey(@Nullable ConfigurationSection section, @NotNull String key) {
        if (section == null)
            return null;
        if (section.contains(key))
            return key;

        for (String existingKey : section.getKeys(false)) {
            if (existingKey.equalsIgnoreCase(key))
                return existingKey;
        }
        return null;
    }

    @Nullable
    public static Material getMaterial(@Nullable String materialName) {
        if (materialName == null || materialName.isBlank())
            return null;

        return Material.getMaterial(materialName.trim().toUpperCase(Locale.ROOT));
    }

    public static boolean isValidYaml(File file) {
        YamlConfiguration config = new YamlConfiguration();
        try {
            config.load(file);
            return true;
        } catch (InvalidConfigurationException e) {
            Logs.logError("Error loading YAML configuration file: " + file.getPath());
            Logs.logError("Ensure that your config is formatted correctly:");
            Logs.logWarning(e.getMessage());
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    @NotNull
    public static YamlConfiguration loadConfiguration(@NotNull File file) throws RuntimeException {
        YamlConfiguration config = new YamlConfiguration();
        try {
            config.load(file);
        } catch (InvalidConfigurationException e) {
            // Handle the exception here (e.g., log the error)
            Logs.logError("Error loading YAML configuration file: " + file.getName());
            Logs.logError("Ensure that your config is formatted correctly:");
            Logs.logWarning(e.getMessage());
            //Logs.logWarning(Arrays.toString(e.getStackTrace()));
            // You can choose to do nothing and keep the existing data in the file
            // or provide default values and continue.
        } catch (IOException e) {
            e.printStackTrace();
        }
        return config;
    }

    @Override
    public void load(@NotNull File file) {
        try {
            super.load(file);
        } catch (Exception e) {
            // Handle the exception here (e.g., log the error)
            Logs.logError("Error loading YAML configuration file: " + file.getName());
            if (Settings.DEBUG.toBool()) Logs.logWarning(e.getMessage());
            // You can choose to do nothing and keep the existing data in the file
            // or provide default values and continue.
        }
    }

    public static void saveConfig(@NotNull File file, @NotNull ConfigurationSection section) {
        try {
            YamlConfiguration config = loadConfiguration(file);
            config.set(section.getCurrentPath(), section);
            config.save(file);
        } catch (Exception e) {
            // Handle the exception here (e.g., log the error)
            Logs.logError("Error saving YAML configuration file: " + file.getName());
            if (Settings.DEBUG.toBool()) Logs.logWarning(e.getMessage());
            // You can choose to do nothing and keep the existing data in the file
            // or provide default values and continue.
        }
    }

    public static void copyConfigurationSection(ConfigurationSection source, ConfigurationSection target) {
        for (String key : source.getKeys(false)) {
            String targetKey = getActualKey(target, key);
            if (targetKey == null)
                targetKey = key;
            Object sourceValue = source.get(key), targetValue = target.get(targetKey);

            if (sourceValue instanceof ConfigurationSection sourceSection) {
                ConfigurationSection targetSection;
                if (targetValue instanceof ConfigurationSection existingSection) {
                    targetSection = existingSection;
                } else targetSection = target.createSection(targetKey);
                copyConfigurationSection(sourceSection, targetSection);
            } else target.set(targetKey, sourceValue);
        }
    }

}
