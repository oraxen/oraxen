package io.th0rgal.oraxen.pack.modifiers;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.utils.Utils;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class PackFilesInjector extends PackModifier {

    private Map<File, String> contentByFiles;

    public PackFilesInjector(File folder, Map<String, String> contentByFileNames) {
        if (!folder.exists())
            folder.mkdirs();
        contentByFiles = new HashMap<>();
        for (Map.Entry<String, String> contentByFileName : contentByFileNames.entrySet()) {
            contentByFiles.put(new File(folder, contentByFileName.getKey()), contentByFileName.getValue());
        }
    }

    public PackFilesInjector(Map<File, String> contentByFiles) {
        this.contentByFiles = contentByFiles;
    }

    public PackFilesInjector(String folderName, String fileName, String content) {
        File folder = new File(OraxenPlugin.get().getDataFolder(), folderName);
        if (!folder.exists())
            folder.mkdirs();
        contentByFiles = new HashMap<>();
        contentByFiles.put(new File(folder, fileName), content);
    }

    public PackFilesInjector(File file, String content) {
        contentByFiles = new HashMap<>();
        contentByFiles.put(file, content);
    }

    @Override
    public void update(File packDirectory) {
        for (Map.Entry<File, String> contentFile : contentByFiles.entrySet())
            Utils.writeStringToFile(contentFile.getKey(), contentFile.getValue());
    }

}
