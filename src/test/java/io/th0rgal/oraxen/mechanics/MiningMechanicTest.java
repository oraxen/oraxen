package io.th0rgal.oraxen.mechanics;

import io.th0rgal.oraxen.configs.MiningConfigMigration;
import io.th0rgal.oraxen.mechanics.provided.farming.mining.MiningMechanic;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.util.Vector;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MiningMechanicTest extends MechanicTestSupport {

    @Test
    void parsesWorldRelativeOffsetsAndDeduplicates() {
        MiningMechanic mechanic = new MiningMechanic(mechanicFactory(), "hammer",
                List.of("0,1,0", " -1, 0, 2 ", "0,1,0"));

        assertEquals("hammer", mechanic.getItemID());
        assertEquals(List.of(new MiningMechanic.Offset(0, 1, 0), new MiningMechanic.Offset(-1, 0, 2)), mechanic.getOffsets());
        assertThrows(IllegalArgumentException.class,
                () -> new MiningMechanic(mechanicFactory(), "hammer", List.of("1,2")));
    }

    @Test
    void migratesLegacyRadiusAndDepthWithoutOverwritingMining() throws Exception {
        YamlConfiguration configuration = new YamlConfiguration();
        configuration.loadFromString("hammer:\n  mechanics:\n    bigmining:\n      radius: 1\n      depth: 2\n");
        ConfigurationSection item = configuration.getConfigurationSection("hammer");

        assertTrue(MiningConfigMigration.migrateItem(item));
        assertFalse(item.getConfigurationSection("mechanics").contains("bigmining"));
        assertEquals(1, item.getInt("mechanics.mining.radius"));
        assertEquals(2, item.getInt("mechanics.mining.depth"));
        assertFalse(MiningConfigMigration.migrateItem(item));

        configuration.set("hammer.mechanics.bigmining.radius", 9);
        configuration.set("hammer.mechanics.bigmining.depth", 9);
        assertTrue(MiningConfigMigration.migrateItem(item));
        assertEquals(1, item.getInt("mechanics.mining.radius"));
        assertEquals(2, item.getInt("mechanics.mining.depth"));
    }

    @Test
    void faceRelativeDepthFollowsTheLookDirection() throws Exception {
        YamlConfiguration configuration = new YamlConfiguration();
        configuration.loadFromString("hammer:\n  mechanics:\n    mining:\n      radius: 1\n      depth: 2\n");
        MiningMechanic mechanic = new MiningMechanic(mechanicFactory(),
                configuration.getConfigurationSection("hammer.mechanics.mining"));

        assertTrue(mechanic.isFaceRelative());
        assertEquals(BlockFace.NORTH, MiningMechanic.lookingDirection(new Vector(0, 0, -1)));
        assertEquals(BlockFace.EAST, MiningMechanic.lookingDirection(new Vector(1, 0.2, 0)));

        List<Location> north = mechanic.faceTargets(new Location(null, 10, 64, 20), BlockFace.NORTH);
        assertEquals(17, north.size());
        assertTrue(north.contains(new Location(null, 9, 64, 20)));
        assertTrue(north.contains(new Location(null, 10, 65, 19)));
        assertFalse(north.contains(new Location(null, 10, 64, 20)));

        Location east = MiningMechanic.faceRelative(new Location(null, 0, 0, 0), BlockFace.EAST, 1, -1, 2);
        assertEquals(new Location(null, 2, 1, -1), east);
    }

    @Test
    void movesFactorySettingsAndPreservesNewValues() {
        YamlConfiguration configuration = new YamlConfiguration();
        configuration.set("bigmining.enabled", false);
        configuration.set("bigmining.call_events", false);
        configuration.set("mining.call_events", true);

        assertTrue(MiningConfigMigration.migrateFactory(configuration));
        assertFalse(configuration.contains("bigmining"));
        assertFalse(configuration.getBoolean("mining.enabled"));
        assertTrue(configuration.getBoolean("mining.call_events"));
        assertFalse(MiningConfigMigration.migrateFactory(configuration));
    }
}
