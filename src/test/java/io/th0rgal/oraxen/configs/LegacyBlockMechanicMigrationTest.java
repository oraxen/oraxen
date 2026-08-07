package io.th0rgal.oraxen.configs;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LegacyBlockMechanicMigrationTest {

    @Test
    void migratesLegacySectionsEvenWhenLegacyBlockSectionExists() {
        YamlConfiguration config = new YamlConfiguration();
        config.set("block.enabled", false);
        config.set("block.tool_types", List.of("WOODEN"));
        config.set("block.tool-types", List.of("STONE"));

        config.set("noteblock.enabled", true);
        config.set("noteblock.tool_types", Arrays.asList("IRON", null, "null"));
        config.set("noteblock.farmblock_check_delay", 2000);
        config.set("noteblock.remove_mineable_tag", true);

        config.set("stringblock.enabled", true);
        config.set("stringblock.tool_types", List.of("DIAMOND"));
        config.set("stringblock.sapling_growth_check_delay", 3000);
        config.set("stringblock.disable_vanilla_strings", true);

        config.set("chorusblock.enabled", false);
        config.set("shaped_block.enabled", true);
        config.set("shaped_block.tool_types", List.of("NETHERITE"));
        config.set("shaped_block.convert_vanilla_waxed", false);
        config.set("shaped_block.handle_world_generation", false);

        config.set("custom_block_sounds.block", true);
        config.set("custom_block_sounds.furniture", true);
        config.set("custom_block_sounds.noteblock_and_block", false);
        config.set("custom_block_sounds.stringblock_and_furniture", false);
        config.set("custom_block_sounds.chorusblock", false);

        assertTrue(LegacyBlockMechanicMigration.migrate(config));

        ConfigurationSection block = config.getConfigurationSection("block");
        assertTrue(block.getBoolean("enabled"));
        assertEquals(List.of("STONE", "WOODEN", "IRON", "DIAMOND", "NETHERITE"), block.getStringList("tool-types"));
        assertNull(block.get("tool_types"));
        assertEquals(2000, block.getInt("farmblock-check-delay"));
        assertTrue(block.getBoolean("remove-mineable-tag"));
        assertEquals(3000, block.getInt("sapling-growth-check-delay"));
        assertTrue(block.getBoolean("disable-vanilla-strings"));
        assertFalse(block.getBoolean("convert-vanilla-waxed"));
        assertFalse(block.getBoolean("handle-world-generation"));

        assertNull(config.getConfigurationSection("noteblock"));
        assertNull(config.getConfigurationSection("stringblock"));
        assertNull(config.getConfigurationSection("chorusblock"));
        assertNull(config.getConfigurationSection("shaped_block"));
        assertFalse(config.getBoolean("custom_block_sounds.block"));
        assertFalse(config.getBoolean("custom_block_sounds.furniture"));
    }

    @Test
    void returnsFalseWhenNothingLegacyExists() {
        YamlConfiguration config = new YamlConfiguration();
        config.set("block.enabled", true);

        assertFalse(LegacyBlockMechanicMigration.migrate(config));
        assertTrue(config.getBoolean("block.enabled"));
    }
}
