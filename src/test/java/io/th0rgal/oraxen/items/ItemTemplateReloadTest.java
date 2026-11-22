package io.th0rgal.oraxen.items;

import io.th0rgal.oraxen.configs.ConfigsManager;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ItemTemplateReloadTest {

    @TempDir
    Path folder;

    @AfterEach
    void clearTemplates() {
        ItemTemplate.replaceAll(List.of());
    }

    @Test
    void reloadConvertsTemplatesToItemsAndDropsDeletedTemplateReferences() throws Exception {
        Path file = folder.resolve("items.yml");
        Files.writeString(file, """
                converted:
                  template: true
                  material: PAPER
                deleted:
                  template: true
                  material: PAPER
                  pack:
                    model: old_model
                unchanged:
                  template: true
                  material: PAPER
                  pack:
                    model: retained_model
                """);
        ConfigsManager manager = mock(ConfigsManager.class, CALLS_REAL_METHODS);
        Field itemsFolder = ConfigsManager.class.getDeclaredField("itemsFolder");
        itemsFolder.setAccessible(true);
        itemsFolder.set(manager, folder.toFile());

        manager.parseAllItemTemplates();
        Map<String, ItemLoader> firstLoad = ItemTemplate.getItemTemplates();
        assertTrue(ItemTemplate.isTemplate("converted"));
        assertTrue(loader("deleted").usesTemplate());
        assertEquals("retained_model", getMeta(loader("unchanged")).getModelName());

        Files.writeString(file, """
                converted:
                  material: PAPER
                unchanged:
                  template: true
                  material: PAPER
                  pack:
                    model: retained_model
                child:
                  template: deleted
                  material: PAPER
                """);
        manager.parseAllItemTemplates();

        assertFalse(ItemTemplate.isTemplate("converted"));
        assertNull(ItemTemplate.getLoaderTemplate("deleted"));
        assertFalse(loader("deleted").usesTemplate());
        assertNull(getMeta(loader("deleted")).getModelName());
        assertTrue(loader("unchanged").usesTemplate());
        assertEquals("retained_model", getMeta(loader("unchanged")).getModelName());
        assertNotSame(firstLoad, ItemTemplate.getItemTemplates());
        assertTrue(firstLoad.containsKey("deleted"));

        Method parse = ConfigsManager.class.getDeclaredMethod("parseItemLoaders", YamlConfiguration.class);
        parse.setAccessible(true);
        Map<?, ?> items = (Map<?, ?>) parse.invoke(manager, YamlConfiguration.loadConfiguration(file.toFile()));
        assertTrue(items.containsKey("converted"));
        assertTrue(items.containsKey("child"));
        assertFalse(items.containsKey("unchanged"));

        Files.delete(file);
        manager.parseAllItemTemplates();
        assertTrue(ItemTemplate.getItemTemplates().isEmpty());
        assertFalse(loader("unchanged").usesTemplate());
    }

    private static OraxenMeta getMeta(ItemLoader loader) throws Exception {
        Field field = ItemLoader.class.getDeclaredField("oraxenMeta");
        field.setAccessible(true);
        return (OraxenMeta) field.get(loader);
    }

    private ItemLoader loader(String template) {
        YamlConfiguration config = new YamlConfiguration();
        var section = config.createSection("child");
        section.set("material", "PAPER");
        section.set("template", template);
        return new ItemLoader(section);
    }
}
