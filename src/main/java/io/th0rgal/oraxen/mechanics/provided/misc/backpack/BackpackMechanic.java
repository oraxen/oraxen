package io.th0rgal.oraxen.mechanics.provided.misc.backpack;

import io.th0rgal.oraxen.mechanics.Mechanic;
import io.th0rgal.oraxen.mechanics.MechanicFactory;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;


public class BackpackMechanic extends Mechanic {

    private final int rows;
    private final String title;
    private final Sound openSound;
    private final float volume;
    private final float pitch;

    public BackpackMechanic(MechanicFactory mechanicFactory, ConfigurationSection section) {
        super(mechanicFactory, section);
        this.rows = section.getInt("rows");
        this.title = section.getString("title");
        if(section.isConfigurationSection("open_sound")) {
            ConfigurationSection sound = section.getConfigurationSection("open_sound");
            this.openSound = Sound.valueOf(sound.getString("sound"));
            this.volume = sound.isDouble("volume") ? (float) sound.getDouble("volume") : 1.0F;
            this.pitch =  sound.isDouble("pitch") ? (float) sound.getDouble("pitch") : 1.0F;
        } else {
            this.openSound = null;
            this.volume = 1.0F;
            this.pitch = 1.0F;
        }
    }

    public int getRows() {
        return rows;
    }

    public String getTitle() {
        return title;
    }

    public boolean hasOpenSound(){
        return openSound != null;
    }

    public Sound getOpenSound() {
        return openSound;
    }

    public float getPitch() {
        return pitch;
    }

    public float getVolume() {
        return volume;
    }
}
