package io.th0rgal.oraxen.api;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.font.FontManager;
import io.th0rgal.oraxen.sound.SoundManager;

public class OraxenPack {

    public static void reloadPack() {
        OraxenPlugin oraxen = OraxenPlugin.get();
        oraxen.setFontManager(new FontManager(oraxen.getConfigsManager()));
        oraxen.setSoundManager(new SoundManager(oraxen.getConfigsManager().getSound()));
        oraxen.getResourcePack().generate(oraxen.getFontManager(), oraxen.getSoundManager());
        oraxen.getUploadManager().uploadAsyncAndSendToPlayers(oraxen.getResourcePack(), true, true);
    }
}
