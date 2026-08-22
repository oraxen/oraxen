package io.th0rgal.oraxen.utils.drops;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.LinkedHashMap;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LootTest {

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"any", "false"})
    void silkTouchAnyDropsWithOrWithoutSilkTouch(String value) {
        Loot loot = lootWithSilkTouch(value);

        assertTrue(loot.canDropWithSilkTouch(true));
        assertTrue(loot.canDropWithSilkTouch(false));
    }

    @ParameterizedTest
    @ValueSource(strings = {"required", "true"})
    void silkTouchRequiredOnlyDropsWithSilkTouch(String value) {
        Loot loot = lootWithSilkTouch(value);

        assertTrue(loot.canDropWithSilkTouch(true));
        assertFalse(loot.canDropWithSilkTouch(false));
    }

    @Test
    void silkTouchForbiddenOnlyDropsWithoutSilkTouch() {
        Loot loot = lootWithSilkTouch("forbidden");

        assertFalse(loot.canDropWithSilkTouch(true));
        assertTrue(loot.canDropWithSilkTouch(false));
    }

    @Test
    void silkTouchBooleanTrueRequiresSilkTouch() {
        Loot loot = lootWithSilkTouch(Boolean.TRUE);

        assertTrue(loot.canDropWithSilkTouch(true));
        assertFalse(loot.canDropWithSilkTouch(false));
    }

    @Test
    void silkTouchBooleanFalseDropsWithOrWithoutSilkTouch() {
        Loot loot = lootWithSilkTouch(Boolean.FALSE);

        assertTrue(loot.canDropWithSilkTouch(true));
        assertTrue(loot.canDropWithSilkTouch(false));
    }

    @Test
    void replacingFurnitureItemPreservesSilkTouchMode() {
        Loot replacement = lootWithSilkTouch("forbidden").withItem("test", null);

        assertFalse(replacement.canDropWithSilkTouch(true));
        assertTrue(replacement.canDropWithSilkTouch(false));
    }

    private Loot lootWithSilkTouch(Object value) {
        LinkedHashMap<String, Object> config = new LinkedHashMap<>();
        if (value != null) config.put("silk-touch", value);
        return new Loot(config, "test");
    }
}
