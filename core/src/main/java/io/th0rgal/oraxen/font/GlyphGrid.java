package io.th0rgal.oraxen.font;

import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

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
     * Creates a GlyphGrid from a glyph configuration section.
     * Supports the canonical top-level keys as well as the legacy nested
     * {@code grid.rows}/{@code grid.columns} layout.
     *
     * @param section The glyph configuration section
     * @return The configured grid, or SINGLE if no grid keys are present
     */
    @NotNull
    public static GlyphGrid fromGlyphConfig(@Nullable ConfigurationSection section) {
        if (section == null)
            return SINGLE;

        boolean hasTopLevelRows = section.contains("rows");
        boolean hasTopLevelColumns = section.contains("columns") || section.contains("cols");
        if (hasTopLevelRows || hasTopLevelColumns) {
            GlyphGrid legacyGrid = fromConfig(section.getConfigurationSection("grid"));
            int rows = hasTopLevelRows ? section.getInt("rows", legacyGrid.rows()) : legacyGrid.rows();
            int columns = hasTopLevelColumns
                    ? (section.contains("columns") ? section.getInt("columns", legacyGrid.columns()) : section.getInt("cols", legacyGrid.columns()))
                    : legacyGrid.columns();
            return normalize(rows, columns);
        }

        return fromConfig(section.getConfigurationSection("grid"));
    }

    /**
     * Infers a glyph grid from the configured unicode rows.
     *
     * @param unicodeRows The glyph unicode rows
     * @return A grid matching the unicode row count and the widest row
     */
    @NotNull
    public static GlyphGrid fromUnicodeRows(@Nullable List<String> unicodeRows) {
        if (unicodeRows == null || unicodeRows.isEmpty())
            return SINGLE;

        int rows = unicodeRows.size();
        int columns = unicodeRows.stream().mapToInt(row -> row != null ? row.length() : 0).max().orElse(1);
        return normalize(rows, columns);
    }

    /**
     * Returns a grid large enough for both this grid and the provided grid.
     *
     * @param other Another grid
     * @return A merged grid using the max rows/columns of both inputs
     */
    @NotNull
    public GlyphGrid merge(@Nullable GlyphGrid other) {
        if (other == null)
            return this;
        return normalize(Math.max(rows, other.rows), Math.max(columns, other.columns));
    }

    @NotNull
    private static GlyphGrid normalize(int rows, int columns) {
        return new GlyphGrid(Math.max(rows, 1), Math.max(columns, 1));
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
