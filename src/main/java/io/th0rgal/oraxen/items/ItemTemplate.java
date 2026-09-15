package io.th0rgal.oraxen.items;

import io.th0rgal.oraxen.utils.OraxenYaml;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public class ItemTemplate {

    private static final Map<String, ItemLoader> itemTemplates = new HashMap<>();

    public static void register(@NotNull ConfigurationSection section) {
        section.set("injectId", false);
        OraxenYaml.invalidateKeyCache(section);
        itemTemplates.put(section.getName(), new ItemLoader(section));
    }

    public static Map<String, ItemLoader> getItemTemplates() {
        return itemTemplates;
    }

    @Nullable
    public static ItemLoader getLoaderTemplate(String id) {
        return itemTemplates.get(id);
    }

    public static boolean isTemplate(String id) {
        return itemTemplates.containsKey(id);
    }
}
