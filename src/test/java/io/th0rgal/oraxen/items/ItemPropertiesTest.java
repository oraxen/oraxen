package io.th0rgal.oraxen.items;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.utils.PotionUtils;
import io.th0rgal.oraxen.utils.logs.Logs;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.persistence.PersistentDataType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.Map;

import static org.mockito.Mockito.*;

class ItemPropertiesTest {

    private MockedStatic<OraxenPlugin> plugin;
    private MockedStatic<Logs> logs;
    private MockedStatic<PotionUtils> potions;

    @BeforeEach
    void setUp() {
        OraxenPlugin instance = mock(OraxenPlugin.class);
        when(instance.getName()).thenReturn("Oraxen");
        when(instance.namespace()).thenReturn("oraxen");
        plugin = mockStatic(OraxenPlugin.class);
        plugin.when(OraxenPlugin::get).thenReturn(instance);
        logs = mockStatic(Logs.class);
        potions = mockStatic(PotionUtils.class);
    }

    @AfterEach
    void tearDown() {
        potions.close();
        logs.close();
        plugin.close();
    }

    @Test
    void invalidPotionEffectDoesNotSkipLaterSections() {
        ItemBuilder item = apply("""
                item:
                  injectId: false
                  PotionEffects:
                    - type: not_an_effect
                  PersistentData:
                    - key: test:valid
                      type: STRING
                      value: hello
                """);

        verify(item).addCustomTag(eq(new NamespacedKey("test", "valid")), eq(PersistentDataType.STRING), eq("hello"));
        logs.verify(() -> Logs.logWarning(contains("not_an_effect")));
    }

    @Test
    void invalidPersistentDataEntriesAreSkipped() {
        ItemBuilder item = apply("""
                item:
                  injectId: false
                  PersistentData:
                    - key: missing_namespace
                      type: STRING
                      value: a
                    - key: test:type_not_text
                      type: 5
                      value: b
                    - key: test:unknown_type
                      type: NOT_A_TYPE
                      value: c
                    - key: test:wrong_value
                      type: INTEGER
                      value: text
                    - not a map
                    - key: test:valid
                      type: INTEGER
                      value: 7
                """);

        verify(item).addCustomTag(eq(new NamespacedKey("test", "valid")), eq(PersistentDataType.INTEGER), eq(7));
        verify(item, times(1)).addCustomTag(any(), any(), any());
        logs.verify(() -> Logs.logWarning(contains("Invalid PersistentData")), times(5));
    }

    private static ItemBuilder apply(String yaml) {
        YamlConfiguration config = new YamlConfiguration();
        try {
            config.loadFromString(yaml);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
        ConfigurationSection section = config.getConfigurationSection("item");
        ItemBuilder item = mock(ItemBuilder.class);
        new ItemProperties(section, Material.PAPER, new OraxenMeta(), Map.of()).applyMiscAndVanilla(item, section);
        return item;
    }
}
