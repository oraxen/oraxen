package io.th0rgal.oraxen.font;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.flattener.ComponentFlattener;
import net.kyori.adventure.text.minimessage.Context;
import net.kyori.adventure.text.minimessage.ParsingException;
import net.kyori.adventure.text.minimessage.tag.Modifying;
import net.kyori.adventure.text.minimessage.tag.Tag;
import net.kyori.adventure.text.minimessage.tag.resolver.ArgumentQueue;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

/**
 * MiniMessage tag resolver for text effects.
 * <p>
 * Usage: {@code <effect:wave>wavy text</effect>}
 * <p>
 * The effect's color, speed, and param are defined in text_effects.yml
 * and baked into the shader at resource pack generation time.
 */
public class TextEffectTag {

    private static final String EFFECT = "effect";

    /**
     * Tag resolver for text effects.
     */
    public static final TagResolver RESOLVER = TagResolver.resolver(Set.of(EFFECT), TextEffectTag::createTag);

    @NotNull
    private static Tag createTag(ArgumentQueue args, Context ctx) throws ParsingException {
        // Get the effect name from the argument
        String effectName = args.popOr("An effect name is required (e.g., <effect:wave>)").value();

        // Look up the effect definition
        TextEffect.Definition definition = TextEffect.getEffect(effectName);
        if (definition == null) {
            throw ctx.newException("Unknown text effect: " + effectName);
        }

        // Check if effect is enabled
        if (!TextEffect.isEffectEnabled(definition)) {
            // Return a no-op tag that passes through content unchanged
            return new PassthroughTag();
        }

        // Return a modifying tag that transforms the inner content
        return new EffectModifyingTag(definition);
    }

    /**
     * Creates a resolver for text effects.
     */
    public static TagResolver resolver() {
        return RESOLVER;
    }

    /**
     * A no-op tag that passes content through unchanged.
     */
    private static class PassthroughTag implements Modifying {
        @Override
        public Component apply(@NotNull Component current, int depth) {
            return current;
        }
    }

    /**
     * Modifying tag that applies text effects to wrapped content.
     * Each component's text content is transformed with the effect font and encoding.
     */
    private static class EffectModifyingTag implements Modifying {
        private final TextEffect.Definition definition;

        EffectModifyingTag(TextEffect.Definition definition) {
            this.definition = definition;
        }

        @Override
        public Component apply(@NotNull Component current, int depth) {
            // Extract plain text from the current component
            StringBuilder textBuilder = new StringBuilder();
            ComponentFlattener.basic().flatten(current, textBuilder::append);
            String text = textBuilder.toString();

            if (text.isEmpty()) {
                return current;
            }

            // Apply the text effect
            return TextEffect.apply(text, definition);
        }
    }
}
