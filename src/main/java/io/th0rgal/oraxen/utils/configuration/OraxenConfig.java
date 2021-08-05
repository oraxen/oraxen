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

    public static boolean pack_Generation_Generate = true;
    public static String pack_Generation_Compression = "BEST_COMPRESSION";
    public static boolean pack_Generation_Protection = true;
    public static String pack_Generation_comment = """
            The content of this texture pack
            belongs to the owner of the Oraxen
            plugin and any complete or partial
            use must comply with the terms and
            conditions of Oraxen.""";
    public static boolean pack_Upload_Enabled = true;
    public static String pack_Upload_Type = "polymath";
    public static String pack_Upload_Polymath_Server = "atlas.oraxen.com";
    public static boolean pack_Dispatch_sendPack = true;
    public static boolean pack_Dispatch_sendPackAdvanced_Enabled = true;
    public static boolean pack_Dispatch_sendPackAdvanced_Mandatory = true;
    public static String pack_Dispatch_sendPackAdvanced_Message = "<#fa4943>Accept the pack to enjoy a full <b><gradient:#9055FF:#13E2DA>Oraxen</b><#fa4943> experience";
    public static boolean pack_Dispatch_JoinMessage_Enabled = false;
    public static int pack_Dispatch_JoinMessage_Delay = -1;
    private static void packConfig() {
        // Pack.generation[]
        pack_Generation_Generate = getBoolean("Pack.generation.generate", pack_Generation_Generate);
        pack_Generation_Compression = getString("Pack.generation.compression", pack_Generation_Compression);
        pack_Generation_Protection = getBoolean("Pack.generation.protection", pack_Generation_Protection);
        pack_Generation_comment = getString("Pack.generation.comment", pack_Generation_comment);
        // Pack.upload[]
        pack_Upload_Enabled = getBoolean("Pack.upload.enabled", pack_Upload_Enabled);
        pack_Upload_Type = getString("Pack.upload.type", pack_Upload_Type);
        pack_Upload_Polymath_Server = getString("Pack.upload.polymath.server", pack_Upload_Polymath_Server);
        // Pack.dispatch[]
        pack_Dispatch_sendPack = getBoolean("Pack.dispatch.send_pack", pack_Dispatch_sendPack);
        pack_Dispatch_sendPackAdvanced_Enabled = getBoolean("Pack.dispatch.send_pack_advanced.enabled", pack_Dispatch_sendPackAdvanced_Enabled);
        pack_Dispatch_sendPackAdvanced_Mandatory = getBoolean("Pack.dispatch.send_pack_advanced.mandatory", pack_Dispatch_sendPackAdvanced_Mandatory);
        pack_Dispatch_sendPackAdvanced_Message = getString("Pack.dispatch.send_pack_advanced.message", pack_Dispatch_sendPackAdvanced_Message);
        pack_Dispatch_JoinMessage_Enabled = getBoolean("Pack.dispatch.join_message.enabled", pack_Dispatch_JoinMessage_Enabled);
        pack_Dispatch_JoinMessage_Delay = getInt("Pack.dispatch.join_message.delay", pack_Dispatch_JoinMessage_Delay);
    }
}
