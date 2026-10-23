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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

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
    void selectDefinitionIncludesItemsWithAutomaticallyAssignedCustomModelData() {
        VanillaItemDefinitionGenerator generator = generator(
                Material.PAPER,
                List.of(customItem("configured", 123, "oraxen:item/configured"),
                        customItem("automatic", null, "oraxen:item/automatic")),
                true,
                false);

        // The select dispatcher keys off the Oraxen id, not the number, so an item whose
        // custom model data was assigned automatically still gets a case.
        JsonObject model = generator.toJSON().getAsJsonObject("model");
        assertEquals(2, model.getAsJsonArray("cases").size());
        assertEquals("oraxen:automatic", model.getAsJsonArray("cases")
                .get(0).getAsJsonObject().get("when").getAsString());
        assertEquals("oraxen:configured", model.getAsJsonArray("cases")
                .get(1).getAsJsonObject().get("when").getAsString());
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

    @ParameterizedTest
    @EnumSource(value = Material.class, names = {
            "LEATHER_HELMET", "LEATHER_CHESTPLATE", "LEATHER_LEGGINGS", "LEATHER_BOOTS", "LEATHER_HORSE_ARMOR"
    })
    void leatherDispatchersKeepCustomModelsAndUseOpaqueVanillaFallbacks(Material material) {
        String customModel = "oraxen:item/custom_leather";
        String vanillaModel = "minecraft:item/" + material.name().toLowerCase(Locale.ROOT);

        for (boolean[] mode : new boolean[][]{{false, false}, {true, false}, {true, true}}) {
            JsonObject model = generator(material,
                    List.of(customItem("custom_leather", 123, customModel)), mode[0], mode[1])
                    .toJSON().getAsJsonObject("model");

            if (mode[0]) {
                JsonObject entry = model.getAsJsonArray("cases").get(0).getAsJsonObject();
                assertEquals("oraxen:custom_leather", entry.get("when").getAsString());
                assertLeatherModel(entry.getAsJsonObject("model"), customModel, 0xFFFCF8F8);
                model = model.getAsJsonObject("fallback");
            }
            if (!mode[0] || mode[1]) {
                JsonObject entry = model.getAsJsonArray("entries").get(0).getAsJsonObject();
                assertEquals(123, entry.get("threshold").getAsInt());
                assertLeatherModel(entry.getAsJsonObject("model"), customModel, 0xFFFCF8F8);
                model = model.getAsJsonObject("fallback");
            }
            assertLeatherModel(model, vanillaModel, 0xFFA06540);
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"minecraft:item/leather_helmet", "item/leather_helmet"})
    void customEntriesReferencingVanillaLeatherUseVanillaTint(String reference) {
        JsonObject select = generator(Material.LEATHER_HELMET,
                List.of(customItem("vanilla_leather", 123, reference)), true, true)
                .toJSON().getAsJsonObject("model");

        JsonObject selectEntry = select.getAsJsonArray("cases").get(0).getAsJsonObject();
        assertLeatherModel(selectEntry.getAsJsonObject("model"), reference, 0xFFA06540);
        JsonObject rangeDispatch = select.getAsJsonObject("fallback");
        JsonObject rangeEntry = rangeDispatch.getAsJsonArray("entries").get(0).getAsJsonObject();
        assertLeatherModel(rangeEntry.getAsJsonObject("model"), reference, 0xFFA06540);
    }

    private static void assertLeatherModel(JsonObject model, String expectedModel, int expectedColor) {
        assertEquals("minecraft:model", model.get("type").getAsString());
        assertEquals(expectedModel, model.get("model").getAsString());
        assertEquals(1, model.getAsJsonArray("tints").size());
        JsonObject tint = model.getAsJsonArray("tints").get(0).getAsJsonObject();
        assertEquals("minecraft:dye", tint.get("type").getAsString());
        assertEquals(expectedColor, tint.get("default").getAsInt(), model.toString());
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

    private static ItemBuilder customItem(String itemId, Integer customModelData, String modelName) {
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
