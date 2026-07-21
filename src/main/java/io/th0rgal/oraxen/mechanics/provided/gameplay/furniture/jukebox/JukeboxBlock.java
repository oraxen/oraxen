package io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.jukebox;

import io.th0rgal.oraxen.mechanics.MechanicFactory;
import io.th0rgal.oraxen.utils.MusicDiscHelpers;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

public class JukeboxBlock {

    private final MechanicFactory factory;
    private final String permission;
    private final double volume;
    private final double pitch;

    /**
     * @deprecated Use {@link #activeModel} instead. This field references another item ID.
     */
    @Deprecated
    public final String active_stage;

    /**
     * Model key from Pack.models to use when jukebox is playing.
     * Resolves to oraxen:&lt;itemId&gt;/&lt;activeModel&gt;
     */
    private final String activeModel;

    public JukeboxBlock(MechanicFactory factory, ConfigurationSection section) {
        this.factory = factory;
        this.volume = section.getDouble("volume", 1);
        this.pitch = section.getDouble("pitch", 1);
        this.permission = section.getString("permission");
        // Legacy support: active_stage references another item ID
        this.active_stage = section.getString("active_stage");
        // New: active_model references a key from Pack.models
        this.activeModel = section.getString("active_model");
    }

    /**
     * Gets the active model key (from Pack.models).
     * @return The model key, or null if not set
     */
    @Nullable
    public String getActiveModel() {
        return activeModel;
    }

    /**
     * Checks if this jukebox has an active model defined.
     */
    public boolean hasActiveModel() {
        return activeModel != null && !activeModel.isEmpty();
    }

    /**
     * Checks if this jukebox uses the legacy active_stage system.
     */
    @Deprecated
    public boolean hasLegacyActiveStage() {
        return active_stage != null && !active_stage.isEmpty();
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
