package io.th0rgal.oraxen.pack.generation;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.items.ItemBuilder;
import io.th0rgal.oraxen.items.OraxenMeta;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataType;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class VanillaItemDefinitionGeneratorTest {

    private static OraxenPlugin previousOraxenPlugin;

    @BeforeAll
    static void setUpOraxenNamespace() throws Exception {
        OraxenPlugin plugin = mock(OraxenPlugin.class);
        when(plugin.namespace()).thenReturn("oraxen");
        Field field = OraxenPlugin.class.getDeclaredField("oraxen");
        field.setAccessible(true);
        previousOraxenPlugin = (OraxenPlugin) field.get(null);
        field.set(null, plugin);
    }

    @AfterAll
    static void restoreOraxenPlugin() throws Exception {
        Field field = OraxenPlugin.class.getDeclaredField("oraxen");
        field.setAccessible(true);
        field.set(null, previousOraxenPlugin);
    }

    @Test
    void playerHeadSelectDefinitionKeepsCustomCasesAndUsesSpecialHeadFallback() {
        VanillaItemDefinitionGenerator generator = generator(
                Material.PLAYER_HEAD,
                List.of(customItem("custom_head", 123, "oraxen:item/custom_head")),
                true,
                false);

        JsonObject json = generator.toJSON();
        System.out.println("assets/minecraft/items/player_head.json\n"
                + new GsonBuilder().setPrettyPrinting().create().toJson(json));

        JsonObject model = json.getAsJsonObject("model");
        assertEquals("minecraft:select", model.get("type").getAsString());
        assertEquals("oraxen:custom_head", model.getAsJsonArray("cases")
                .get(0).getAsJsonObject().get("when").getAsString());
        assertEquals("minecraft:model", model.getAsJsonArray("cases")
                .get(0).getAsJsonObject().getAsJsonObject("model").get("type").getAsString());
        assertEquals("oraxen:item/custom_head", model.getAsJsonArray("cases")
                .get(0).getAsJsonObject().getAsJsonObject("model").get("model").getAsString());

        assertPlayerHeadSpecialFallback(model.getAsJsonObject("fallback"));
    }

    @Test
    void playerHeadRangeDispatchDefinitionKeepsCustomEntriesAndUsesSpecialHeadFallback() {
        VanillaItemDefinitionGenerator generator = generator(
                Material.PLAYER_HEAD,
                List.of(customItem("custom_head", 123, "oraxen:item/custom_head")),
                false,
                false);

        JsonObject model = generator.toJSON().getAsJsonObject("model");
        assertEquals("minecraft:range_dispatch", model.get("type").getAsString());
        assertEquals(123, model.getAsJsonArray("entries").get(0).getAsJsonObject().get("threshold").getAsInt());
        assertPlayerHeadSpecialFallback(model.getAsJsonObject("fallback"));
    }

    @Test
    void playerHeadCombinedDefinitionKeepsSelectAndRangeDispatchFallbacks() {
        VanillaItemDefinitionGenerator generator = generator(
                Material.PLAYER_HEAD,
                List.of(customItem("custom_head", 123, "oraxen:item/custom_head")),
                true,
                true);

        JsonObject select = generator.toJSON().getAsJsonObject("model");
        assertEquals("minecraft:select", select.get("type").getAsString());
        assertEquals("oraxen:custom_head", select.getAsJsonArray("cases")
                .get(0).getAsJsonObject().get("when").getAsString());

        JsonObject rangeDispatch = select.getAsJsonObject("fallback");
        assertEquals("minecraft:range_dispatch", rangeDispatch.get("type").getAsString());
        assertEquals(123, rangeDispatch.getAsJsonArray("entries").get(0).getAsJsonObject().get("threshold").getAsInt());
        assertPlayerHeadSpecialFallback(rangeDispatch.getAsJsonObject("fallback"));
    }

    @Test
    void otherSkullsUseHeadSpecialFallbacksWithKind() {
        assertHeadFallback(Material.SKELETON_SKULL, "skeleton");
        assertHeadFallback(Material.WITHER_SKELETON_SKULL, "wither_skeleton");
        assertHeadFallback(Material.ZOMBIE_HEAD, "zombie");
        assertHeadFallback(Material.CREEPER_HEAD, "creeper");
        assertHeadFallback(Material.DRAGON_HEAD, "dragon");
        assertHeadFallback(Material.PIGLIN_HEAD, "piglin");
    }

    private static void assertPlayerHeadSpecialFallback(JsonObject fallback) {
        assertEquals("minecraft:special", fallback.get("type").getAsString());
        assertEquals("minecraft:item/template_skull", fallback.get("base").getAsString());
        assertEquals("minecraft:player_head", fallback.getAsJsonObject("model").get("type").getAsString());
    }

    private static void assertHeadFallback(Material material, String kind) {
        JsonObject model = generator(material, List.of(), true, false).toJSON().getAsJsonObject("model");
        assertEquals("minecraft:special", model.get("type").getAsString());
        assertEquals("minecraft:item/template_skull", model.get("base").getAsString());
        assertEquals("minecraft:head", model.getAsJsonObject("model").get("type").getAsString());
        assertEquals(kind, model.getAsJsonObject("model").get("kind").getAsString());
    }

    private static VanillaItemDefinitionGenerator generator(Material material, List<ItemBuilder> items,
            boolean useSelect, boolean includeBothModes) {
        PredicatesGenerator predicatesGenerator = mock(PredicatesGenerator.class);
        when(predicatesGenerator.getVanillaModelName(any(Material.class)))
                .thenReturn("item/" + material.name().toLowerCase(Locale.ROOT));
        return new VanillaItemDefinitionGenerator(material, new ArrayList<>(items), predicatesGenerator, useSelect,
                includeBothModes);
    }

    private static ItemBuilder customItem(String itemId, int customModelData, String modelName) {
        OraxenMeta meta = new OraxenMeta();
        meta.setModelName(modelName);
        meta.setCustomModelData(customModelData);
        try {
            Field hasPackInfos = OraxenMeta.class.getDeclaredField("hasPackInfos");
            hasPackInfos.setAccessible(true);
            hasPackInfos.setBoolean(meta, true);
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError(exception);
        }

        ItemBuilder item = mock(ItemBuilder.class);
        when(item.getOraxenMeta()).thenReturn(meta);
        when(item.getCustomTag(any(NamespacedKey.class), eq(PersistentDataType.STRING))).thenReturn(itemId);
        return item;
    }

}
