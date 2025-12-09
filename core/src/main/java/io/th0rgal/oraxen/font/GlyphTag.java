package io.th0rgal.oraxen.font;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.utils.VersionUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.ShadowColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.minimessage.tag.Tag;
import net.kyori.adventure.text.minimessage.tag.resolver.ArgumentQueue;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GlyphTag {

    static final String GLYPH = "glyph";
    private static final String GLYPH_SHORT = "g";

    // Pattern for index/range: "1", "2", "1..4", etc.
    private static final Pattern INDEX_PATTERN = Pattern.compile("^(\\d+)$");
    private static final Pattern RANGE_PATTERN = Pattern.compile("^(\\d+)\\.\\.(\\d+)$");

    public static final TagResolver RESOLVER = TagResolver.resolver(Set.of(GLYPH, GLYPH_SHORT),
            (args, ctx) -> glyphTag(null, args));

    public static TagResolver getResolverForPlayer(Player player) {
        return TagResolver.resolver(Set.of(GLYPH, GLYPH_SHORT), (args, ctx) -> glyphTag(player, args));
    }

    public static Tag glyphTag(Player player, ArgumentQueue args) {
        String glyphId = args.popOr("A glyph value is required").value();
        Glyph glyph = OraxenPlugin.get().getFontManager().getGlyphFromName(glyphId);

        // Collect all arguments
        List<String> arguments = new ArrayList<>();
        while (args.hasNext()) {
            arguments.add(args.pop().value());
        }

        // Parse options from arguments
        ParsedOptions options = parseOptions(arguments, glyph);

        // Get characters to display
        String chars = extractCharacters(glyph, options.startIndex, options.endIndex);

        // Build base component
        Component glyphComponent = Component.text(chars)
                .font(glyph.getFont())
                .style(Style.empty());

        // Apply color (null if colorable, WHITE otherwise)
        glyphComponent = glyphComponent.color(options.colorable ? null : NamedTextColor.WHITE);

        // Apply shadow color if specified (1.21.4+ only)
        glyphComponent = applyShadowColor(glyphComponent, options.shadowColor);

        // Handle permission check
        if (!glyph.hasPermission(player)) {
            glyphComponent = Component.text().content(glyph.getGlyphTag()).build();
        }

        return Tag.selfClosingInserting(glyphComponent);
    }

    /**
     * Parsed options from tag arguments.
     */
    private record ParsedOptions(
            boolean colorable,
            @Nullable Integer shadowColor,
            int startIndex,
            int endIndex) {
    }

    /**
     * Parses arguments into options.
     *
     * @param arguments List of argument strings
     * @param glyph     The glyph being processed
     * @return Parsed options
     */
    private static ParsedOptions parseOptions(List<String> arguments, Glyph glyph) {
        boolean colorable = false;
        Integer shadowColor = null;
        int startIndex = -1;
        int endIndex = -1;

        for (int i = 0; i < arguments.size(); i++) {
            String arg = arguments.get(i).toLowerCase();

            // Check for colorable flag
            if (arg.equals("colorable") || arg.equals("c")) {
                colorable = true;
                continue;
            }

            // Check for shadow color
            if (arg.equals("shadow") || arg.equals("s")) {
                // Only consume next argument if it looks like a color value
                if (i + 1 < arguments.size() && looksLikeColor(arguments.get(i + 1))) {
                    shadowColor = GlyphAppearance.parseArgbColor(arguments.get(++i));
                } else {
                    // Mark that shadow was requested without explicit color (use glyph default)
                    shadowColor = glyph.getAppearance().shadowColor();
                }
                continue;
            }

            // Check for index pattern (e.g., "1", "2")
            Matcher indexMatcher = INDEX_PATTERN.matcher(arg);
            if (indexMatcher.matches()) {
                try {
                    int index = Integer.parseInt(indexMatcher.group(1));
                    startIndex = index;
                    endIndex = index;
                } catch (NumberFormatException ignored) {
                    // Index too large, skip
                }
                continue;
            }

            // Check for range pattern (e.g., "1..4")
            Matcher rangeMatcher = RANGE_PATTERN.matcher(arg);
            if (rangeMatcher.matches()) {
                try {
                    startIndex = Integer.parseInt(rangeMatcher.group(1));
                    endIndex = Integer.parseInt(rangeMatcher.group(2));
                } catch (NumberFormatException ignored) {
                    // Range values too large, skip
                }
            }
        }

        // Use glyph's default shadow color if none specified
        if (shadowColor == null && glyph.getAppearance().hasShadowColor()) {
            shadowColor = glyph.getAppearance().shadowColor();
        }

        return new ParsedOptions(colorable, shadowColor, startIndex, endIndex);
    }

    /**
     * Extracts characters from the glyph based on index/range.
     * Indices are 1-based (user-facing).
     *
     * @param glyph      The glyph
     * @param startIndex Start index (1-based), -1 for all
     * @param endIndex   End index (1-based), -1 for all
     * @return The extracted character string
     */
    private static String extractCharacters(Glyph glyph, int startIndex, int endIndex) {
        char[] allChars = glyph.getAllChars();

        // No range specified, return all characters formatted with newlines
        if (startIndex < 0 || endIndex < 0) {
            return glyph.getFormattedUnicodes();
        }

        // Handle empty glyph
        if (allChars.length == 0) {
            return "";
        }

        // Convert 1-based to 0-based indices
        int start = startIndex - 1;
        int end = endIndex - 1;

        // Normalize reversed ranges (e.g., 4..1 becomes 1..4)
        if (start > end) {
            int temp = start;
            start = end;
            end = temp;
        }

        // Bounds checking
        if (start < 0)
            start = 0;
        if (end >= allChars.length)
            end = allChars.length - 1;

        // Extract range
        StringBuilder sb = new StringBuilder();
        for (int i = start; i <= end; i++) {
            sb.append(allChars[i]);
        }
        return sb.toString();
    }

    /**
     * Applies shadow color to a component.
     * Only works on 1.21.4+ where ShadowColor is available.
     *
     * @param component   The component to modify
     * @param shadowColor The ARGB shadow color, or null
     * @return The modified component
     */
    private static Component applyShadowColor(Component component, @Nullable Integer shadowColor) {
        if (shadowColor == null)
            return component;

        // ShadowColor was added in 1.21.4
        if (!VersionUtil.atOrAbove("1.21.4"))
            return component;

        try {
            return component.shadowColor(ShadowColor.shadowColor(shadowColor));
        } catch (NoClassDefFoundError | NoSuchMethodError e) {
            // Graceful degradation for older Adventure versions
            return component;
        }
    }

    /**
     * Checks if a string looks like a hex color value.
     * Used to determine if an argument should be consumed as a shadow color.
     * <p>
     * To avoid ambiguity with numeric indices (e.g., "123" could be index 123 or
     * color #112233),
     * this method requires the # prefix for color values.
     *
     * @param value The string to check
     * @return true if it starts with # and is a valid hex color
     */
    private static boolean looksLikeColor(String value) {
        if (value == null || value.isBlank())
            return false;

        // Require # prefix to disambiguate from numeric indices
        if (!value.startsWith("#"))
            return false;

        // Validate hex color format after #
        String hex = value.substring(1).toLowerCase();
        if (hex.length() != 3 && hex.length() != 6 && hex.length() != 8)
            return false;

        // Verify all characters are hex digits
        for (char c : hex.toCharArray()) {
            if (!((c >= '0' && c <= '9') || (c >= 'a' && c <= 'f')))
                return false;
        }
        return true;
    }
}
