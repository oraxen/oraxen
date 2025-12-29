package io.th0rgal.oraxen.nms.v1_20_R4_spigot;

import io.th0rgal.oraxen.utils.logs.Logs;
import org.bukkit.entity.Player;

/**
 * GlyphHandler for Spigot 1.20.6.
 *
 * Note: Packet-based glyph injection is not fully supported on Spigot
 * because it requires Paper-specific network APIs and class names differ.
 * For full glyph support, use Paper 1.20.6+.
 */
public class GlyphHandler implements io.th0rgal.oraxen.nms.GlyphHandler {

    private static boolean warningLogged = false;

    @Override
    public void setupNmsGlyphs() {
        if (!warningLogged) {
            Logs.logWarning("NMS Glyph injection is limited on Spigot servers.");
            Logs.logWarning("For full glyph support, consider using Paper 1.20.6+");
            warningLogged = true;
        }
    }

    @Override
    public void inject(Player player) {
    }

    @Override
    public void uninject(Player player) {
    }
}
