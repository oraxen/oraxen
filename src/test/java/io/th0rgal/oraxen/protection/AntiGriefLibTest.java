package io.th0rgal.oraxen.protection;

import net.momirealms.antigrieflib.AntiGriefCompatibility;
import net.momirealms.antigrieflib.Flag;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class AntiGriefLibTest {

    @AfterEach
    void tearDown() {
        AntiGriefLib.setInstance(null);
        AntiGriefLib.setDebug(false);
    }

    @Test
    void allowsActionsWhenAntiGriefLibIsNotInitialized() {
        Player player = mock(Player.class);
        Location location = mock(Location.class);

        assertTrue(AntiGriefLib.canBuild(player, location));
        assertTrue(AntiGriefLib.canBreak(player, location));
        assertTrue(AntiGriefLib.canInteract(player, location));
        assertTrue(AntiGriefLib.canUse(player, location));
    }

    @Test
    void mapsLegacyProtectionChecksToAntiGriefFlags() throws Exception {
        Player player = mock(Player.class);
        Location location = mock(Location.class);
        TestCompatibility compatibility = new TestCompatibility(Flag.PLACE);
        AntiGriefLib.setInstance(antiGriefLib(compatibility));

        assertFalse(AntiGriefLib.canBuild(player, location));
        assertTrue(AntiGriefLib.canBreak(player, location));
        assertTrue(AntiGriefLib.canInteract(player, location));
        assertTrue(AntiGriefLib.canUse(player, location));

        assertEquals(List.of(Flag.PLACE, Flag.BREAK, Flag.INTERACT, Flag.INTERACT), compatibility.testedFlags);
    }

    @Test
    void allowsActionsWhenAntiGriefProviderThrows() throws Exception {
        Player player = mock(Player.class);
        Location location = mock(Location.class);
        TestCompatibility compatibility = new TestCompatibility(null);
        compatibility.throwOnTest = true;
        AntiGriefLib.setInstance(antiGriefLib(compatibility));

        assertTrue(AntiGriefLib.canBreak(player, location));
    }

    private static net.momirealms.antigrieflib.AntiGriefLib antiGriefLib(AntiGriefCompatibility compatibility) throws Exception {
        Constructor<net.momirealms.antigrieflib.AntiGriefLib> constructor = net.momirealms.antigrieflib.AntiGriefLib.class.getDeclaredConstructor(
                org.bukkit.plugin.java.JavaPlugin.class,
                AntiGriefCompatibility[].class,
                boolean.class,
                boolean.class,
                boolean.class,
                String.class
        );
        constructor.setAccessible(true);
        return constructor.newInstance(null, new AntiGriefCompatibility[]{compatibility}, false, true, false, null);
    }

    private static final class TestCompatibility implements AntiGriefCompatibility {
        private final List<Flag<?>> testedFlags = new ArrayList<>();
        private final Flag<?> deniedFlag;
        private boolean throwOnTest;

        private TestCompatibility(Flag<?> deniedFlag) {
            this.deniedFlag = deniedFlag;
        }

        @Override
        public void init() {
        }

        @Override
        public <T> boolean test(Player player, Flag<T> flag, T value) {
            testedFlags.add(flag);
            if (throwOnTest) throw new IllegalStateException("AntiGriefLib provider failure");
            return flag != deniedFlag;
        }

        @Override
        public Plugin plugin() {
            return null;
        }
    }
}
