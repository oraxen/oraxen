package io.th0rgal.oraxen;

import io.papermc.paper.ServerBuildInfo;
import io.papermc.paper.plugin.bootstrap.BootstrapContext;
import io.papermc.paper.plugin.bootstrap.PluginBootstrap;
import io.th0rgal.oraxen.sounds.SoundConfigMigration;
import io.th0rgal.oraxen.utils.OraxenYaml;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.jar.JarFile;

public final class OraxenPluginBootstrap implements PluginBootstrap {

    @Override
    public void bootstrap(@NotNull BootstrapContext context) {
        final String minecraftVersion;
        try {
            minecraftVersion = ServerBuildInfo.buildInfo().minecraftVersionId();
        } catch (LinkageError | RuntimeException exception) {
            context.getLogger().warn("Could not determine the Minecraft version; skipping Oraxen bootstrap registry changes.");
            return;
        }

        if (atOrAbove(minecraftVersion, 1, 21, 3)) {
            registerFeature(context, "custom paintings",
                    () -> OraxenRegistryBootstrap.registerPaintings(context));
            registerFeature(context, "block tag edits", () -> OraxenRegistryBootstrap.registerBlockTagEdits(context));
        }
        if (atOrAbove(minecraftVersion, 1, 21, 6)) {
            registerFeature(context, "custom jukebox songs",
                    () -> OraxenRegistryBootstrap.registerJukeboxSongs(context));
        }
    }

    static boolean shouldRemoveNoteBlockMineableTag(YamlConfiguration mechanics) {
        ConfigurationSection block = OraxenYaml.getConfigurationSection(mechanics, "block");
        ConfigurationSection legacyNoteBlock = OraxenYaml.getConfigurationSection(mechanics, "noteblock");
        return sectionRequestsMineableTagRemoval(block) || sectionRequestsMineableTagRemoval(legacyNoteBlock);
    }

    private static boolean sectionRequestsMineableTagRemoval(ConfigurationSection section) {
        return section != null && section.getBoolean("enabled", false)
                && booleanAlias(section, "remove-mineable-tag", "remove_mineable_tag");
    }

    private static boolean booleanAlias(ConfigurationSection section, String primary, String alias) {
        if (section == null) return false;
        return section.contains(primary) ? section.getBoolean(primary) : section.getBoolean(alias, false);
    }

    static boolean atOrAbove(String version, int requiredMajor, int requiredMinor, int requiredPatch) {
        String[] parts = version.split("-", 2)[0].split("\\.");
        try {
            int major = Integer.parseInt(parts[0]);
            int minor = parts.length > 1 ? Integer.parseInt(parts[1]) : 0;
            int patch = parts.length > 2 ? Integer.parseInt(parts[2]) : 0;
            if (major != requiredMajor) return major > requiredMajor;
            if (minor != requiredMinor) return minor > requiredMinor;
            return patch >= requiredPatch;
        } catch (NumberFormatException exception) {
            return false;
        }
    }

    static YamlConfiguration loadSoundsConfig(BootstrapContext context) {
        YamlConfiguration migrated = loadSoundsConfigWithLegacyMigration(context.getDataDirectory());
        return migrated != null ? migrated : loadConfig(context, "sounds.yml");
    }

    /**
     * Mirrors the enable-time sound.yml migration so jukebox songs configured before the
     * sounds.yml rename are still registered on the first bootstrap after an upgrade.
     *
     * @return the migrated (and merged) sound configuration, or null when no legacy sound.yml exists
     */
    static YamlConfiguration loadSoundsConfigWithLegacyMigration(Path dataDirectory) {
        Path legacySoundFile = dataDirectory.resolve("sound.yml");
        if (!Files.isRegularFile(legacySoundFile))
            return null;

        YamlConfiguration legacyConfiguration = YamlConfiguration.loadConfiguration(legacySoundFile.toFile());
        SoundConfigMigration.migrateToNewFormat(legacyConfiguration);

        Path soundsFile = dataDirectory.resolve("sounds.yml");
        if (!Files.isRegularFile(soundsFile))
            return legacyConfiguration;

        YamlConfiguration soundsConfiguration = YamlConfiguration.loadConfiguration(soundsFile.toFile());
        SoundConfigMigration.mergeSounds(soundsConfiguration, legacyConfiguration);
        return soundsConfiguration;
    }

    static YamlConfiguration loadConfig(BootstrapContext context, String name) {
        Path configuredFile = context.getDataDirectory().resolve(name);
        if (Files.isRegularFile(configuredFile))
            return YamlConfiguration.loadConfiguration(configuredFile.toFile());

        Path pluginSource = context.getPluginSource();
        try {
            if (Files.isDirectory(pluginSource)) {
                Path bundledFile = pluginSource.resolve(name);
                if (Files.isRegularFile(bundledFile))
                    return YamlConfiguration.loadConfiguration(bundledFile.toFile());
            } else {
                try (JarFile jar = new JarFile(pluginSource.toFile())) {
                    var entry = jar.getJarEntry(name);
                    if (entry != null) {
                        try (InputStream stream = jar.getInputStream(entry);
                                InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                            return YamlConfiguration.loadConfiguration(reader);
                        }
                    }
                }
            }
        } catch (IOException exception) {
            context.getLogger().warn("Failed to read bundled {}", name, exception);
        }
        return new YamlConfiguration();
    }

    private static void registerFeature(BootstrapContext context, String feature, Runnable registration) {
        try {
            registration.run();
        } catch (LinkageError | RuntimeException exception) {
            context.getLogger().warn("Could not register {} on this Paper version", feature, exception);
        }
    }
}
