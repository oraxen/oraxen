package io.th0rgal.oraxen.compatibilities.provided.placeholderapi;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.font.Glyph;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
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
        if (params.equals("pack_url"))
            return plugin.getPackURL();
        else if (params.equals("pack_hash"))
            return plugin.getPackSHA1();

        // Handle positive shift: %oraxen_shift_N%
        if (params.startsWith("shift_")) {
            try {
                int pixels = Integer.parseInt(params.substring(6));
                return plugin.getFontManager().getShift(pixels);
            } catch (NumberFormatException ignored) {
            }
        }

        // Handle negative shift: %oraxen_neg_shift_N%
        if (params.startsWith("neg_shift_")) {
            try {
                int pixels = Integer.parseInt(params.substring(10));
                return plugin.getFontManager().getShift(-pixels);
            } catch (NumberFormatException ignored) {
            }
        }

        final Glyph glyph = plugin.getFontManager().getGlyphFromName(params);
        if (glyph != null)
            return glyph.getCharacter();
        return null; // Placeholder is unknown by the Expansion
    }
}
