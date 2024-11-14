package io.th0rgal.oraxen.utils;

import io.th0rgal.oraxen.config.Settings;
import io.th0rgal.oraxen.utils.logs.Logs;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;

public class OraxenYaml extends YamlConfiguration {

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
            Object sourceValue = source.get(key), targetValue = target.get(key);

            if (sourceValue instanceof ConfigurationSection sourceSection) {
                ConfigurationSection targetSection;
                if (targetValue instanceof ConfigurationSection existingSection) {
                    targetSection = existingSection;
                } else targetSection = target.createSection(key);
                copyConfigurationSection(sourceSection, targetSection);
            } else target.set(key, sourceValue);
        }
    }

}
