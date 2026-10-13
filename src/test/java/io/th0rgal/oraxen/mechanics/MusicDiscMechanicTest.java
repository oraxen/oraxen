package io.th0rgal.oraxen.mechanics;

import io.th0rgal.oraxen.mechanics.provided.misc.music_disc.MusicDiscMechanic;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class MusicDiscMechanicTest extends MechanicTestSupport {

    @Test
    void readsSong() {
        MusicDiscMechanic mechanic = new MusicDiscMechanic(mechanicFactory(), mechanicSection("music_disc", "song", "oraxen:test_song"));

        assertFalse(mechanic.hasNoSong());
        assertEquals("oraxen:test_song", mechanic.getSong());
    }
}
