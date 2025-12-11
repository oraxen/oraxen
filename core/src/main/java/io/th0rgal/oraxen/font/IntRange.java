package io.th0rgal.oraxen.font;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Represents an inclusive range of integers, typically used for indexing
 * characters within multi-character glyphs.
 * <p>
 * Supports parsing from various formats:
 * <ul>
 *   <li>"5" - single index (start = end = 5)</li>
 *   <li>"1..5" - range from 1 to 5 inclusive</li>
 *   <li>"1-5" - alternative range syntax</li>
 *   <li>"5..1" - reversed ranges are normalized to 1..5</li>
 * </ul>
 * <p>
 * Indices are 1-based by convention (user-facing).
 */
public record IntRange(int start, int end) {

    private static final Pattern SINGLE_PATTERN = Pattern.compile("^(\\d+)$");
    private static final Pattern RANGE_PATTERN = Pattern.compile("^(\\d+)\\.\\.(\\d+)$");
    private static final Pattern DASH_RANGE_PATTERN = Pattern.compile("^(\\d+)-(\\d+)$");

    /**
     * Creates an IntRange, normalizing reversed ranges.
     *
     * @param start The start index (1-based)
     * @param end   The end index (1-based, inclusive)
     */
    public IntRange {
        // Normalize reversed ranges
        if (start > end) {
            int temp = start;
            start = end;
            end = temp;
        }
    }

    /**
     * Creates a single-element range.
     *
     * @param index The index value
     * @return IntRange with start = end = index
     */
    public static IntRange single(int index) {
        return new IntRange(index, index);
    }

    /**
     * Creates an IntRange covering all elements.
     *
     * @return IntRange representing all elements (1 to MAX_VALUE)
     */
    public static IntRange all() {
        return new IntRange(1, Integer.MAX_VALUE);
    }

    /**
     * Parses an IntRange from a string.
     *
     * @param value The string to parse (e.g., "5", "1..5", "1-5")
     * @return The parsed IntRange, or null if parsing fails
     */
    @Nullable
    public static IntRange parse(@Nullable String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        value = value.trim();

        // Try single index
        Matcher singleMatcher = SINGLE_PATTERN.matcher(value);
        if (singleMatcher.matches()) {
            try {
                int index = Integer.parseInt(singleMatcher.group(1));
                return single(index);
            } catch (NumberFormatException e) {
                return null;
            }
        }

        // Try range with ".."
        Matcher rangeMatcher = RANGE_PATTERN.matcher(value);
        if (rangeMatcher.matches()) {
            return parseRangeMatch(rangeMatcher);
        }

        // Try range with "-"
        Matcher dashMatcher = DASH_RANGE_PATTERN.matcher(value);
        if (dashMatcher.matches()) {
            return parseRangeMatch(dashMatcher);
        }

        return null;
    }

    @Nullable
    private static IntRange parseRangeMatch(Matcher matcher) {
        try {
            int rangeStart = Integer.parseInt(matcher.group(1));
            int rangeEnd = Integer.parseInt(matcher.group(2));
            return new IntRange(rangeStart, rangeEnd);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Checks if this range represents a single element.
     *
     * @return true if start equals end
     */
    public boolean isSingle() {
        return start == end;
    }

    /**
     * Gets the length of this range (number of elements).
     *
     * @return The count of integers in this range
     */
    public int length() {
        return end - start + 1;
    }

    /**
     * Checks if an index is within this range.
     *
     * @param index The index to check (1-based)
     * @return true if start <= index <= end
     */
    public boolean contains(int index) {
        return index >= start && index <= end;
    }

    /**
     * Clamps this range to valid bounds.
     *
     * @param minIndex The minimum valid index (typically 1)
     * @param maxIndex The maximum valid index (typically array length)
     * @return A new IntRange clamped to the valid bounds, or null if no overlap
     */
    @Nullable
    public IntRange clamp(int minIndex, int maxIndex) {
        int clampedStart = Math.max(start, minIndex);
        int clampedEnd = Math.min(end, maxIndex);

        if (clampedStart > clampedEnd) {
            return null; // No overlap with valid range
        }

        return new IntRange(clampedStart, clampedEnd);
    }

    /**
     * Converts this 1-based range to 0-based indices.
     *
     * @return A new IntRange with indices shifted down by 1
     */
    @NotNull
    public IntRange toZeroBased() {
        return new IntRange(start - 1, end - 1);
    }

    @Override
    public String toString() {
        if (isSingle()) {
            return String.valueOf(start);
        }
        return start + ".." + end;
    }
}

