package io.th0rgal.oraxen.nms;

import org.bukkit.entity.Player;

public interface GlyphHandler {

    void setupNmsGlyphs();

    void inject(Player player);
    void uninject(Player player);

    class EmptyGlyphHandler implements GlyphHandler {

        @Override
        public void setupNmsGlyphs() {

        }

        @Override
        public void inject(Player player) {

        }

        @Override
        public void uninject(Player player) {

        }
    }
}
