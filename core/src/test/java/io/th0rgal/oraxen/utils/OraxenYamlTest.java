package io.th0rgal.oraxen.utils;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class OraxenYamlTest {

    @Test
    void getIgnoreCaseReturnsNullForMalformedDotPaths() {
        YamlConfiguration config = new YamlConfiguration();
        config.set("valid.path", "value");

        assertNull(OraxenYaml.getIgnoreCase(config, "."));
        assertNull(OraxenYaml.getIgnoreCase(config, ".valid"));
        assertNull(OraxenYaml.getIgnoreCase(config, "valid."));
        assertNull(OraxenYaml.getIgnoreCase(config, "valid..path"));
    }

    @Test
    void getIgnoreCaseStillResolvesNestedKeysIgnoringCase() {
        YamlConfiguration config = new YamlConfiguration();
        config.set("Parent.Child", "value");

        assertEquals("value", OraxenYaml.getIgnoreCase(config, "parent.child"));
    }
}
