package io.th0rgal.oraxen.compatibilities.provided.mythicmobs;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MythicMobsCompatibilityTest {

    @Test
    void readsItemIdFromFourTokenDropLine() {
        assertEquals("my_item", MythicMobsDropParser.getItemId(new String[]{"oraxen", "my_item", "6-7", "0.75"}));
    }

    @Test
    void readsItemIdFromThreeTokenDropLine() {
        assertEquals("my_item", MythicMobsDropParser.getItemId(new String[]{"oraxen", "my_item", "6-7"}));
    }

    @Test
    void readsItemIdFromThreeTokenDropLineWithStaticAmount() {
        assertEquals("my_item", MythicMobsDropParser.getItemId(new String[]{"oraxen", "my_item", "1"}));
    }

    @Test
    void readsItemIdFromThreeTokenEquipmentLine() {
        assertEquals("my_item", MythicMobsDropParser.getItemId(new String[]{"oraxen", "mainhand", "my_item"}));
    }

    @Test
    void readsItemIdFromTwoTokenDropLine() {
        assertEquals("my_item", MythicMobsDropParser.getItemId(new String[]{"oraxen", "my_item"}));
    }

    @Test
    void returnsEmptyIdForMalformedLine() {
        assertEquals("", MythicMobsDropParser.getItemId(new String[]{"oraxen"}));
    }
}
