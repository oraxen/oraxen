package io.th0rgal.oraxen.font;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GlyphGridTest {

    @Test
    void partialTopLevelRowsPreservesLegacyColumns() {
        YamlConfiguration config = new YamlConfiguration();
        config.set("rows", 3);
        config.set("grid.rows", 2);
        config.set("grid.columns", 4);

        GlyphGrid grid = GlyphGrid.fromGlyphConfig(config);

        assertEquals(3, grid.rows());
        assertEquals(4, grid.columns());
    }

    @Test
    void partialTopLevelColumnsPreservesLegacyRows() {
        YamlConfiguration config = new YamlConfiguration();
        config.set("cols", 5);
        config.set("grid.rows", 2);
        config.set("grid.columns", 4);

        GlyphGrid grid = GlyphGrid.fromGlyphConfig(config);

        assertEquals(2, grid.rows());
        assertEquals(5, grid.columns());
    }
}
