package io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.jukebox;

import com.jeff_media.morepersistentdatatypes.DataType;
import io.papermc.paper.event.block.BlockBreakBlockEvent;
import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.api.OraxenFurniture;
import io.th0rgal.oraxen.api.OraxenItems;
import io.th0rgal.oraxen.config.Message;
import io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.FurnitureMechanic;
import io.th0rgal.oraxen.utils.AdventureUtils;
import io.th0rgal.oraxen.utils.BlockHelpers;
import io.th0rgal.oraxen.utils.VersionUtil;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
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
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;

import static io.th0rgal.oraxen.mechanics.provided.misc.music_disc.MusicDiscListener.MUSIC_DISC_KEY;

public class JukeboxListener implements Listener {

    public JukeboxListener() {
        if (VersionUtil.isPaperServer())
            Bukkit.getPluginManager().registerEvents(new JukeboxPaperListener(), OraxenPlugin.get());
    }

    public class JukeboxPaperListener implements Listener {

        @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
        public void onJukeboxBreak(BlockBreakBlockEvent event) {
            ejectAndStopDisc(event.getBlock(), null);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onInsertDisc(PlayerInteractEvent event) {
        Block block = event.getClickedBlock();
        Player player = event.getPlayer();
        ItemStack itemStack = player.getInventory().getItemInMainHand();

        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (event.getHand() != EquipmentSlot.HAND) return;

        boolean played = insertAndPlayDisc(block, itemStack, player);
        if (!played) return;
        player.swingMainHand();

        String displayName = null;
        if (itemStack.hasItemMeta()) {
            assert itemStack.getItemMeta() != null;
            if (itemStack.getItemMeta().hasLore()) {
                assert itemStack.getItemMeta().getLore() != null;
                displayName = itemStack.getItemMeta().getLore().get(0);
            } else if (OraxenItems.exists(itemStack) && itemStack.getItemMeta().hasDisplayName()) {
                displayName = itemStack.getItemMeta().getDisplayName();
            }
        }

        if (displayName != null) {
            Component message = AdventureUtils.MINI_MESSAGE.deserialize(Message.MECHANICS_JUKEBOX_NOW_PLAYING.toString(),
                    TagResolver.builder().resolvers(AdventureUtils.OraxenTagResolver, AdventureUtils.tagResolver("disc", displayName)).build());
            OraxenPlugin.get().getAudience().player(player).sendActionBar(message);
        }

        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onEjectDisc(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (event.getHand() != EquipmentSlot.HAND) return;

        boolean stopped = ejectAndStopDisc(event.getClickedBlock(), event.getPlayer());
        if (!stopped) return;
        event.getPlayer().swingMainHand();
        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onJukeboxBreak(BlockBreakEvent event) {
        ejectAndStopDisc(event.getBlock(), null);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onJukeboxBreak(BlockExplodeEvent event) {
        ejectAndStopDisc(event.getBlock(), null);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onJukeboxBreak(BlockBurnEvent event) {
        ejectAndStopDisc(event.getBlock(), null);
    }

    private boolean insertAndPlayDisc(Block block, ItemStack disc, @Nullable Player player) {
        PersistentDataContainer pdc = BlockHelpers.getPDC(block);
        FurnitureMechanic furnitureMechanic = OraxenFurniture.getFurnitureMechanic(block);
        Location loc = BlockHelpers.toCenterLocation(block.getLocation());

        if (furnitureMechanic == null || !furnitureMechanic.isJukebox()) return false;
        if (pdc.has(MUSIC_DISC_KEY, DataType.ITEM_STACK)) return false;
        if (disc == null || !Tag.ITEMS_MUSIC_DISCS.isTagged(disc.getType())) return false;
        JukeboxBlock jukebox = furnitureMechanic.getJukebox();
        if (!jukebox.hasPermission(player)) return false;
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
        FurnitureMechanic furnitureMechanic = OraxenFurniture.getFurnitureMechanic(block);
        Location loc = BlockHelpers.toCenterLocation(block.getLocation());

        if (furnitureMechanic == null || !furnitureMechanic.isJukebox()) return false;
        if (!pdc.has(MUSIC_DISC_KEY, DataType.ITEM_STACK)) return false;
        if (item == null || !Tag.ITEMS_MUSIC_DISCS.isTagged(item.getType())) return false;

        JukeboxBlock jukebox = furnitureMechanic.getJukebox();
        if (!jukebox.hasPermission(player)) return false;

        block.getWorld().getNearbyEntities(loc, 32, 32, 32).stream()
                .filter(entity -> entity instanceof Player)
                .map(entity -> (Player) entity)
                .forEach(p -> OraxenPlugin.get().getAudience().player(p).stopSound(Sound.sound(getSongFromDisc(item), Sound.Source.RECORD, jukebox.getVolume(), jukebox.getPitch())));
        block.getWorld().dropItemNaturally(loc, item);
        pdc.remove(MUSIC_DISC_KEY);
        return true;
    }

    private @NotNull Key getSongFromDisc(ItemStack disc) {
        return Key.key("minecraft", "music_disc." + disc.getType().toString().toLowerCase().split("music_disc_")[1]);
    }
}
