package io.th0rgal.oraxen.mechanics.provided.gameplay.furniture;

import org.bukkit.configuration.serialization.ConfigurationSerialization;
import org.bukkit.persistence.PersistentDataAdapterContext;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

class BlockLocationTest {

    @Test
    void persistentDataTypeRoundTripsBlockLocation() {
        BlockLocation location = new BlockLocation(3, -2, 7);
        PersistentDataAdapterContext context = mock(PersistentDataAdapterContext.class);

        byte[] primitive = BlockLocation.dataType.toPrimitive(location, context);
        ConfigurationSerialization.unregisterClass(BlockLocation.class);

        try {
            BlockLocation deserialized = BlockLocation.dataType.fromPrimitive(primitive, context);
            assertEquals(location, deserialized);
        } finally {
            ConfigurationSerialization.registerClass(BlockLocation.class);
        }
    }
}
