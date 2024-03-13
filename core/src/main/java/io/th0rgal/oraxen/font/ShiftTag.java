package io.th0rgal.oraxen.font;

import io.th0rgal.oraxen.OraxenPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.Tag;
import net.kyori.adventure.text.minimessage.tag.resolver.ArgumentQueue;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;

import java.util.Set;

public class ShiftTag {
    private static final String SHIFT = "shift";
    private static final String SHIFT_SHORT = "s";

    public static final TagResolver RESOLVER = TagResolver.resolver(Set.of(SHIFT, SHIFT_SHORT), (args, ctx) -> shiftTag(args));

    private static Tag shiftTag(final ArgumentQueue args) {
        int length = 0;
        try {
            length = Integer.parseInt(args.popOr("A shift value is required").value());
        } catch (final NumberFormatException ignored) {
        }
        return Tag.selfClosingInserting(Component.text(OraxenPlugin.get().getFontManager().getShift(length)));
    }
}
