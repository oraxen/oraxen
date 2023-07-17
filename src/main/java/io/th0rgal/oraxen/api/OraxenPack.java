package io.th0rgal.oraxen.api;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.font.FontManager;
import io.th0rgal.oraxen.pack.generation.ResourcePack;
import io.th0rgal.oraxen.sound.SoundManager;
import io.th0rgal.oraxen.utils.VirtualFile;
import io.th0rgal.oraxen.utils.logs.Logs;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

public class OraxenPack {

    /**
     * Add files to the final resourcepack without adding them to the folder-structure
     * Useful to avoid overwriting files
     * @param files The files to add to the final resourcepack
     */
    public static void addFilesToPack(File[] files) {
        for (File file : files) {
            try(InputStream inputStream = new FileInputStream(file)) {
                VirtualFile virtualFile = new VirtualFile(file.getParent(), file.getName(), inputStream);
                ResourcePack.addOutputFiles(virtualFile);
            } catch (IOException e) {
                Logs.logWarning("Failed to add file " + file.getName() + " to the resource pack");
            }
        }
    }

    public static void reloadPack() {
        OraxenPlugin oraxen = OraxenPlugin.get();
        oraxen.setFontManager(new FontManager(oraxen.getConfigsManager()));
        oraxen.setSoundManager(new SoundManager(oraxen.getConfigsManager().getSound()));
        oraxen.getResourcePack().generate(oraxen.getFontManager(), oraxen.getSoundManager());
        oraxen.getUploadManager().uploadAsyncAndSendToPlayers(oraxen.getResourcePack(), true, true);
    }
}
