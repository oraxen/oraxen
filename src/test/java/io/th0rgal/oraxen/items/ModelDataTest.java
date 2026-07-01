package io.th0rgal.oraxen.items;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ModelDataTest {

    @Test
    void nextModelDataReturnsCurrentValueWhenItIsNotSkipped() {
        assertEquals(1001, ModelData.getNextNotSkippedCustomModelData(1001, Set.of(1000, 1002)));
    }

    @Test
    void nextModelDataSkipsConsecutiveReservedValues() {
        assertEquals(1004, ModelData.getNextNotSkippedCustomModelData(1001, Set.of(1001, 1002, 1003)));
    }

    @Test
    void nextModelDataHandlesReservedValueAboveCurrentCandidate() {
        assertEquals(1003, ModelData.getNextNotSkippedCustomModelData(1003, Set.of(1004)));
    }
}
