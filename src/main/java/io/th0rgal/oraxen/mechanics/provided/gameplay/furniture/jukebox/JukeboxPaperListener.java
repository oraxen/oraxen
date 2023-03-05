package io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.jukebox;

import com.jeff_media.morepersistentdatatypes.DataType;
import io.papermc.paper.event.block.BlockBreakBlockEvent;
import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.api.OraxenFurniture;
import io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.FurnitureMechanic;
import io.th0rgal.oraxen.utils.BlockHelpers;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import org.bukkit.Location;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.jetbrains.annotations.NotNull;

import static io.th0rgal.oraxen.mechanics.provided.misc.music_disc.MusicDiscListener.MUSIC_DISC_KEY;

public class JukeboxPaperListener implements Listener {

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onJukeboxBreak(BlockBreakBlockEvent event) {
        ejectAndStopDisc(event.getBlock());
    }

    private void ejectAndStopDisc(Block block) {
        PersistentDataContainer pdc = BlockHelpers.getPDC(block);
        ItemStack item = pdc.get(MUSIC_DISC_KEY, DataType.ITEM_STACK);
        FurnitureMechanic furnitureMechanic = OraxenFurniture.getFurnitureMechanic(block);
        Location loc = BlockHelpers.toCenterLocation(block.getLocation());

        if (furnitureMechanic == null || !furnitureMechanic.isJukebox()) return;
        if (!pdc.has(MUSIC_DISC_KEY, DataType.ITEM_STACK)) return;
        if (item == null || !Tag.ITEMS_MUSIC_DISCS.isTagged(item.getType())) return;

        JukeboxBlock jukebox = furnitureMechanic.getJukebox();
        if (!jukebox.hasPermission(null)) return;

        block.getWorld().getNearbyEntities(loc, 32, 32, 32).stream()
                .filter(entity -> entity instanceof Player)
                .map(entity -> (Player) entity)
                .forEach(p -> OraxenPlugin.get().getAudience().player(p).stopSound(Sound.sound(getSongFromDisc(item), Sound.Source.RECORD, jukebox.getVolume(), jukebox.getPitch())));
        block.getWorld().dropItemNaturally(loc, item);
        pdc.remove(MUSIC_DISC_KEY);
    }

    private @NotNull Key getSongFromDisc(ItemStack disc) {
        return Key.key("minecraft", "music_disc." + disc.getType().toString().toLowerCase().split("music_disc_")[1]);
    }
}
