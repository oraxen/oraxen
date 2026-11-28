package io.th0rgal.oraxen.pack.generation;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.items.ItemBuilder;
import io.th0rgal.oraxen.items.OraxenMeta;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Tag;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemFactory;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

class PredicatesGeneratorTest {

    @Test
    void itemsWithoutCustomModelDataDoNotOverrideVanillaStates() {
        OraxenPlugin plugin = mock(OraxenPlugin.class);
        when(plugin.namespace()).thenReturn("oraxen");

        JsonObject json;
        try (MockedStatic<OraxenPlugin> plugins = mockStatic(OraxenPlugin.class);
             MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            plugins.when(OraxenPlugin::get).thenReturn(plugin);
            bukkit.when(Bukkit::getItemFactory).thenReturn(mock(ItemFactory.class));
            bukkit.when(() -> Bukkit.getTag(anyString(), any(NamespacedKey.class), any())).thenAnswer(ignored -> mock(Tag.class));
            List<ItemBuilder> items = List.of(fishingRod("automatic", null), fishingRod("numbered", 5));
            json = new PredicatesGenerator(Material.FISHING_ROD, items).toJSON();
        }

        List<String> customOverrides = new ArrayList<>();
        for (JsonElement element : json.getAsJsonArray("overrides")) {
            JsonObject override = element.getAsJsonObject();
            String model = override.get("model").getAsString();
            if (model.startsWith("item/")) continue;
            assertTrue(override.getAsJsonObject("predicate").has("custom_model_data"), override.toString());
            customOverrides.add(model);
        }
        assertEquals(List.of("custom/numbered", "custom/numbered_cast"), customOverrides);
    }

    private static ItemBuilder fishingRod(String id, Integer customModelData) {
        ConfigurationSection pack = new YamlConfiguration().createSection(id + ".Pack");
        pack.set("generate_model", false);
        pack.set("model", "custom/" + id);
        pack.set("cast_model", "custom/" + id + "_cast");
        OraxenMeta meta = new OraxenMeta();
        meta.setPackInfos(pack);
        meta.setCustomModelData(customModelData);

        ItemBuilder item = mock(ItemBuilder.class);
        when(item.getOraxenMeta()).thenReturn(meta);
        return item;
    }
}
