package io.th0rgal.oraxen.nms.v1_21_R4_spigot;

import io.th0rgal.oraxen.utils.logs.Logs;
import org.bukkit.entity.Player;

public class GlyphHandler implements io.th0rgal.oraxen.nms.GlyphHandler {

    private static boolean warningLogged = false;

    @Override
    public void setupNmsGlyphs() {
        if (!warningLogged) {
            Logs.logWarning("NMS Glyph injection is limited on Spigot servers.");
            Logs.logWarning("For full glyph support, consider using Paper 1.21.5+");
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
