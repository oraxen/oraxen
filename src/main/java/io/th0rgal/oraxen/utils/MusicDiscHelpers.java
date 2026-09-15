package io.th0rgal.oraxen.utils;

import com.jeff_media.morepersistentdatatypes.DataType;
import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.api.OraxenItems;
import io.th0rgal.oraxen.mechanics.Mechanic;
import io.th0rgal.oraxen.mechanics.MechanicFactory;
import io.th0rgal.oraxen.mechanics.provided.misc.music_disc.MusicDiscMechanic;
import io.th0rgal.oraxen.nms.NMSHandlers;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.sound.SoundStop;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.Jukebox;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;
import java.util.logging.Level;

public class MusicDiscHelpers {
    public static final NamespacedKey MUSIC_DISC_KEY = new NamespacedKey(OraxenPlugin.get(), "music_disc");

    public static boolean hasMusicDisc(PersistentDataContainer pdc) {
        return pdc.has(MUSIC_DISC_KEY, DataType.ITEM_STACK);
    }

    public static ItemStack getMusicDisc(PersistentDataContainer pdc) {
        try {
            return pdc.get(MUSIC_DISC_KEY, DataType.ITEM_STACK);
        } catch (Exception e) {
            // music disc is saved as an older format, that is no longer supported.
            // TODO: maybe try recovering from the old format
            // possible causes item was saved on paper pre 1.21.5 and now its 1.21.5 or later
            // or it was saved on spigot and now loaded on paper.
            OraxenPlugin.get().getLogger().log(Level.SEVERE, "Failed to read Music disc from pdc! " + pdc.toString(), e);
            return null;
        }
    }

    public static void setAndPlayMusicDisc(Entity entity, ItemStack record, float volume, float pitch) {
        var pdc = entity.getPersistentDataContainer();
        if (ItemUtils.isMusicDisc(record) && volume == 1F && pitch == 1F && NMSHandlers.getHandler().supportsJukeboxPlaying()) {
            NMSHandlers.getHandler().playJukeBoxSong(entity.getLocation(), record);
        } else {
            var song = MusicDiscHelpers.getSong(record);
            if (song == null) return;
            Key songKey = Key.key(song);
            Key soundId = Key.key(OraxenPlugin.get().getSoundManager().songKeyToSoundId(songKey));
            Float range = OraxenPlugin.get().getSoundManager().jukeboxRange(songKey);

            Sound sound = Sound.sound(soundId, Sound.Source.RECORD, volume, pitch);
            if (range != null) {
                double radius = Math.max(1.0D, range);
                // The range is config-driven and may exceed the chunks owned by this
                // region; Folia thread-checks the whole box searched by
                // getNearbyEntities. Iterating the online-player snapshot with a
                // distance check avoids the scan and keeps any configured range.
                Location soundLocation = entity.getLocation();
                forEachPlayerInRange(entity, radius,
                        player -> AdventureUtils.playSound(player, soundLocation, sound));
            } else {
                AdventureUtils.playSound(entity.getLocation(), sound);
            }
        }
        pdc.set(MUSIC_DISC_KEY, DataType.ITEM_STACK, record);
    }

    public static ItemStack stopJukeboxAt(Entity entity, float volume, float pitch) {
        var pdc = entity.getPersistentDataContainer();
        ItemStack record = getMusicDisc(pdc);
        if(record == null) return null;
        pdc.remove(MUSIC_DISC_KEY);
        if(ItemUtils.isMusicDisc(record) && volume == 1F && pitch == 1F && NMSHandlers.getHandler().supportsJukeboxPlaying()) {
            NMSHandlers.getHandler().stopJukeBox(entity.getLocation());
        } else {
            var song = MusicDiscHelpers.getSong(record);
            if (song == null) return record;
            Key songKey = Key.key(song);
            Key soundId = Key.key(OraxenPlugin.get().getSoundManager().songKeyToSoundId(songKey));

            forEachPlayerInRange(entity, 64,
                    player -> player.stopSound(SoundStop.namedOnSource(soundId, Sound.Source.RECORD)));
        }
        return record;
    }

    /**
     * Runs the action for every online player within {@code radius} of the
     * entity. Uses the thread-safe online-player snapshot plus benign location
     * reads instead of {@code getNearbyEntities}, whose searched box is
     * thread-checked against the owning region on Folia. Each matched player
     * may be owned by a different region than the entity, so the action itself
     * is re-dispatched onto the player's entity scheduler; the action must not
     * touch the entity's world state.
     */
    private static void forEachPlayerInRange(Entity entity, double radius, java.util.function.Consumer<Player> action) {
        double radiusSquared = radius * radius;
        Location center = entity.getLocation();
        for (Player player : Bukkit.getOnlinePlayers()) {
            SchedulerUtil.runForEntity(player, () -> {
                if (!player.getWorld().equals(center.getWorld())) return;
                if (player.getLocation().distanceSquared(center) > radiusSquared) return;
                action.accept(player);
            });
        }
    }

    public static boolean isVanillaJukeboxWithVanillaDisc(Block block) {
        if (!block.getType().equals(Material.JUKEBOX)) return false;
        var blockState = BlockHelpers.getState(block);
        if (blockState instanceof Jukebox jukebox) {
            return ItemUtils.isMusicDisc(jukebox.getRecord());
        }
        return false;
    }

    @Nullable
    public static String getSong(ItemStack record, MechanicFactory factory) {
        if (ItemUtils.isInvalidItem(record)) return null;

        var song = getSong(record);
        if (song != null) return song;

        String itemID = OraxenItems.getIdByItem(record);
        Mechanic mechanic = factory.getMechanic(itemID);
        if (mechanic instanceof MusicDiscMechanic musicDiscMechanic && !musicDiscMechanic.hasNoSong()) {
            return musicDiscMechanic.getSong();
        }
        return null;
    }

    /**
     * Gets the vanilla song from the ItemStack
     *
     * @param record the record who could hold a song
     * @return the song from the playable component or record
     */
    @Nullable
    public static String getSong(ItemStack record) {
        if (ItemUtils.isInvalidItem(record)) return null;
        // native disks don't seem to have jukebox playable set to true
        if (VersionUtil.atOrAbove("1.21") && record.hasItemMeta() && record.getItemMeta().hasJukeboxPlayable()) {
            return record.getItemMeta().getJukeboxPlayable().getSongKey().toString();
        } else if (record.getType().isRecord()) {
            return record.getType().toString().toLowerCase(Locale.ROOT)
                .replaceFirst("music_disc_", "minecraft:music_disc.");
        }
        return null;
    }
}
