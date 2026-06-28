package io.th0rgal.oraxen.mechanics.provided.misc.music_disc;

import io.th0rgal.oraxen.mechanics.Mechanic;
import io.th0rgal.oraxen.mechanics.MechanicFactory;
import org.bukkit.configuration.ConfigurationSection;

@Deprecated(forRemoval = true, since = "1.21")
public class MusicDiscMechanic extends Mechanic {
    private final String song;

    public MusicDiscMechanic(MechanicFactory mechanicFactory, ConfigurationSection section) {
        super(mechanicFactory, section);
        song = section.getString("song");
    }

    public boolean hasNoSong() { return song == null || song.isBlank(); }
    public String getSong() {
        return song;
    }
}
