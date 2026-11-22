package io.th0rgal.oraxen.items;

import com.jeff_media.morepersistentdatatypes.DataType;
import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.configs.ConfigsManager;
import io.th0rgal.oraxen.nms.NMSHandler;
import io.th0rgal.oraxen.nms.NMSHandlers;
import io.th0rgal.oraxen.utils.AdventureUtils;
import io.th0rgal.oraxen.utils.MinecraftVersion;
import io.th0rgal.oraxen.utils.VersionUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Isolated;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.MockedStatic;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Isolated
class ItemNameTest {
    private Field pluginInstanceField;
    private Object previousPlugin;
    private MockedStatic<VersionUtil> versions;
    private MockedStatic<NMSHandlers> handlers;
    private ItemMeta meta;
    private PersistentDataContainer pdc;

    @BeforeEach
    void setUp() throws Exception {
        pluginInstanceField = OraxenPlugin.class.getDeclaredField("oraxen");
        pluginInstanceField.setAccessible(true);
        previousPlugin = pluginInstanceField.get(null);
        OraxenPlugin plugin = mock(OraxenPlugin.class, CALLS_REAL_METHODS);
        doReturn("oraxen").when(plugin).namespace();
        ConfigsManager configs = mock(ConfigsManager.class);
        YamlConfiguration language = new YamlConfiguration();
        language.set("general.prefix", "");
        when(configs.getLanguage()).thenReturn(language);
        doReturn(configs).when(plugin).getConfigsManager();
        Field pluginFile = JavaPlugin.class.getDeclaredField("file");
        pluginFile.setAccessible(true);
        pluginFile.set(plugin, new File("missing-oraxen-test.jar"));
        pluginInstanceField.set(null, plugin);

        versions = mockStatic(VersionUtil.class);
        handlers = mockStatic(NMSHandlers.class);
        handlers.when(NMSHandlers::getHandler).thenReturn(mock(NMSHandler.class, CALLS_REAL_METHODS));
        meta = mock(ItemMeta.class);
        pdc = mock(PersistentDataContainer.class);
        when(meta.getPersistentDataContainer()).thenReturn(pdc);
        when(meta.getItemFlags()).thenReturn(Set.of());
    }

    @AfterEach
    void tearDown() throws Exception {
        if (handlers != null) handlers.close();
        if (versions != null) versions.close();
        pluginInstanceField.set(null, previousPlugin);
    }

    @ParameterizedTest
    @ValueSource(strings = {"1.20.4", "1.20.6", "1.21.4", "1.21.5", "26.2"})
    void canonicalNameUsesTheAvailableNameComponent(String version) {
        ItemBuilder builder = parse(version, "<red>Base name", null);
        builder.regen();

        Component name = Component.text("Base name", NamedTextColor.RED);
        if (isAtLeast(version, "1.20.5")) {
            verify(meta).itemName(name);
            verify(meta, never()).displayName(any(Component.class));
            verify(meta, never()).customName(any(Component.class));
            verify(pdc, never()).set(eq(ItemBuilder.ORIGINAL_NAME_KEY), eq(DataType.STRING), anyString());
        } else {
            verify(meta).displayName(name.decoration(TextDecoration.ITALIC, false));
            verify(meta, never()).itemName(any(Component.class));
            verify(pdc).set(ItemBuilder.ORIGINAL_NAME_KEY, DataType.STRING,
                    AdventureUtils.MINI_MESSAGE.serialize(name.decoration(TextDecoration.ITALIC, false)));
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"1.20.4", "1.20.6", "1.21.4", "1.21.5", "26.2"})
    void customNameOverridesTheRenderedBaseName(String version) {
        ItemBuilder builder = parse(version, "<red>Base name", "<blue>Custom name");
        builder.regen();

        Component customName = Component.text("Custom name", NamedTextColor.BLUE)
                .decoration(TextDecoration.ITALIC, false);
        if (isAtLeast(version, "1.21.4")) verify(meta).customName(customName);
        else verify(meta).displayName(customName);

        if (isAtLeast(version, "1.20.5")) {
            verify(meta).itemName(Component.text("Base name", NamedTextColor.RED));
        } else {
            verify(meta, never()).itemName(any(Component.class));
            verify(pdc).set(ItemBuilder.ORIGINAL_NAME_KEY, DataType.STRING,
                    AdventureUtils.MINI_MESSAGE.serialize(customName));
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"\u00a7cLegacy name", "<red>Legacy name"})
    void oldRuntimePreservesLegacyAndMiniMessageBaseNameFormatting(String name) {
        parse("1.20.4", name, null).regen();

        verify(meta).displayName(Component.text("Legacy name", NamedTextColor.RED)
                .decoration(TextDecoration.ITALIC, false));
        verify(pdc).set(ItemBuilder.ORIGINAL_NAME_KEY, DataType.STRING,
                AdventureUtils.MINI_MESSAGE.serialize(Component.text("Legacy name", NamedTextColor.RED)
                        .decoration(TextDecoration.ITALIC, false)));
    }

