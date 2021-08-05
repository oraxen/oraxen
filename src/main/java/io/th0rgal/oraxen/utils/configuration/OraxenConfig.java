package io.th0rgal.oraxen.utils.configuration;


import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableMap;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

@SuppressWarnings("unused")
public class OraxenConfig {

    private static final String HEADER = "This is the main configuration file for Oraxen";

    private static File CONFIG_FILE;
    public static YamlConfiguration config;

    static int versionOraxen;
    static boolean verbose;

    public static void init(File configFile) {
        CONFIG_FILE = configFile;
        config = new YamlConfiguration();
        try {
            config.load(configFile);
        } catch (IOException ignore) {

        } catch (InvalidConfigurationException exception) {
            Bukkit.getLogger().log(Level.SEVERE, "Could not load settings.yml, please correct your syntax errors", exception);
            throw Throwables.propagate(exception);
        }
        config.options().header(HEADER);
        config.options().copyDefaults(true);

        verbose = getBoolean("verbose", false);
        versionOraxen = getInt("config-version", 1);
        set("config-version", 1);

        readConfig(OraxenConfig.class, null);
    }

    protected static void log(String s) {
        if (verbose) {
            log(Level.INFO, s);
        }
    }

    protected static void log(Level level, String s) {
        Bukkit.getLogger().log(level, s);
    }

    static void readConfig(Class<?> clazz, Object instance) {
        for (Method method : clazz.getDeclaredMethods()) {
            if (Modifier.isPrivate(method.getModifiers())) {
                if (method.getParameterTypes().length == 0 && method.getReturnType() == Void.TYPE) {
                    try {
                        method.setAccessible(true);
                        method.invoke(instance);
                    } catch (InvocationTargetException ex) {
                        throw Throwables.propagate(ex.getCause());
                    } catch (Exception ex) {
                        Bukkit.getLogger().log(Level.SEVERE, "Error invoking " + method, ex);
                    }
                }
            }
        }

        try {
            config.save(CONFIG_FILE);
        } catch (IOException ex) {
            Bukkit.getLogger().log(Level.SEVERE, "Could not save " + CONFIG_FILE, ex);
        }
    }

    private static void set(String path, Object val) {
        config.addDefault(path, val);
        config.set(path, val);
    }

    private static String getString(String path, String def) {
        config.addDefault(path, def);
        return config.getString(path, config.getString(path));
    }

    private static boolean getBoolean(String path, boolean def) {
        config.addDefault(path, def);
        return config.getBoolean(path, config.getBoolean(path));
    }

    private static double getDouble(String path, double def) {
        config.addDefault(path, def);
        return config.getDouble(path, config.getDouble(path));
    }

    private static int getInt(String path, int def) {
        config.addDefault(path, def);
        return config.getInt(path, config.getInt(path));
    }

    private static <T> List getList(String path, T def) {
        config.addDefault(path, def);
        return config.getList(path, config.getList(path));
    }

    static Map<String, Object> getMap(String path, Map<String, Object> def) {
        if (def != null && config.getConfigurationSection(path) == null) {
            config.addDefault(path, def);
            return def;
        }
        return toMap(config.getConfigurationSection(path));
    }

    private static Map<String, Object> toMap(ConfigurationSection section) {
        ImmutableMap.Builder<String, Object> builder = ImmutableMap.builder();
        if (section != null) {
            for (String key : section.getKeys(false)) {
                Object obj = section.get(key);
                if (obj != null) {
                    builder.put(key, obj instanceof ConfigurationSection val ? toMap(val) : obj);
                }
            }
        }
        return builder.build();
    }

    // Configurations Lists
    public static String languageOraxen = "english";
    private static void oraxenPlugins() {
        languageOraxen = getString("Plugin.language", languageOraxen);
    }

    public static boolean oraxenRepairDurabilityCommands = false;
    private static void oraxenCommands() {
        oraxenRepairDurabilityCommands = getBoolean("Plugin.commands.repair.oraxen_durability_only", oraxenRepairDurabilityCommands);
    }

    public static boolean generationResourcesPack = true;
    public static String compressionResourcesPack = "BEST_COMPRESSION";
    public static boolean protectionResourcesPack = true;
    public static String commentTexturesPack = """
            The content of this texture pack
            belongs to the owner of the Oraxen
            plugin and any complete or partial
            use must comply with the terms and
            conditions of Oraxen.""";
    private static void packGeneration() {
        generationResourcesPack = getBoolean("Pack.generation.generate", generationResourcesPack);
        compressionResourcesPack = getString("Pack.generation.compression", compressionResourcesPack);
        protectionResourcesPack = getBoolean("Pack.generation.protection", protectionResourcesPack);
        commentTexturesPack = getString("Pack.generation.comment", commentTexturesPack);
    }
}
