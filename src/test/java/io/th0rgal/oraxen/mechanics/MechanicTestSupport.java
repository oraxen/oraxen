package io.th0rgal.oraxen.mechanics;

import io.th0rgal.oraxen.OraxenPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.UnsafeValues;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.util.logging.Logger;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

import java.io.File;
import java.lang.reflect.Field;
import java.util.Map;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

abstract class MechanicTestSupport {

    @BeforeAll
    static void setUpOraxenPlugin() throws Exception {
        OraxenPlugin plugin = mock(OraxenPlugin.class);
        when(plugin.namespace()).thenReturn("oraxen");
        Field pluginFile = org.bukkit.plugin.java.JavaPlugin.class.getDeclaredField("file");
        pluginFile.setAccessible(true);
        pluginFile.set(plugin, new File("missing-oraxen-test.jar"));

        Field instance = OraxenPlugin.class.getDeclaredField("oraxen");
        instance.setAccessible(true);
        instance.set(null, plugin);

        Field serverField = Bukkit.class.getDeclaredField("server");
        serverField.setAccessible(true);
        if (serverField.get(null) == null) {
            Server server = mock(Server.class);
            when(server.getVersion()).thenReturn("1.21.11-R0.1-SNAPSHOT");
            when(server.getBukkitVersion()).thenReturn("1.21.11-R0.1-SNAPSHOT");
            when(server.getLogger()).thenReturn(Logger.getLogger("TestBukkit"));
            when(server.getUnsafe()).thenReturn(mock(UnsafeValues.class));
            serverField.set(null, server);
        }
    }

    @AfterAll
    static void resetOraxenPlugin() throws Exception {
        Field instance = OraxenPlugin.class.getDeclaredField("oraxen");
        instance.setAccessible(true);
        instance.set(null, null);
    }

    static MechanicFactory mechanicFactory() {
        return mock(MechanicFactory.class);
    }

    static ConfigurationSection mechanicSection(String mechanicId) {
        YamlConfiguration configuration = new YamlConfiguration();
        return configuration.createSection("test_item.mechanics." + mechanicId);
    }

    static ConfigurationSection mechanicSection(String mechanicId, Object... keyValues) {
        ConfigurationSection section = mechanicSection(mechanicId);
        put(section, keyValues);
        return section;
    }

    static ConfigurationSection standaloneSection(Object... keyValues) {
        YamlConfiguration configuration = new YamlConfiguration();
        ConfigurationSection section = configuration.createSection("test");
        put(section, keyValues);
        return section;
    }

    @SuppressWarnings("unchecked")
    static void put(ConfigurationSection section, Object... keyValues) {
        if (keyValues.length % 2 != 0) {
            throw new IllegalArgumentException("keyValues must contain key/value pairs");
        }

        for (int i = 0; i < keyValues.length; i += 2) {
            String key = (String) keyValues[i];
            Object value = keyValues[i + 1];
            if (value instanceof Map<?, ?> map) {
                ConfigurationSection child = section.createSection(key);
                map.forEach((childKey, childValue) -> child.set(String.valueOf(childKey), childValue));
            } else {
                section.set(key, value);
            }
        }
    }
}
