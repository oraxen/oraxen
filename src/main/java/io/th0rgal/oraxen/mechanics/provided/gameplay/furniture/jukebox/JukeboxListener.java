package io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.jukebox;

import com.jeff_media.morepersistentdatatypes.DataType;
import io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.FurnitureMechanic;
import io.th0rgal.oraxen.utils.BlockHelpers;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.SoundCategory;
import org.bukkit.Tag;
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

import static io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.FurnitureListener.getFurnitureMechanic;
import static io.th0rgal.oraxen.mechanics.provided.misc.music_disc.MusicDiscListener.MUSIC_DISC_KEY;

public class JukeboxListener implements Listener {

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInsertDisc(PlayerInteractEvent event) {
        Block block = event.getClickedBlock();
        Player player = event.getPlayer();

        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (event.getHand() != EquipmentSlot.HAND) return;

        boolean played = insertAndPlayDisc(block, player.getInventory().getItemInMainHand(), player);
        if (!played) return;
        player.swingMainHand();
        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEjectDisc(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (event.getHand() != EquipmentSlot.HAND) return;

        boolean stopped = ejectAndStopDisc(event.getClickedBlock(), event.getPlayer());
        if (!stopped) return;
        event.getPlayer().swingMainHand();
        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onJukeboxBreak(BlockBreakEvent event) {
        ejectAndStopDisc(event.getBlock(), event.getPlayer());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onJukeboxBreak(BlockExplodeEvent event) {
        ejectAndStopDisc(event.getBlock(), null);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onJukeboxBreak(BlockBurnEvent event) {
        ejectAndStopDisc(event.getBlock(), null);
    }

    private boolean insertAndPlayDisc(Block block, ItemStack disc, @Nullable Player player) {
        PersistentDataContainer pdc = BlockHelpers.getPDC(block);
        ItemStack item = pdc.get(MUSIC_DISC_KEY, DataType.ITEM_STACK);
        FurnitureMechanic furnitureMechanic = getFurnitureMechanic(block);
        Location loc = BlockHelpers.toCenterLocation(block.getLocation());

        if (furnitureMechanic == null || !furnitureMechanic.isJukebox()) return false;
        if (!pdc.has(MUSIC_DISC_KEY, DataType.ITEM_STACK)) return false;
        if (item == null || !Tag.ITEMS_MUSIC_DISCS.isTagged(item.getType())) return false;

        JukeboxBlock jukebox = furnitureMechanic.getJukebox();
        if (player != null && !player.hasPermission(jukebox.getPermission())) return false;

        ItemStack insertedDisc = disc.clone();
        insertedDisc.setAmount(1);

        if (player != null && player.getGameMode() != GameMode.CREATIVE)
            disc.setAmount(disc.getAmount() - insertedDisc.getAmount());
        pdc.set(MUSIC_DISC_KEY, DataType.ITEM_STACK, insertedDisc);
        block.getWorld().playSound(loc, jukebox.getPlayingSong(block), SoundCategory.RECORDS, jukebox.getVolume(), jukebox.getPitch());

        /*long discDuration = jukebox.getDiscDuration(block);
        if (jukebox.shouldLoopDisc()) {
            String song = jukebox.getPlayingSong(block);
            OraxenPlugin.get().getSoundManager().getCustomSounds().stream().filter(s -> s.getName().equals(song)).findFirst().ifPresent(s -> {
                String path = s.getName().replace(".", "/").replace("/ogg", ".ogg");
                File soundFile = OraxenPlugin.get().getDataFolder().toPath().resolve("pack").resolve("sounds").resolve(path).toFile();
                soundFile.getTotalSpace()
            });
        }*/
        return true;
    }

    private boolean ejectAndStopDisc(Block block, @Nullable Player player) {
        PersistentDataContainer pdc = BlockHelpers.getPDC(block);
        ItemStack item = pdc.get(MUSIC_DISC_KEY, DataType.ITEM_STACK);
        FurnitureMechanic furnitureMechanic = getFurnitureMechanic(block);
        Location loc = BlockHelpers.toCenterLocation(block.getLocation());

        if (furnitureMechanic == null || !furnitureMechanic.isJukebox()) return false;
        if (!pdc.has(MUSIC_DISC_KEY, DataType.ITEM_STACK)) return false;
        if (item == null || !Tag.ITEMS_MUSIC_DISCS.isTagged(item.getType())) return false;

        JukeboxBlock jukebox = furnitureMechanic.getJukebox();
        if (player != null && !player.hasPermission(jukebox.getPermission())) return false;

        block.getWorld().getNearbyEntities(loc, 32, 32, 32).stream()
                .filter(entity -> entity instanceof Player)
                .map(entity -> (Player) entity)
                .forEach(p -> p.stopSound(getSongFromDisc(item), SoundCategory.RECORDS));
        block.getWorld().dropItemNaturally(loc, item);
        pdc.remove(MUSIC_DISC_KEY);
        return true;
    }

    private String getSongFromDisc(ItemStack disc) {
        return "minecraft:music_disc." + disc.getType().toString().toLowerCase().split("music_disc_")[1];
    }
}
