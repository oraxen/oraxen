package io.th0rgal.oraxen.api;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.font.FontManager;
import io.th0rgal.oraxen.sound.SoundManager;

public class OraxenPack {

    public static void reloadPack() {
        OraxenPlugin.get().setFontManager(new FontManager(OraxenPlugin.get().getConfigsManager()));
        OraxenPlugin.get().setSoundManager(new SoundManager(OraxenPlugin.get().getConfigsManager().getSound()));
        OraxenPlugin.get().getPackGenerator().generatePack();
    }
}
