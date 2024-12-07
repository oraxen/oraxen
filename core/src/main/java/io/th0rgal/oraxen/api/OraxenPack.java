package io.th0rgal.oraxen.api;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.config.Message;
import io.th0rgal.oraxen.font.FontManager;
import io.th0rgal.oraxen.pack.generation.ResourcePack;
import io.th0rgal.oraxen.pack.upload.UploadManager;
import io.th0rgal.oraxen.sound.SoundManager;
import io.th0rgal.oraxen.utils.AdventureUtils;
import io.th0rgal.oraxen.utils.VirtualFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

public class OraxenPack {

    /**
     * Add files to the final resourcepack without adding them to the
     * folder-structure
     * Useful to avoid overwriting files
     * 
     * @param files The files to add to the final resourcepack
     */
    public static void addFilesToPack(File[] files) {
        for (File file : files) {
            try (InputStream inputStream = new FileInputStream(file)) {
                VirtualFile virtualFile = new VirtualFile(file.getParent(), file.getName(), inputStream);
                ResourcePack.addOutputFiles(virtualFile);
            } catch (IOException e) {
                Message.IO_ERROR_ADD_PACK_FILE.log(AdventureUtils.tagResolver("file", file.getName()));
            }
        }
    }

    public static File getPack() {
        return OraxenPlugin.get().getResourcePack().getFile();
    }

    public static void uploadPack() {
        UploadManager uploadManager = new UploadManager(OraxenPlugin.get());
        OraxenPlugin.get().setUploadManager(uploadManager);
        uploadManager.uploadAsyncAndSendToPlayers(OraxenPlugin.get().getResourcePack(), true, true);
    }

    public static void reloadPack() {
        OraxenPlugin.get().setFontManager(new FontManager(OraxenPlugin.get().getConfigsManager()));
        OraxenPlugin.get().setSoundManager(new SoundManager(OraxenPlugin.get().getConfigsManager().getSound()));
        OraxenPlugin.get().getResourcePack().generate();
    }
}
