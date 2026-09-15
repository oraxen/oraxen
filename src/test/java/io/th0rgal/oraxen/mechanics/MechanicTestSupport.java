package io.th0rgal.oraxen.mechanics;

import io.papermc.paper.registry.RegistryKey;
import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.utils.VersionUtil;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.Server;
import org.bukkit.UnsafeValues;
import org.bukkit.attribute.Attribute;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.util.logging.Logger;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

import java.io.File;
import java.lang.reflect.Field;
import java.util.Map;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

abstract class MechanicTestSupport {

    /**
     * Attribute keys as the 1.21.2+ registry exposes them. The legacy {@code generic.} prefix is
     * gone, so lookups by the old names have to resolve to null here just like they do on a server.
     */
    private static final Set<String> ATTRIBUTE_KEYS = Set.of(
            "armor", "armor_toughness", "attack_damage", "attack_knockback", "attack_speed",
            "block_break_speed", "block_interaction_range", "burning_time", "entity_interaction_range",
            "explosion_knockback_resistance", "fall_damage_multiplier", "flying_speed", "follow_range",
            "gravity", "jump_strength", "knockback_resistance", "luck", "max_absorption", "max_health",
            "mining_efficiency", "movement_efficiency", "movement_speed", "oxygen_bonus",
            "safe_fall_distance", "scale", "sneaking_speed", "spawn_reinforcements", "step_height",
            "submerged_mining_speed", "sweeping_damage_ratio", "tempt_range");

    @BeforeAll
    static void setUpOraxenPlugin() throws Exception {
        // Initialise VersionUtil while OraxenPlugin.get() is still null. Its static manifest lookup
        // goes through OraxenPlugin#getJarFile, which would call getFile() on the mock below and
        // fail the whole static initialiser.
        VersionUtil.isPaperServer();

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
            when(server.getMinecraftVersion()).thenReturn("1.21.11");
            when(server.getLogger()).thenReturn(Logger.getLogger("TestBukkit"));
            UnsafeValues unsafe = unsafeValues();
            when(server.getUnsafe()).thenReturn(unsafe);
            serverField.set(null, server);
        }
    }

    private static UnsafeValues unsafeValues() {
        UnsafeValues unsafe = mock(UnsafeValues.class);
        when(unsafe.get(any(RegistryKey.class), any(NamespacedKey.class))).thenAnswer(invocation -> {
            RegistryKey<?> registry = invocation.getArgument(0);
            NamespacedKey key = invocation.getArgument(1);
            if (registry != RegistryKey.ATTRIBUTE || !NamespacedKey.MINECRAFT.equals(key.getNamespace()))
                return null;
            if (!ATTRIBUTE_KEYS.contains(key.getKey()))
                return null;

            Attribute attribute = mock(Attribute.class);
            when(attribute.getKey()).thenReturn(key);
            return attribute;
        });
        return unsafe;
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
