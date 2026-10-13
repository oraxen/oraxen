package io.th0rgal.oraxen.mechanics;

import io.th0rgal.oraxen.mechanics.provided.gameplay.storage.StorageMechanic;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
}