    @Test
    void configuredOldNameReplacesAnImportedDisplayName() {
        when(meta.hasDisplayName()).thenReturn(true);
        when(meta.displayName()).thenReturn(Component.text("Imported name"));

        parse("1.20.4", "<red>Configured name", null).regen();

        Component configuredName = Component.text("Configured name", NamedTextColor.RED)
                .decoration(TextDecoration.ITALIC, false);
        verify(meta).displayName(configuredName);
        verify(pdc).set(ItemBuilder.ORIGINAL_NAME_KEY, DataType.STRING,
                AdventureUtils.MINI_MESSAGE.serialize(configuredName));
    }

    @ParameterizedTest
    @ValueSource(strings = {"1.20.4", "1.20.6"})
    void canonicalChildNameReplacesLegacyTemplateName(String version) throws Exception {
        ConfigurationSection child = templateChild(null);
        parse(version, child).regen();

        Component name = Component.text("Child name", NamedTextColor.RED);
        if (isAtLeast(version, "1.20.5")) verify(meta).itemName(name);
        else verify(meta).displayName(name.decoration(TextDecoration.ITALIC, false));
    }

    @ParameterizedTest
    @ValueSource(strings = {"1.20.4", "1.20.6", "1.21.4"})
    void inheritedCustomNameOverridesTheChildBaseName(String version) throws Exception {
        ConfigurationSection child = templateChild("<blue>Inherited custom name");
        parse(version, child).regen();

        Component name = Component.text("Inherited custom name", NamedTextColor.BLUE)
                .decoration(TextDecoration.ITALIC, false);
        if (isAtLeast(version, "1.21.4")) verify(meta).customName(name);
        else verify(meta).displayName(name);
        if (isAtLeast(version, "1.20.5"))
            verify(meta).itemName(Component.text("Child name", NamedTextColor.RED));
    }

    private ConfigurationSection templateChild(String customName) throws Exception {
        YamlConfiguration config = new YamlConfiguration();
        ConfigurationSection parent = config.createSection("name_test_parent");
        parent.set("displayname", "<green>Parent name");
        parent.set("customname", customName);
        ItemLoader previousTemplate = ItemTemplate.getItemTemplates().get(parent.getName());
        try {
            ItemTemplate.register(parent);
            ConfigurationSection child = config.createSection("name_test_child");
            child.set("template", parent.getName());
            child.set("name", "<red>Child name");
            ItemLoader loader = new ItemLoader(child);
            Method merge = ItemLoader.class.getDeclaredMethod("mergeWithTemplateSection");
            merge.setAccessible(true);
            return (ConfigurationSection) merge.invoke(loader);
        } finally {
            if (previousTemplate == null) ItemTemplate.getItemTemplates().remove(parent.getName());
            else ItemTemplate.getItemTemplates().put(parent.getName(), previousTemplate);
        }
    }

    private ItemBuilder parse(String version, String name, String customName) {
        YamlConfiguration section = new YamlConfiguration();
        section.set("name", name);
        section.set("custom-name", customName);
        return parse(version, section);
    }

    private ItemBuilder parse(String version, ConfigurationSection section) {
        versions.when(() -> VersionUtil.atOrAbove(anyString())).thenAnswer(invocation ->
                isAtLeast(version, invocation.getArgument(0)));
        ItemStack stack = mock(ItemStack.class);
        when(stack.getType()).thenReturn(Material.PAPER);
        when(stack.getAmount()).thenReturn(1);
        when(stack.getItemMeta()).thenReturn(meta);
        ItemBuilder builder = new ItemBuilder(stack);
        new ItemProperties(section, Material.PAPER, mock(OraxenMeta.class), Map.of()).applyBasic(builder);
        new ItemComponents(section, Material.PAPER).apply(builder, section);
        return builder;
    }

    private static boolean isAtLeast(String version, String minimum) {
        return new MinecraftVersion(version).isAtLeast(new MinecraftVersion(minimum));
    }
}
