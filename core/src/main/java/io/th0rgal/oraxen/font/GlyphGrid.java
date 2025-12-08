package io.th0rgal.oraxen.font;

import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Encapsulates the grid configuration for multi-character glyphs.
 * Supports rows/columns for sprite sheets and large textures.
 *
 * @param rows    Number of rows in the grid (default 1)
 * @param columns Number of columns in the grid (default 1)
 */
public record GlyphGrid(int rows, int columns) {

    /**
     * Default single-cell grid for non-grid glyphs.
     */
    public static final GlyphGrid SINGLE = new GlyphGrid(1, 1);

    /**
     * Creates a GlyphGrid from a configuration section.
     *
     * @param section The "grid" configuration section, may be null
     * @return A GlyphGrid with the configured rows/columns, or SINGLE if section is
     *         null
     */
    @NotNull
    public static GlyphGrid fromConfig(@Nullable ConfigurationSection section) {
        if (section == null)
            return SINGLE;
        int rows = section.getInt("rows", 1);
        int columns = section.getInt("columns", 1);
        if (rows < 1)
            rows = 1;
        if (columns < 1)
            columns = 1;
        return new GlyphGrid(rows, columns);
    }

    /**
     * Returns the total number of cells in the grid.
     *
     * @return rows * columns
     */
    public int totalCells() {
        return rows * columns;
    }

    /**
     * Checks if this grid requires multiple characters.
     *
     * @return true if the grid has more than one cell
     */
    public boolean isMultiCell() {
        return totalCells() > 1;
    }
}
