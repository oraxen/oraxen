package io.th0rgal.oraxen.mechanics.provided.misc.music_disc;

import com.jeff_media.morepersistentdatatypes.DataType;
import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.api.OraxenFurniture;
import io.th0rgal.oraxen.api.OraxenItems;
import io.th0rgal.oraxen.config.Message;
import io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.FurnitureMechanic;
import io.th0rgal.oraxen.utils.AdventureUtils;
import io.th0rgal.oraxen.utils.BlockHelpers;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import org.bukkit.*;
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

@Deprecated(forRemoval = true, since = "1.21")
public class MusicDiscListener implements Listener {

    private final MusicDiscMechanicFactory factory;
    public static final NamespacedKey MUSIC_DISC_KEY = new NamespacedKey(OraxenPlugin.get(), "music_disc");

    public MusicDiscListener(MusicDiscMechanicFactory factory) {
        this.factory = factory;
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onInsertDisc(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (event.getHand() != EquipmentSlot.HAND) return;

        Player player = event.getPlayer();
        ItemStack itemStack = player.getInventory().getItemInMainHand();
        boolean playing = insertAndPlayCustomDisc(event.getClickedBlock(), itemStack, player);
        if (!playing) return;
        player.swingMainHand();
        Component message = AdventureUtils.MINI_MESSAGE.deserialize(Message.MECHANICS_JUKEBOX_NOW_PLAYING.toString(),
                AdventureUtils.tagResolver("disc", AdventureUtils.parseLegacy(itemStack.getItemMeta().getDisplayName())));
        OraxenPlugin.get().getAudience().player(player).sendActionBar(message);
        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onEjectDisc(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (event.getHand() != EquipmentSlot.HAND) return;

        boolean stopped = ejectAndStopCustomDisc(event.getClickedBlock());
        if (!stopped) return;
        event.getPlayer().swingMainHand();
        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onJukeboxBreak(BlockBreakEvent event) {
        ejectAndStopCustomDisc(event.getBlock());
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onJukeboxBreak(BlockExplodeEvent event) {
        ejectAndStopCustomDisc(event.getBlock());
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onJukeboxBreak(BlockBurnEvent event) {
        ejectAndStopCustomDisc(event.getBlock());
    }

    /**
     * Inserts the custom Oraxen Disc item by storing the ItemStack in the blocks PersistentDataContainer.
     * Plays the sound attached to the disc, if any.
     *
     * @param block The Jukebox block to insert the disc into
     * @param disc The Oraxen Disc item to insert
     * @param player The player who inserted the disc, null if inserted by a non-player, i.e hoppers or other entities
     **/
    private boolean insertAndPlayCustomDisc(Block block, ItemStack disc, @Nullable Player player) {
        String itemID = OraxenItems.getIdByItem(disc);
        PersistentDataContainer pdc = BlockHelpers.getPDC(block);
        MusicDiscMechanic mechanic = (MusicDiscMechanic) factory.getMechanic(itemID);
        FurnitureMechanic furnitureMechanic = OraxenFurniture.getFurnitureMechanic(block);

        if (block.getType() != Material.JUKEBOX && (furnitureMechanic == null || !furnitureMechanic.isJukebox())) return false;
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
    private boolean ejectAndStopCustomDisc(Block block) {
        PersistentDataContainer pdc = BlockHelpers.getPDC(block);
        ItemStack ejectedDisc = pdc.get(MUSIC_DISC_KEY, DataType.ITEM_STACK);
        String itemID = OraxenItems.getIdByItem(ejectedDisc);
        MusicDiscMechanic mechanic = (MusicDiscMechanic) factory.getMechanic(itemID);
        FurnitureMechanic furnitureMechanic = OraxenFurniture.getFurnitureMechanic(block);
        Location loc = BlockHelpers.toCenterLocation(block.getLocation());

        if (block.getType() != Material.JUKEBOX && (furnitureMechanic == null || !furnitureMechanic.isJukebox())) return false;
        if (!pdc.has(MUSIC_DISC_KEY, DataType.ITEM_STACK)) return false;
        if (ejectedDisc == null || factory.isNotImplementedIn(itemID)) return false;
        if (mechanic == null || mechanic.hasNoSong()) return false;

        block.getWorld().getNearbyEntities(loc, 32, 32, 32).stream()
                .filter(entity -> entity instanceof Player)
                .map(entity -> (Player) entity)
                .forEach(p -> OraxenPlugin.get().getAudience().player(p).stopSound(Sound.sound(Key.key(mechanic.getSong()), Sound.Source.RECORD, 1, 1)));
        block.getWorld().dropItemNaturally(loc, ejectedDisc);
        pdc.remove(MUSIC_DISC_KEY);
        return true;
    }
}
