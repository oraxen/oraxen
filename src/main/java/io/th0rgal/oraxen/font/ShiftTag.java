package io.th0rgal.oraxen.font;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.utils.AdventureUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.Context;
import net.kyori.adventure.text.minimessage.ParsingException;
import net.kyori.adventure.text.minimessage.internal.serializer.Emitable;
import net.kyori.adventure.text.minimessage.internal.serializer.SerializableResolver;
import net.kyori.adventure.text.minimessage.tag.Tag;
import net.kyori.adventure.text.minimessage.tag.resolver.ArgumentQueue;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;

import javax.annotation.Nullable;

public class ShiftTag {
    private static final String SHIFT = "shift";

    public static final TagResolver RESOLVER = SerializableResolver.claimingComponent(SHIFT, ShiftTag::create, ShiftTag::emit);
    public static final TagResolver RESOLVER_SHORT = SerializableResolver.claimingComponent("s", ShiftTag::create, ShiftTag::emit);

    static Tag create(final ArgumentQueue args, final Context ctx) throws ParsingException {
        int length = 0;
        try {
            length = Integer.parseInt(args.popOr("A shift value is required").value());
        } catch (final NumberFormatException ignored) {
        }
        String shift = OraxenPlugin.get().getFontManager().getShift(length);
        return Tag.inserting(AdventureUtils.MINI_MESSAGE.deserialize(shift));
    }

    static @Nullable Emitable emit(final Component component) {
        return null;
    }
}
