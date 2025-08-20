package io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.jukebox;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.api.OraxenFurniture;
import io.th0rgal.oraxen.api.OraxenItems;
import io.th0rgal.oraxen.api.events.furniture.OraxenFurnitureBreakEvent;
import io.th0rgal.oraxen.api.events.furniture.OraxenFurnitureInteractEvent;
import io.th0rgal.oraxen.config.Message;
import io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.FurnitureMechanic;
import io.th0rgal.oraxen.utils.AdventureUtils;
import io.th0rgal.oraxen.utils.BlockHelpers;
import io.th0rgal.oraxen.utils.ItemUtils;
import io.th0rgal.oraxen.utils.MusicDiscHelpers;
import io.th0rgal.oraxen.utils.VersionUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;

import javax.annotation.Nullable;
import java.util.Locale;

public class JukeboxListener implements Listener {

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onInsertDisc(OraxenFurnitureInteractEvent event) {
        Entity baseEntity = event.getBaseEntity();
        Player player = event.getPlayer();
        ItemStack itemStack = player.getInventory().getItemInMainHand();

        if (event.getHand() != EquipmentSlot.HAND)
            return;

        boolean played = insertAndPlayDisc(baseEntity, itemStack, player);

        if (!played)
            return;
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
            Component message = AdventureUtils.MINI_MESSAGE.deserialize(
                Message.MECHANICS_JUKEBOX_NOW_PLAYING.toString(), AdventureUtils.tagResolver("disc", displayName));
            OraxenPlugin.get().getAudience().player(player).sendActionBar(message);
        }

        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onEjectDisc(OraxenFurnitureInteractEvent event) {
        if (!ejectAndStopDisc(event.getBaseEntity(), event.getPlayer()))
            return;
        event.getPlayer().swingMainHand();
        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onJukeboxBreak(OraxenFurnitureBreakEvent event) {
        ejectAndStopDisc(event.getBaseEntity(), null);
    }

    private boolean insertAndPlayDisc(Entity baseEntity, ItemStack disc, @Nullable Player player) {
        PersistentDataContainer pdc = baseEntity.getPersistentDataContainer();
        FurnitureMechanic furnitureMechanic = OraxenFurniture.getFurnitureMechanic(baseEntity);
        Location loc = BlockHelpers.toCenterLocation(baseEntity.getLocation());

        if (furnitureMechanic == null || !furnitureMechanic.isJukebox())
            return false;

        if (!ItemUtils.isMusicDisc(disc))
            return false;

        if (MusicDiscHelpers.hasMusicDisc(pdc))
            return false;

        JukeboxBlock jukebox = furnitureMechanic.getJukebox();
        if (!jukebox.hasPermission(player))
            return false;

        ItemStack insertedDisc = disc.clone();
        insertedDisc.setAmount(1);
        if (player != null && player.getGameMode() != GameMode.CREATIVE)
            disc.setAmount(disc.getAmount() - insertedDisc.getAmount());
        MusicDiscHelpers.setAndPlayMusicDisc(baseEntity, insertedDisc, jukebox.getVolume(), jukebox.getPitch());

        if (jukebox.active_stage != null) {
            FurnitureMechanic.setFurnitureItem(baseEntity, OraxenItems.getItemById(jukebox.active_stage).build());
        }

        return true;
    }

    private boolean ejectAndStopDisc(Entity baseEntity, @Nullable Player player) {
        PersistentDataContainer pdc = baseEntity.getPersistentDataContainer();
        FurnitureMechanic furnitureMechanic = OraxenFurniture.getFurnitureMechanic(baseEntity);

        if (furnitureMechanic == null || !furnitureMechanic.isJukebox())
            return false;
        JukeboxBlock jukebox = furnitureMechanic.getJukebox();
        if (!jukebox.hasPermission(player))
            return false;
        var item = MusicDiscHelpers.stopJukeboxAt(baseEntity, jukebox.getVolume(), jukebox.getPitch());
        if(item == null) return false;
        baseEntity.getWorld().dropItemNaturally(baseEntity.getLocation().toCenterLocation(), item);

        // if the active stage was not null we need to reset it because it changed
        if (jukebox.active_stage != null) {
            FurnitureMechanic.setFurnitureItem(baseEntity,
                OraxenItems.getItemById(furnitureMechanic.getItemID()).build());
        }
        return true;
    }

    @Nullable
    private NamespacedKey getSongFromDisc(ItemStack disc) {
        if (VersionUtil.atOrAbove("1.21") && disc.hasItemMeta() && disc.getItemMeta().hasJukeboxPlayable()) {
            return disc.getItemMeta().getJukeboxPlayable().getSongKey();
        } else {
            return NamespacedKey.minecraft("music_disc." + disc.getType().toString().toLowerCase(Locale.ROOT).split("music_disc_")[1]);
        }
    }
}
