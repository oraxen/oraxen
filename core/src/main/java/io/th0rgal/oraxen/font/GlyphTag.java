package io.th0rgal.oraxen.font;

import io.th0rgal.oraxen.OraxenPlugin;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.minimessage.tag.Tag;
import net.kyori.adventure.text.minimessage.tag.resolver.ArgumentQueue;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.entity.Player;

import java.util.Set;

public class GlyphTag {

    static final String GLYPH = "glyph";
    private static final String GLYPH_SHORT = "g";
    public static final TagResolver RESOLVER = TagResolver.resolver(Set.of(GLYPH, GLYPH_SHORT), (args, ctx) -> glyphTag(null,args));


    public static TagResolver getResolverForPlayer(Player player) {
        return TagResolver.resolver(Set.of(GLYPH, GLYPH_SHORT), (args, ctx) -> glyphTag(player, args));
    }

    public static Tag glyphTag(Player player, ArgumentQueue args) {
        String glyphId = args.popOr("A glyph value is required").value();
        Glyph glyph = OraxenPlugin.get().getFontManager().getGlyphFromName(glyphId);
        boolean colorable = args.hasNext() && (args.peek().value().equals("colorable") || args.peek().value().equals("c"));
        Component glyphComponent = Component.text(glyph.getCharacter()).font(Key.key("default")).style(Style.empty());

        glyphComponent = glyph.hasPermission(player) ? glyphComponent.color(colorable ? null : NamedTextColor.WHITE) : Component.text().content(glyph.getGlyphTag()).build();
        return Tag.selfClosingInserting(glyphComponent);
    }
}
