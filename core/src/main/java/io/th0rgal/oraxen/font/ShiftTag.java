package io.th0rgal.oraxen.font;

import io.th0rgal.oraxen.OraxenPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.Tag;
import net.kyori.adventure.text.minimessage.tag.resolver.ArgumentQueue;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;

import java.util.Set;

/**
 * MiniMessage tag resolver for shift tags.
 * Supports both {@code <shift:N>} and {@code <s:N>} syntax.
 * <p>
 * Uses the dedicated shift font (oraxen:shift) with space provider
 * for efficient pixel-based text shifting.
 */
public class ShiftTag {
    private static final String SHIFT = "shift";
    private static final String SHIFT_SHORT = "s";

    public static final TagResolver RESOLVER = TagResolver.resolver(Set.of(SHIFT, SHIFT_SHORT),
            (args, ctx) -> shiftTag(args));

    private static Tag shiftTag(final ArgumentQueue args) {
        int length = 0;
        try {
            length = Integer.parseInt(args.popOr("A shift value is required").value());
        } catch (final NumberFormatException ignored) {
        }

        FontManager fontManager = OraxenPlugin.get().getFontManager();
        String shiftChars = fontManager.getShift(length);

        // Apply the dedicated shift font to the shift characters
        Component shiftComponent = Component.text(shiftChars)
                .font(ShiftProvider.FONT_KEY);

        return Tag.selfClosingInserting(shiftComponent);
    }
}
