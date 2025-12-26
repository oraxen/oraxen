package io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.jukebox;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.api.OraxenFurniture;
import io.th0rgal.oraxen.api.OraxenItems;
import io.th0rgal.oraxen.api.events.furniture.OraxenFurnitureBreakEvent;
import io.th0rgal.oraxen.api.events.furniture.OraxenFurnitureInteractEvent;
import io.th0rgal.oraxen.config.AppearanceMode;
import io.th0rgal.oraxen.config.Message;
import io.th0rgal.oraxen.items.ItemBuilder;
import io.th0rgal.oraxen.items.OraxenMeta;
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
import org.bukkit.inventory.meta.ItemMeta;
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

        // Apply active model/stage
        applyActiveModel(baseEntity, furnitureMechanic, jukebox, true);

        return true;
    }

    /**
     * Applies the active or inactive model to a jukebox furniture.
     * Supports both the new Pack.models system and legacy active_stage.
     */
    @SuppressWarnings("deprecation")
    private void applyActiveModel(Entity baseEntity, FurnitureMechanic furnitureMechanic, JukeboxBlock jukebox, boolean active) {
        String itemId = furnitureMechanic.getItemID();
        ItemBuilder itemBuilder = OraxenItems.getItemById(itemId);
        if (itemBuilder == null) return;

        // Try new Pack.models system first (1.21.2+)
        if (jukebox.hasActiveModel() && VersionUtil.atOrAbove("1.21.2") && AppearanceMode.isItemPropertiesEnabled()) {
            OraxenMeta meta = itemBuilder.getOraxenMeta();
            if (meta != null && meta.hasAdditionalModels()) {
                String activeModelKey = jukebox.getActiveModel();
                if (meta.getAdditionalModel(activeModelKey) != null) {
                    // Get the current furniture item and swap its model
                    ItemStack furnitureItem = FurnitureMechanic.getFurnitureItem(baseEntity);
                    if (furnitureItem != null) {
                        ItemMeta itemMeta = furnitureItem.getItemMeta();
                        if (itemMeta != null) {
                            NamespacedKey modelKey = active
                                    ? new NamespacedKey("oraxen", itemId + "/" + activeModelKey)
                                    : new NamespacedKey("oraxen", itemId);
                            itemMeta.setItemModel(modelKey);
                            furnitureItem.setItemMeta(itemMeta);
                            FurnitureMechanic.setFurnitureItem(baseEntity, furnitureItem);
                        }
                    }
                    return;
                }
            }
        }

        // Fallback to legacy active_stage system
        if (jukebox.hasLegacyActiveStage()) {
            if (active) {
                ItemBuilder activeBuilder = OraxenItems.getItemById(jukebox.active_stage);
                if (activeBuilder != null) {
                    FurnitureMechanic.setFurnitureItem(baseEntity, activeBuilder.build());
                }
            } else {
                FurnitureMechanic.setFurnitureItem(baseEntity, itemBuilder.build());
            }
        }
    }

    @SuppressWarnings("deprecation")
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

        // Reset to inactive model
        if (jukebox.hasActiveModel() || jukebox.hasLegacyActiveStage()) {
            applyActiveModel(baseEntity, furnitureMechanic, jukebox, false);
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
