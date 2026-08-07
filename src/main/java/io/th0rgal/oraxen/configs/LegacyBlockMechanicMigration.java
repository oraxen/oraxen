package io.th0rgal.oraxen.configs;

import io.th0rgal.oraxen.utils.OraxenYaml;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Migrates the pre-1.215 split block factory configuration to the unified
 * {@code block} mechanic configuration introduced in 1.215.
 */
public final class LegacyBlockMechanicMigration {

    private static final List<String> LEGACY_BLOCK_MECHANICS = List.of(
            "noteblock", "stringblock", "chorusblock", "shaped_block"
    );

    private static final Map<String, String> MODERN_BLOCK_OPTION_KEYS = Map.of(
            "tool_types", "tool-types",
            "farmblock_check_delay", "farmblock-check-delay",
            "sapling_growth_check_delay", "sapling-growth-check-delay",
            "disable_vanilla_strings", "disable-vanilla-strings",
            "remove_mineable_tag", "remove-mineable-tag",
            "convert_vanilla_waxed", "convert-vanilla-waxed",
            "handle_world_generation", "handle-world-generation"
    );

    private static final List<String> LEGACY_BLOCK_SOUND_KEYS = List.of(
            "noteblock_and_block", "stringblock_and_furniture", "chorusblock"
    );

    private LegacyBlockMechanicMigration() {
    }

    public static boolean migrate(@NotNull final YamlConfiguration mechanicsConfig) {
        final boolean soundsMigrated = migrateCustomBlockSounds(mechanicsConfig);
        final List<ConfigurationSection> legacySections = LEGACY_BLOCK_MECHANICS.stream()
                .map(legacyMechanic -> OraxenYaml.getConfigurationSection(mechanicsConfig, legacyMechanic))
                .filter(section -> section != null)
                .toList();
        if (legacySections.isEmpty())
            return soundsMigrated;

        ConfigurationSection blockSection = OraxenYaml.getConfigurationSection(mechanicsConfig, "block");
        if (blockSection == null) {
            blockSection = mechanicsConfig.createSection("block");
            OraxenYaml.invalidateKeyCache(mechanicsConfig);
        }

        final Set<String> toolTypes = new LinkedHashSet<>();
        addToolTypes(toolTypes, blockSection, "tool-types");
        addToolTypes(toolTypes, blockSection, "tool_types");

        boolean enabled = blockSection.getBoolean("enabled", false);
        for (final ConfigurationSection legacySection : legacySections) {
            enabled |= legacySection.getBoolean("enabled", false);
            addToolTypes(toolTypes, legacySection, "tool_types");
            addToolTypes(toolTypes, legacySection, "tool-types");
            copyLegacyFactoryOptions(legacySection, blockSection);
        }

        if (!toolTypes.isEmpty())
            blockSection.set("tool-types", new ArrayList<>(toolTypes));
        blockSection.set("enabled", enabled);
        removeLegacyAliases(blockSection);

        for (final String legacyMechanic : LEGACY_BLOCK_MECHANICS)
            mechanicsConfig.set(legacyMechanic, null);

        OraxenYaml.invalidateKeyCache(mechanicsConfig);
        OraxenYaml.invalidateKeyCache(blockSection);
        return true;
    }

    private static void copyLegacyFactoryOptions(final ConfigurationSection legacySection,
            final ConfigurationSection blockSection) {
        for (final String key : legacySection.getKeys(false)) {
            if (key.equalsIgnoreCase("enabled"))
                continue;

            final String targetKey = MODERN_BLOCK_OPTION_KEYS.getOrDefault(key, key);
            blockSection.set(targetKey, legacySection.get(key));
        }
    }

    private static void removeLegacyAliases(final ConfigurationSection blockSection) {
        for (final Map.Entry<String, String> alias : MODERN_BLOCK_OPTION_KEYS.entrySet()) {
            if (!blockSection.contains(alias.getValue()))
                continue;
            blockSection.set(alias.getKey(), null);
        }
    }

    private static void addToolTypes(final Set<String> toolTypes, final ConfigurationSection section, final String key) {
        for (final Object value : section.getList(key, List.of())) {
            if (value == null)
                continue;

            final String toolType = String.valueOf(value).trim();
            if (toolType.isBlank() || toolType.equalsIgnoreCase("null"))
                continue;

            toolTypes.add(toolType);
        }
    }

    private static boolean migrateCustomBlockSounds(final YamlConfiguration mechanicsConfig) {
        final ConfigurationSection soundsSection = OraxenYaml.getConfigurationSection(mechanicsConfig, "custom_block_sounds");
        if (soundsSection == null)
            return false;

        boolean changed = false;
        boolean hasLegacyBlockSoundKey = false;
        boolean blockSoundsEnabled = false;
        for (final String legacyKey : LEGACY_BLOCK_SOUND_KEYS) {
            if (!soundsSection.contains(legacyKey))
                continue;

            hasLegacyBlockSoundKey = true;
            blockSoundsEnabled |= soundsSection.getBoolean(legacyKey, true);
        }

        if (hasLegacyBlockSoundKey && (!soundsSection.contains("block")
                || soundsSection.getBoolean("block", true) != blockSoundsEnabled)) {
            soundsSection.set("block", blockSoundsEnabled);
            changed = true;
        }

        if (soundsSection.contains("stringblock_and_furniture")) {
            final boolean furnitureSoundsEnabled = soundsSection.getBoolean("stringblock_and_furniture", true);
            if (!soundsSection.contains("furniture")
                    || soundsSection.getBoolean("furniture", true) != furnitureSoundsEnabled) {
                soundsSection.set("furniture", furnitureSoundsEnabled);
                changed = true;
            }
        }

        if (changed)
            OraxenYaml.invalidateKeyCache(soundsSection);
        return changed;
    }
}
