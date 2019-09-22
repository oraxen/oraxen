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

    JavaPlugin plugin;

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

    public void extractItemsConfigs() {
        ZipInputStream zip = browse();
        try {
            ZipEntry e = zip.getNextEntry();
            while (e != null) {
                String name = e.getName();
                if (!e.isDirectory())
                    if (name.startsWith("items/") && name.endsWith(".yml"))
                        plugin.saveResource(name, true);
                e = zip.getNextEntry();
            }
            zip.closeEntry();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }


    public static ZipInputStream browse() {
        CodeSource src = OraxenPlugin.class.getProtectionDomain().getCodeSource();
        if (src != null) {
            URL jar = src.getLocation();
            try {
                return new ZipInputStream(jar.openStream());
            } catch (IOException e) {
                Message.ZIP_BROWSE_ERROR.logError();
                e.printStackTrace();
            }
        } else {
            Message.ZIP_BROWSE_ERROR.logError();
        }
        throw new RuntimeException();
    }


}
