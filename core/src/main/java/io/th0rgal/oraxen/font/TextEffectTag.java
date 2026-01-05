package io.th0rgal.oraxen.font;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.minimessage.Context;
import net.kyori.adventure.text.minimessage.ParsingException;
import net.kyori.adventure.text.minimessage.tag.Modifying;
import net.kyori.adventure.text.minimessage.tag.Tag;
import net.kyori.adventure.text.minimessage.tag.resolver.ArgumentQueue;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * MiniMessage tag resolver for text effects.
 * <p>
 * Usage: {@code <effect:wave>wavy text</effect>}
 * <p>
 * Preserves nested formatting: {@code <effect:wave><bold>bold wavy</bold></effect>}
 * <p>
 * The effect's trigger color is defined in text_effects.yml
 * and matched exactly by the shader at runtime.
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
     * Preserves component structure, decorations, click/hover events while
     * applying the effect font and trigger color to text content.
     */
    private static class EffectModifyingTag implements Modifying {
        private final TextEffect.Definition definition;
        private final Key effectFont;
        private final TextColor triggerColor;

        EffectModifyingTag(TextEffect.Definition definition) {
            this.definition = definition;
            this.effectFont = EffectFontProvider.getFontKey(definition.getId());
            this.triggerColor = definition.getTriggerColor();
        }

        @Override
        public Component apply(@NotNull Component current, int depth) {
            // Only transform at depth 0 to avoid double-processing
            if (depth > 0) {
                return current;
            }

            return transformComponent(current);
        }

        /**
         * Recursively transforms a component tree, applying effect font/color
         * while preserving structure, decorations, and events.
         */
        private Component transformComponent(Component component) {
            // Apply effect font and color, preserving other styles
            // Adventure's style system is additive, so decorations like bold/italic are preserved
            Component transformed = component.font(effectFont).color(triggerColor);

            // Recursively transform children
            List<Component> children = component.children();
            if (!children.isEmpty()) {
                List<Component> transformedChildren = new ArrayList<>(children.size());
                for (Component child : children) {
                    transformedChildren.add(transformComponent(child));
                }
                // Build new component with transformed children
                return transformed.children(transformedChildren);
            }

            return transformed;
        }
    }
}
