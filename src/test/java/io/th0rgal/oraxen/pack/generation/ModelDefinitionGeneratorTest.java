package io.th0rgal.oraxen.pack.generation;

import com.google.gson.JsonObject;
import io.th0rgal.oraxen.items.OraxenMeta;
import org.bukkit.Material;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ModelDefinitionGeneratorTest {

    @ParameterizedTest
    @EnumSource(value = Material.class, names = {
            "LEATHER_HELMET", "LEATHER_CHESTPLATE", "LEATHER_LEGGINGS", "LEATHER_BOOTS", "LEATHER_HORSE_ARMOR"
    })
    void customLeatherModelsKeepTheirRgbWithOpaqueAlpha(Material material) {
        JsonObject model = model(material, "oraxen:item/custom_leather");

        assertEquals("oraxen:item/custom_leather", model.get("model").getAsString());
        assertDyeTint(model, 0xFFFCF8F8);
    }

    @ParameterizedTest
    @EnumSource(value = Material.class, names = {
            "LEATHER_HELMET", "LEATHER_CHESTPLATE", "LEATHER_LEGGINGS", "LEATHER_BOOTS", "LEATHER_HORSE_ARMOR"
    })
    void vanillaLeatherModelReferencesUseTheOpaqueVanillaColor(Material material) {
        String reference = "minecraft:item/" + material.name().toLowerCase(Locale.ROOT);
        JsonObject model = model(material, reference);

        assertEquals(reference, model.get("model").getAsString());
        assertDyeTint(model, 0xFFA06540);
    }

    @Test
    void potionTintRemainsUnchanged() {
        JsonObject tint = model(Material.POTION, "oraxen:item/custom_potion")
                .getAsJsonArray("tints").get(0).getAsJsonObject();

        assertEquals("minecraft:potion", tint.get("type").getAsString());
        assertEquals(16578808, tint.get("default").getAsInt());
    }

    private static JsonObject model(Material material, String reference) {
        OraxenMeta meta = new OraxenMeta();
        meta.setModelName(reference);
        return new ModelDefinitionGenerator(meta, material).toJSON().getAsJsonObject("model");
    }

    private static void assertDyeTint(JsonObject model, int expectedColor) {
        assertEquals(1, model.getAsJsonArray("tints").size());
        JsonObject tint = model.getAsJsonArray("tints").get(0).getAsJsonObject();
        assertEquals("minecraft:dye", tint.get("type").getAsString());
        int color = tint.get("default").getAsInt();
        assertEquals(expectedColor, color);
        assertEquals(255, color >>> 24);
    }
}
