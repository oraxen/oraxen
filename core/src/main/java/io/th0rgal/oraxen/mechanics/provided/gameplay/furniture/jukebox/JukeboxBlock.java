package io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.jukebox;

import io.th0rgal.oraxen.mechanics.MechanicFactory;
import io.th0rgal.oraxen.utils.MusicDiscHelpers;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class JukeboxBlock {

    private final MechanicFactory factory;
    private final String permission;
    private final double volume;
    private final double pitch;
    public final String active_stage;

    public JukeboxBlock(MechanicFactory factory, ConfigurationSection section) {
        this.factory = factory;
        this.volume = section.getDouble("volume", 1);
        this.pitch = section.getDouble("pitch", 1);
        this.permission = section.getString("permission");
        this.active_stage = section.getString("active_stage");
    }

    public String getPlayingSong(Entity baseEntity) {
        ItemStack disc = MusicDiscHelpers.getMusicDisc(baseEntity.getPersistentDataContainer());
        return MusicDiscHelpers.getSong(disc, factory);
    }

    public String getPermission() {
        return permission != null ? permission : "";
    }

    public float getVolume() {
        return (float) volume;
    }

    public float getPitch() {
        return (float) pitch;
    }

    public boolean hasPermission(Player player) {
        return player == null || getPermission().isBlank() || player.hasPermission(getPermission());
    }
}
