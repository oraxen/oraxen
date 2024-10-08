package io.th0rgal.oraxen.font;

import io.th0rgal.oraxen.utils.ParseUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.Tag;
import net.kyori.adventure.text.minimessage.tag.resolver.ArgumentQueue;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;

import java.util.Set;
import java.util.regex.Pattern;

public class ShiftTag {
    private static final String SHIFT = "shift";
    private static final String SHIFT_SHORT = "s";
    public static final Pattern PATTERN = Pattern.compile("<shift:(-?\\d+)>");

    public static final TagResolver RESOLVER = TagResolver.resolver(Set.of(SHIFT, SHIFT_SHORT), (args, ctx) -> shiftTag(args));

    private static Tag shiftTag(final ArgumentQueue args) {
        int shift = ParseUtils.parseInt(args.popOr("A shift value is required").value(), 0);
        return Tag.selfClosingInserting(Component.text(Shift.of(shift)));
    }
}
