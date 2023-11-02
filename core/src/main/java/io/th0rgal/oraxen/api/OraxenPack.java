package io.th0rgal.oraxen.api;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.font.FontManager;
import io.th0rgal.oraxen.sound.SoundManager;

public class OraxenPack {

    public static void reloadPack() {
        OraxenPlugin.get().fontManager(new FontManager(OraxenPlugin.get().configsManager()));
        OraxenPlugin.get().soundManager(new SoundManager(OraxenPlugin.get().configsManager().getSound()));
        OraxenPlugin.get().packGenerator().generatePack();
    }
}
