package io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.jukebox;

import com.jeff_media.morepersistentdatatypes.DataType;
import io.th0rgal.oraxen.api.OraxenItems;
import io.th0rgal.oraxen.mechanics.MechanicFactory;
import io.th0rgal.oraxen.mechanics.provided.misc.music_disc.MusicDiscMechanic;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Locale;

import static io.th0rgal.oraxen.mechanics.provided.misc.music_disc.MusicDiscListener.MUSIC_DISC_KEY;

public class JukeboxBlock {

    private final MechanicFactory factory;
    private final String permission;
    private final double volume;
    private final double pitch;

    public JukeboxBlock(MechanicFactory factory, ConfigurationSection section) {
        this.factory = factory;
        this.volume = section.getDouble("volume", 1);
        this.pitch = section.getDouble("pitch", 1);
        this.permission = section.getString("permission");
    }

    public String getPlayingSong(Entity baseEntity) {
        ItemStack disc = baseEntity.getPersistentDataContainer().get(MUSIC_DISC_KEY, DataType.ITEM_STACK);
        if (disc == null) return null;
        MusicDiscMechanic mechanic = (MusicDiscMechanic) factory.getMechanic(OraxenItems.getIdByItem(disc));
        return (mechanic != null && !mechanic.hasNoSong()) ? mechanic.getSong()
                : disc.getType().isRecord() ? disc.getType().toString().toLowerCase(Locale.ROOT).replace("music_disc_", "minecraft:music_disc.") : null;
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
