package io.th0rgal.oraxen.utils.storage;

import io.th0rgal.oraxen.OraxenPlugin;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;

public class StorageMechanic {

    public static final NamespacedKey STORAGE_KEY = new NamespacedKey(OraxenPlugin.get(), "storage");
    private final int rows;
    private final String title;
    private final boolean isLockable;
    private final String openSound;
    private final String lockedSound;
    private final String closeSound;
    private final float volume;
    private final float pitch;

    public StorageMechanic(ConfigurationSection section) {
        rows = section.getInt("rows", 6);
        title = section.getString("title", "Storage");
        isLockable = section.getBoolean("is_lockable", false);
        openSound = section.getString("open_sound", "minecraft:block.chest.open");
        lockedSound = section.getString("locked_sound", "minecraft:block.chest.locked");
        closeSound = section.getString("close_sound", "minecraft:block.chest.close");
        volume = (float) section.getDouble("volume", 1.0);
        pitch = (float) section.getDouble("pitch", 0.8f);
    }

    public int getRows() {
        return rows;
    }

    public String getTitle() {
        return title;
    }

    public boolean isLockable() {
        return isLockable;
    }

    public boolean hasOpenSound(){
        return openSound != null;
    }

    public String getOpenSound() {
        return openSound;
    }

    public boolean hasLockedSound(){
        return lockedSound != null;
    }

    public String getLockedSound() {
        return lockedSound;
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
