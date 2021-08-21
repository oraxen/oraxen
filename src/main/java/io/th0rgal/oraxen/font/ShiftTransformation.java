package io.th0rgal.oraxen.font;

import io.th0rgal.oraxen.OraxenPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.parser.node.TagPart;
import net.kyori.adventure.text.minimessage.transformation.Transformation;
import net.kyori.adventure.text.minimessage.transformation.TransformationParser;
import net.kyori.examination.ExaminableProperty;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.stream.Stream;

public class ShiftTransformation extends Transformation {

    /**
     * Get if this transformation can handle the provided tag name.
     *
     * @param name tag name to test
     * @return if this transformation is applicable
     * @since 4.1.0
     */
    public static boolean canParse(final String name) {
        return name.equals("shift");
    }

    private int shift;
    private String prefix;

    private ShiftTransformation() {
    }

    @Override
    public void load(String name, final List<TagPart> args) {
        super.load(name, args);
        shift = Integer.parseInt(args.get(0).value());
        prefix = OraxenPlugin.get().getFontManager().getShift(shift);
    }

    @Override
    public Component apply() {
        return Component.text(prefix);
    }

    @Override
    public @NotNull
    Stream<? extends ExaminableProperty> examinableProperties() {
        return Stream.of(ExaminableProperty.of("shift", prefix));
    }

    @Override
    public boolean equals(final Object other) {
        return this == other || other != null && getClass() == other.getClass() && other.hashCode() == hashCode();
    }

    @Override
    public int hashCode() {
        return shift;
    }

    /**
     * Factory for {@link ShiftTransformation} instances.
     *
     * @since 4.1.0
     */
    public static class Parser implements TransformationParser<ShiftTransformation> {
        @Override
        public ShiftTransformation parse() {
            return new ShiftTransformation();
        }
    }
}