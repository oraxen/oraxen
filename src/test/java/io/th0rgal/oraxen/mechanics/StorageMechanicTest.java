package io.th0rgal.oraxen.mechanics;

import io.th0rgal.oraxen.mechanics.provided.gameplay.storage.StorageMechanic;
import io.th0rgal.oraxen.utils.logs.Logs;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.MockedStatic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mockStatic;

class StorageMechanicTest extends MechanicTestSupport {

    @Test
    void readsStorageSettings() {
        StorageMechanic mechanic = new StorageMechanic(standaloneSection(
                "rows", 4,
                "title", "Crate",
                "type", "PERSONAL",
                "open_sound", "open",
                "close_sound", "close",
                "volume", 0.8,
                "pitch", 1.2));

        assertEquals(4, mechanic.getRows());
        assertEquals("Crate", mechanic.getTitle());
        assertEquals(StorageMechanic.StorageType.PERSONAL, mechanic.getStorageType());
        assertTrue(mechanic.isPersonal());
        assertEquals("open", mechanic.getOpenSound());
        assertEquals("close", mechanic.getCloseSound());
        assertEquals(0.8f, mechanic.getVolume());
        assertEquals(1.2f, mechanic.getPitch());
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 7})
    void fallsBackToOneRowForInvalidStorageSize(int rows) {
        StorageMechanic mechanic = new StorageMechanic(standaloneSection("rows", rows));

        assertEquals(1, mechanic.getRows());
    }

    @Test
    void readsTypeIgnoringCase() {
        StorageMechanic mechanic = new StorageMechanic(standaloneSection("type", "shulker"));

        assertEquals(StorageMechanic.StorageType.SHULKER, mechanic.getStorageType());
    }

    @Test
    void invalidTypeWarnsAndFallsBackToStorage() {
        try (MockedStatic<Logs> logs = mockStatic(Logs.class)) {
            StorageMechanic mechanic = new StorageMechanic(standaloneSection("type", "chest"));

            assertEquals(StorageMechanic.StorageType.STORAGE, mechanic.getStorageType());
            logs.verify(() -> Logs.logWarning("Valid types are: STORAGE, PERSONAL, ENDERCHEST, DISPOSAL, SHULKER"));
        }
    }
}
