package io.th0rgal.oraxen.mechanics.provided.misc.backpack;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.mechanics.Mechanic;
import io.th0rgal.oraxen.mechanics.MechanicFactory;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;

public class BackpackMechanic extends Mechanic {

    public static final NamespacedKey BACKPACK_KEY = new NamespacedKey(OraxenPlugin.get(), "backpack");
    private final int rows;
    private final String title;
    private final String openSound;
    private final String closeSound;
    private final float volume;
    private final float pitch;

    public BackpackMechanic(MechanicFactory mechanicFactory, ConfigurationSection section) {
        super(mechanicFactory, section);
        rows = section.getInt("rows", 6);
        title = section.getString("title", "Backpack");
        openSound = section.getString("open_sound", "minecraft:entity.shulker.open");
        closeSound = section.getString("close_sound", "minecraft:entity.shulker.close");
        volume = (float) section.getDouble("volume", 1.0);
        pitch = (float) section.getDouble("pitch", 1.0);
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

    public String getOpenSound() {
        return openSound;
    }

    public boolean hasCloseSound(){
        return closeSound != null;
    }

    public String getCloseSound() {
        return closeSound;
    }

    public float getPitch() {
        return pitch;
    }

    public float getVolume() {
        return volume;
    }
}
