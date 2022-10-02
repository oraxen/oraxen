package io.th0rgal.oraxen.mechanics.provided.misc.music_disc;

import com.jeff_media.morepersistentdatatypes.DataType;
import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.items.OraxenItems;
import io.th0rgal.oraxen.utils.BlockHelpers;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.SoundCategory;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;

import javax.annotation.Nullable;

public class MusicDiscListener implements Listener {

    private final MusicDiscMechanicFactory factory;
    private final NamespacedKey MUSIC_DISC_KEY = new NamespacedKey(OraxenPlugin.get(), "music_disc");

    public MusicDiscListener(MusicDiscMechanicFactory factory) {
        this.factory = factory;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInsertDisc(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (event.getHand() != EquipmentSlot.HAND) return;

        Player player = event.getPlayer();
        boolean playing = insertAndPlayDisc(event.getClickedBlock(), player.getInventory().getItemInMainHand(), player);
        if (!playing) return;
        player.swingMainHand();
        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEjectDisc(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (event.getHand() != EquipmentSlot.HAND) return;

        boolean stopped = ejectAndStopDisc(event.getClickedBlock());
        if (!stopped) return;
        event.getPlayer().swingMainHand();
        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onJukeboxBreak(BlockBreakEvent event) {
        ejectAndStopDisc(event.getBlock());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onJukeboxBreak(BlockExplodeEvent event) {
        ejectAndStopDisc(event.getBlock());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onJukeboxBreak(BlockBurnEvent event) {
        ejectAndStopDisc(event.getBlock());
    }

    /**
     * Inserts the custom Oraxen Disc item by storing the ItemStack in the blocks PersistentDataContainer.
     * Plays the sound attached to the disc, if any.
     *
     * @param block The Jukebox block to insert the disc into
     * @param disc The Oraxen Disc item to insert
     * @param player The player who inserted the disc, null if inserted by a non-player, i.e hoppers or other entities
     **/
    private boolean insertAndPlayDisc(Block block, ItemStack disc, @Nullable Player player) {
        String itemID = OraxenItems.getIdByItem(disc);
        PersistentDataContainer pdc = BlockHelpers.getPDC(block);
        MusicDiscMechanic mechanic = (MusicDiscMechanic) factory.getMechanic(itemID);

        if (pdc.has(MUSIC_DISC_KEY, DataType.ITEM_STACK)) return false;
        if (disc.getType() == Material.AIR || factory.isNotImplementedIn(itemID)) return false;
        if (mechanic == null || mechanic.hasNoSong()) return false;

        ItemStack insertedDisc = disc.clone();
        insertedDisc.setAmount(1);
        if (player != null && player.getGameMode() != GameMode.CREATIVE)
            disc.setAmount(disc.getAmount() - insertedDisc.getAmount());

        pdc.set(MUSIC_DISC_KEY, DataType.ITEM_STACK, insertedDisc);
        block.getWorld().playSound(BlockHelpers.toCenterLocation(block.getLocation()), mechanic.getSong(), SoundCategory.RECORDS, 1, 1);
        return true;
    }

    /**
     * Ejects the custom Oraxen Disc item from the Jukebox block.
     * Stops the sound attached to the disc, if any.
     * @param block The Jukebox block to eject the disc from
     */
    private boolean ejectAndStopDisc(Block block) {
        PersistentDataContainer pdc = BlockHelpers.getPDC(block);
        ItemStack ejectedDisc = pdc.get(MUSIC_DISC_KEY, DataType.ITEM_STACK);
        String itemID = OraxenItems.getIdByItem(ejectedDisc);
        MusicDiscMechanic mechanic = (MusicDiscMechanic) factory.getMechanic(itemID);

        if (block.getType() != Material.JUKEBOX) return false;
        if (!pdc.has(MUSIC_DISC_KEY, DataType.ITEM_STACK)) return false;
        if (ejectedDisc == null || factory.isNotImplementedIn(itemID)) return false;
        if (mechanic == null || mechanic.hasNoSong()) return false;

        block.getWorld().getNearbyEntities(block.getLocation(), 16, 16, 16).stream()
                .filter(entity -> entity instanceof Player)
                .map(entity -> (Player) entity)
                .forEach(p -> p.stopSound(mechanic.getSong(), SoundCategory.RECORDS));
        block.getWorld().dropItemNaturally(BlockHelpers.toCenterLocation(block.getLocation()), ejectedDisc);
        pdc.remove(MUSIC_DISC_KEY);
        return true;
    }
}
