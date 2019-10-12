package io.th0rgal.oraxen.pack.modifiers;

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

    public PackFilesInjector(File folder, String fileName, String content) {
        if (!folder.exists())
            folder.mkdirs();
        contentByFiles.put(new File(folder, fileName), content);
    }

    public PackFilesInjector(File file, String content) {
        contentByFiles.put(file, content);
    }

    @Override
    public void update(File packDirectory) {
        for (Map.Entry<File, String> contentFile : contentByFiles.entrySet())
            Utils.writeStringToFile(contentFile.getKey(), contentFile.getValue());
    }

}
