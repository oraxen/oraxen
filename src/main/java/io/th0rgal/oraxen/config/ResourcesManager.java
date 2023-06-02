package io.th0rgal.oraxen.config;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.utils.ReflectionUtils;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.AbstractMap;
import java.util.Map.Entry;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class ResourcesManager {

    final JavaPlugin plugin;

    public ResourcesManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    private Entry<File, YamlConfiguration> settings;
    private Entry<File, YamlConfiguration> mechanics;

    public YamlConfiguration getSettings() {
        return getSettingsEntry().getValue();
    }

    public Entry<File, YamlConfiguration> getSettingsEntry() {
        return settings != null ? settings : (settings = getEntry("settings.yml"));
    }

    public YamlConfiguration getMechanics() {
        return getMechanicsEntry().getValue();
    }

    public Entry<File, YamlConfiguration> getMechanicsEntry() {
        return mechanics != null ? mechanics : (mechanics = getEntry("mechanics.yml"));
    }

    public Entry<File, YamlConfiguration> getEntry(String fileName) {
        File file = extractConfiguration(fileName);
        return new AbstractMap.SimpleEntry<>(file, YamlConfiguration.loadConfiguration(file));
    }

    public File extractConfiguration(String fileName) {
        File file = new File(this.plugin.getDataFolder(), fileName);
        if (!file.exists())
            this.plugin.saveResource(fileName, false);
        return file;
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
        zip.close();
    }

    public void extractFileIfTrue(ZipEntry entry, String name, boolean isSuitable) {
        if (entry.isDirectory())
            return;
        if (isSuitable) {
            if (!Settings.GENERATE_ARMOR_SHADER_FILES.toBool() && name.startsWith("pack/shaders/core/rendertype_armor_cutout_no_cull")) return;
            if (!Settings.GENERATE_CUSTOM_ARMOR_TEXTURES.toBool() && name.startsWith("pack/textures/models/armor/leather_layer")) return;

            plugin.saveResource(name, true);
        }
    }

    private void extractFileAccordingToExtension(ZipEntry entry, String folder, String fileExtension) {
        String name = entry.getName();
        boolean isSuitable = name.startsWith(folder + "/") && name.endsWith("." + fileExtension);
        extractFileIfTrue(entry, name, isSuitable);
    }

    public static ZipInputStream browse() {
        return ReflectionUtils.getJarStream(OraxenPlugin.class).orElseThrow(() -> {
            Message.ZIP_BROWSE_ERROR.log();
            return new RuntimeException("OraxenResources not found!");
        });
    }

}
