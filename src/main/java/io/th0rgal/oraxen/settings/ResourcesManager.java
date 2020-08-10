package io.th0rgal.oraxen.settings;

import io.th0rgal.oraxen.OraxenPlugin;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.security.CodeSource;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class ResourcesManager {

    final JavaPlugin plugin;

    public ResourcesManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    private YamlConfiguration settings;
    public YamlConfiguration getSettings() {
        if (settings == null)
            settings = getConfiguration("settings.yml");
        return settings;
    }

    private YamlConfiguration mechanics;
    public YamlConfiguration getMechanics() {
        if (mechanics == null)
            mechanics = getConfiguration("mechanics.yml");
        return mechanics;
    }

    public File extractConfiguration(String fileName) {
        File itemsFile = new File(this.plugin.getDataFolder(), fileName);
        if (!itemsFile.exists())
            this.plugin.saveResource(fileName, false);
        return itemsFile;
    }

    public YamlConfiguration getConfiguration(String fileName) {
        return YamlConfiguration.loadConfiguration(extractConfiguration(fileName));
    }

    public void extractConfigsInFolder(String folder, String fileExtension) {
        ZipInputStream zip = browse();
        try {
            extractConfigsInFolder(zip, folder, fileExtension);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    private void extractConfigsInFolder(ZipInputStream zip, String folder, String fileExtension) throws IOException {
        ZipEntry entry = zip.getNextEntry();
        while (entry != null) {
            extractFileAccordingToExtension(entry, folder, fileExtension);
            entry = zip.getNextEntry();
        }
        zip.closeEntry();
    }

    public void extractFileIfTrue(ZipEntry entry, String name, boolean isSuitable) {
        if (entry.isDirectory())
            return;
        if (isSuitable)
            plugin.saveResource(name, true);
    }

    private void extractFileAccordingToExtension(ZipEntry entry, String folder, String fileExtension) {
        String name = entry.getName();
        boolean isSuitable = name.startsWith(folder + "/") && name.endsWith("." + fileExtension);
        extractFileIfTrue(entry, name, isSuitable);
    }

    public static ZipInputStream browse() {
        CodeSource src = OraxenPlugin.class.getProtectionDomain().getCodeSource();
        if (src != null) {
            URL jar = src.getLocation();
            return tryToBrowse(jar);
        } else {
            MessageOld.ZIP_BROWSE_ERROR.logError();
            throw new RuntimeException();
        }
    }

    private static ZipInputStream tryToBrowse(URL jar) {
        try {
            return new ZipInputStream(jar.openStream());
        } catch (IOException e) {
            MessageOld.ZIP_BROWSE_ERROR.logError();
            e.printStackTrace();
            throw new RuntimeException();
        }
    }

}
