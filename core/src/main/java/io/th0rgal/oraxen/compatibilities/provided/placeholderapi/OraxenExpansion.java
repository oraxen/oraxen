package io.th0rgal.oraxen.compatibilities.provided.placeholderapi;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.font.Glyph;
import io.th0rgal.oraxen.font.Shift;
import io.th0rgal.oraxen.utils.ParseUtils;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import net.kyori.adventure.key.Key;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

public class OraxenExpansion extends PlaceholderExpansion {

    private final OraxenPlugin plugin;

    public OraxenExpansion(final OraxenPlugin plugin) {
        this.plugin = plugin;
    }

    @NotNull
    @Override
    public String getAuthor() {
        return "Th0rgal";
    }

    @NotNull
    @Override
    public String getIdentifier() {
        return "oraxen";
    }

    @NotNull
    @Override
    public String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onRequest(final OfflinePlayer player, @NotNull final String params) {
        final Glyph glyph = plugin.fontManager().getGlyphFromName(params);

        if (!glyph.isRequired()) return glyph.font() == Key.key("default") ? glyph.character() : glyph.glyphTag();
        if (params.startsWith("shift_")) return Shift.of(ParseUtils.parseInt(StringUtils.substringAfter(params, "shift_"), 0));
        if (params.equals("pack_hash")) return plugin.packGenerator().builtPack().hash();

        return "";
    }
}
