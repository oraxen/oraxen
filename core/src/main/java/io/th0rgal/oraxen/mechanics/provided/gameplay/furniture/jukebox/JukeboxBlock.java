package io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.jukebox;

import com.jeff_media.morepersistentdatatypes.DataType;
import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.utils.VersionUtil;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class JukeboxBlock {

    public static final NamespacedKey MUSIC_DISC_KEY = new NamespacedKey(OraxenPlugin.get(), "music_disc");

    private final String permission;
    private final double volume;
    private final double pitch;

    public JukeboxBlock(ConfigurationSection section) {
        this.volume = section.getDouble("volume", 1);
        this.pitch = section.getDouble("pitch", 1);
        this.permission = section.getString("permission");
    }

    public String getPlayingSong(Entity baseEntity) {
        ItemStack disc = baseEntity.getPersistentDataContainer().get(MUSIC_DISC_KEY, DataType.ITEM_STACK);
        if (disc == null) return null;
        if (VersionUtil.below("1.20.5")) {
            if (disc.getType().isRecord()) return disc.getType().toString().toLowerCase().replace("music_disc_", "minecraft:music_disc.");
            else return null;
        } else {
            if (disc.hasItemMeta() && disc.getItemMeta().hasJukeboxPlayable())
                return disc.getItemMeta().getJukeboxPlayable().getSongKey().asString();
            else return null;
        }
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
