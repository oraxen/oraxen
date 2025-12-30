package io.th0rgal.oraxen.config;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.utils.OraxenYaml;
import io.th0rgal.oraxen.utils.VersionUtil;
import io.th0rgal.oraxen.utils.customarmor.CustomArmorType;
import io.th0rgal.oraxen.utils.logs.Logs;
import org.apache.commons.io.FileUtils;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.AbstractMap;
import java.util.Enumeration;
import java.util.Map.Entry;
import java.util.function.Consumer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

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
        return new AbstractMap.SimpleEntry<>(file, OraxenYaml.loadConfiguration(file));
    }

    public File extractConfiguration(String fileName) {
        File file = new File(this.plugin.getDataFolder(), fileName);
        if (!file.exists())
            this.plugin.saveResource(fileName, false);
        return file;
    }

    public void extractConfigsInFolder(String folder, String fileExtension) {
        browseJar(entry -> extractFileAccordingToExtension(entry, folder, fileExtension));
    }

    public void extractFileIfTrue(ZipEntry entry, boolean isSuitable) {
        if (entry.isDirectory() || !isSuitable) return;
        if (entry.getName().startsWith("pack/textures/models/armor/")) {
            CustomArmorType customArmorType = CustomArmorType.getSetting();
            if (OraxenPlugin.get().getDataFolder().toPath().resolve("pack/" + entry.getName()).toFile().exists()) return;
            if (customArmorType != CustomArmorType.SHADER) return;
            if (!Settings.CUSTOM_ARMOR_SHADER_GENERATE_CUSTOM_TEXTURES.toBool() && entry.getName().startsWith("pack/textures/models/armor/leather_layer")) return;
        }
        if (entry.getName().startsWith("items/")) extractVersionSpecificItemConfig(entry);
        else plugin.saveResource(entry.getName(), true);
    }

    private void extractVersionSpecificItemConfig(ZipEntry entry) {
        if (!entry.getName().startsWith("items/")) return;
        if (!entry.getName().endsWith(".yml")) return;
        if (!VersionUtil.atOrAbove("1.20.5")) {
            plugin.saveResource(entry.getName(), true);
            return;
        }

        try(InputStream inputStream = plugin.getResource(entry.getName())) {
            YamlConfiguration itemYaml = OraxenYaml.loadConfiguration(new InputStreamReader(inputStream));
            for (String itemId : itemYaml.getKeys(false)) {
                ConfigurationSection itemSection = itemYaml.getConfigurationSection(itemId);
                if (itemSection == null) continue;

                ConfigurationSection mechanicSection = itemSection.getConfigurationSection("Mechanics");
                if (mechanicSection == null) continue;

                ConfigurationSection componentSection = itemSection.getConfigurationSection("Components");
                if (componentSection == null) componentSection = itemSection.createSection("Components");

                Object durability = mechanicSection.get("durability.value");
                mechanicSection.set("durability", null);
                componentSection.set("durability", durability);

                if (mechanicSection.getKeys(false).isEmpty()) itemSection.set("Mechanics", null);
                if (componentSection.getKeys(false).isEmpty()) itemSection.set("Components", null);
            }
            File itemFile = plugin.getDataFolder().toPath().resolve(entry.getName()).toFile();

            if (VersionUtil.atOrAbove("1.20.5"))
                FileUtils.writeStringToFile(itemFile, itemYaml.saveToString().replace("displayname", "itemname"), StandardCharsets.UTF_8);
            else itemYaml.save(itemFile);
        } catch (Exception e) {
            plugin.saveResource(entry.getName(), true);
        }
    }

    private void extractFileAccordingToExtension(ZipEntry entry, String folder, String fileExtension) {
        boolean isSuitable = entry.getName().startsWith(folder + "/") && entry.getName().endsWith("." + fileExtension);
        extractFileIfTrue(entry, isSuitable);
    }

    /**
     * Browse all entries in the plugin JAR file and apply a consumer to each entry.
     * Uses ZipFile instead of ZipInputStream to avoid issues with malformed ZIP entries
     * that have EXT descriptors with STORED compression method.
     *
     * @param entryConsumer consumer to apply to each ZIP entry
     */
    public static void browseJar(Consumer<ZipEntry> entryConsumer) {
        try {
            File jarFile = new File(OraxenPlugin.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            try (ZipFile zip = new ZipFile(jarFile)) {
                Enumeration<? extends ZipEntry> entries = zip.entries();
                while (entries.hasMoreElements()) {
                    entryConsumer.accept(entries.nextElement());
                }
            }
        } catch (URISyntaxException | IOException e) {
            Logs.logError("Failed to browse plugin JAR file");
            e.printStackTrace();
        }
    }

}
