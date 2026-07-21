package io.th0rgal.oraxen.paintings;

import com.jeff_media.morepersistentdatatypes.DataType;
import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.api.OraxenItems;
import io.th0rgal.oraxen.items.ItemBuilder;
import io.th0rgal.oraxen.items.ItemUpdater;
import io.th0rgal.oraxen.protection.AntiGriefLib;
import io.th0rgal.oraxen.utils.ItemUtils;
import io.th0rgal.oraxen.utils.SchedulerUtil;
import io.th0rgal.oraxen.utils.VersionUtil;
import io.th0rgal.oraxen.utils.logs.Logs;
import net.kyori.adventure.key.Key;
import org.bukkit.Art;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Item;
import org.bukkit.entity.Painting;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDropItemEvent;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.event.hanging.HangingBreakEvent;
import org.bukkit.event.hanging.HangingPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CopyOnWriteArrayList;

public class CustomPaintingListener implements Listener {

    private static final NamespacedKey PLACED_PAINTING_ITEM = new NamespacedKey(OraxenPlugin.get(), "placed_painting_item");
    private static final NamespacedKey PLACED_PAINTING_ITEM_ID = new NamespacedKey(OraxenPlugin.get(), "placed_painting_item_id");
    private static final double DROP_MATCH_RADIUS_SQUARED = 4.0D;
    private static final List<PendingPaintingDrop> PENDING_PAINTING_DROPS = new CopyOnWriteArrayList<>();

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPaintingInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (event.useInteractedBlock() == Event.Result.DENY) return;
        if (event.useItemInHand() == Event.Result.DENY) return;

        EquipmentSlot hand = event.getHand();
        if (hand == null) return;
        if (isDuplicateOffHandPlacement(event, hand)) return;

        Block clickedBlock = event.getClickedBlock();
        if (clickedBlock == null) return;

        ItemStack item = event.getItem();
        ItemBuilder builder = getOraxenPaintingBuilder(item);
        if (builder == null || builder.getPaintingVariant() == null) return;

        if (tryPlace(event.getPlayer(), item, hand, clickedBlock, event.getBlockFace(), builder)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPaintingPlace(HangingPlaceEvent event) {
        if (!(event.getEntity() instanceof Painting painting)) return;

        ItemStack placedItem = getPlacedItem(event);
        if (!isOraxenPaintingItem(placedItem)) return;

        storePaintingItem(painting, placedItem);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPaintingBreak(HangingBreakEvent event) {
        if (!(event.getEntity() instanceof Painting painting)) return;

        ItemStack storedItem = getRestoredPaintingItem(painting);
        if (ItemUtils.isEmpty(storedItem)) return;

        PendingPaintingDrop pendingDrop = new PendingPaintingDrop(painting.getLocation(), storedItem);
        PENDING_PAINTING_DROPS.add(pendingDrop);
        SchedulerUtil.runTaskLater(40L, () -> PENDING_PAINTING_DROPS.remove(pendingDrop));
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPaintingDrop(EntityDropItemEvent event) {
        if (!(event.getEntity() instanceof Painting painting)) return;

        ItemStack storedItem = getRestoredPaintingItem(painting);
        if (ItemUtils.isEmpty(storedItem)) return;

        replaceDrop(event.getItemDrop(), storedItem);
        removePendingDropNear(event.getItemDrop().getLocation());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPaintingItemSpawn(ItemSpawnEvent event) {
        Item item = event.getEntity();
        if (item.getItemStack().getType() != Material.PAINTING) return;

        PendingPaintingDrop pendingDrop = findPendingDrop(item.getLocation());
        if (pendingDrop == null) return;

        replaceDrop(item, pendingDrop.itemStack());
        PENDING_PAINTING_DROPS.remove(pendingDrop);
    }

    private boolean tryPlace(Player player, ItemStack item, EquipmentSlot hand, Block clickedBlock, BlockFace face, ItemBuilder builder) {
        if (!VersionUtil.atOrAbove("1.21.5") || !VersionUtil.isPaperServer()) return false;
        if (!isWallFace(face)) return false;

        Block target = clickedBlock.getRelative(face);
        if (!target.getType().isAir()) return false;

        Art art = getPaintingArt(builder.getPaintingVariant());
        if (art == null) return false;

        Location location = target.getLocation();
        if (!AntiGriefLib.canBuild(player, location)) return false;

        Painting painting = location.getWorld().createEntity(location, Painting.class);
        if (painting == null) return false;

        boolean placed = false;
        try {
            if (!painting.setFacingDirection(face, true)) return false;
            if (!painting.setArt(art, true)) return false;

            HangingPlaceEvent placeEvent = new HangingPlaceEvent(painting, player, clickedBlock, face, hand, item);
            OraxenPlugin.get().getServer().getPluginManager().callEvent(placeEvent);
            if (placeEvent.isCancelled()) return false;

            location.getWorld().addEntity(painting);
            if (!painting.isValid()) return false;

            consumeItem(player, hand);
            placed = true;
            return true;
        } finally {
            if (!placed && painting != null) painting.remove();
        }
    }

    private Art getPaintingArt(String paintingVariant) {
        Key variantKey;
        try {
            variantKey = parsePaintingVariantKey(paintingVariant);
        } catch (IllegalArgumentException exception) {
            Logs.logWarning("Invalid painting_variant '" + paintingVariant + "'");
            Logs.debug(exception);
            return null;
        }

        try {
            Registry<Art> paintingRegistry = RegistryAccess.registryAccess()
                    .getRegistry(RegistryKey.PAINTING_VARIANT);
            Art art = paintingRegistry.get(variantKey);
            if (art == null) Logs.logWarning("Unknown painting_variant '" + paintingVariant + "'");
            return art;
        } catch (Exception exception) {
            Logs.logWarning("Failed to resolve painting_variant '" + paintingVariant + "'");
            Logs.debug(exception);
            return null;
        }
    }

    private void storePaintingItem(Painting painting, ItemStack placedItem) {
        ItemStack storedItem = placedItem.clone();
        storedItem.setAmount(1);

        PersistentDataContainer pdc = painting.getPersistentDataContainer();
        pdc.set(PLACED_PAINTING_ITEM, DataType.ITEM_STACK, storedItem);

        String itemId = OraxenItems.getIdByItem(storedItem);
        if (itemId != null) pdc.set(PLACED_PAINTING_ITEM_ID, PersistentDataType.STRING, itemId);
    }

    private ItemStack getPlacedItem(HangingPlaceEvent event) {
        ItemStack eventItem = getPlacedItemFromEvent(event);
        if (!ItemUtils.isEmpty(eventItem)) return eventItem;

        if (event.getPlayer() == null) return null;
        EquipmentSlot hand = event.getHand();
        if (hand == EquipmentSlot.OFF_HAND) return event.getPlayer().getInventory().getItemInOffHand();
        return event.getPlayer().getInventory().getItemInMainHand();
    }

    private ItemStack getPlacedItemFromEvent(HangingPlaceEvent event) {
        try {
            Method getItemStack = HangingPlaceEvent.class.getMethod("getItemStack");
            Object result = getItemStack.invoke(event);
            return result instanceof ItemStack itemStack ? itemStack : null;
        } catch (NoSuchMethodException ignored) {
            return null;
        } catch (IllegalAccessException | InvocationTargetException exception) {
            Logs.debug(exception);
            return null;
        }
    }

    private boolean isDuplicateOffHandPlacement(PlayerInteractEvent event, EquipmentSlot hand) {
        if (hand != EquipmentSlot.OFF_HAND) return false;
        return getOraxenPaintingBuilder(event.getPlayer().getInventory().getItemInMainHand()) != null;
    }

    private ItemBuilder getOraxenPaintingBuilder(ItemStack itemStack) {
        if (!isOraxenPaintingItem(itemStack)) return null;
        return OraxenItems.getBuilderByItem(itemStack);
    }

    private boolean isOraxenPaintingItem(ItemStack itemStack) {
        return !ItemUtils.isEmpty(itemStack)
                && itemStack.getType() == Material.PAINTING
                && OraxenItems.exists(itemStack);
    }

    private ItemStack getRestoredPaintingItem(Painting painting) {
        PersistentDataContainer pdc = painting.getPersistentDataContainer();

        ItemStack itemById = getStoredItemById(pdc);
        if (!ItemUtils.isEmpty(itemById)) return itemById;

        ItemStack itemByArt = getItemByArt(painting);
        if (!ItemUtils.isEmpty(itemByArt)) return itemByArt;

        ItemStack storedItem = getStoredItemStack(pdc);
        return !ItemUtils.isEmpty(storedItem) ? ItemUpdater.updateItem(storedItem.clone()) : null;
    }

    private ItemStack getStoredItemById(PersistentDataContainer pdc) {
        String itemId = pdc.get(PLACED_PAINTING_ITEM_ID, PersistentDataType.STRING);
        if (itemId == null) return null;

        ItemBuilder builder = OraxenItems.getItemById(itemId);
        return builder != null ? builder.build() : null;
    }

    private ItemStack getItemByArt(Painting painting) {
        String artKey = painting.getArt().getKey().toString();
        for (ItemBuilder builder : OraxenItems.getItems()) {
            if (builder == null || builder.getPaintingVariant() == null) continue;
            String builderVariantKey = normalizedPaintingVariantKey(builder.getPaintingVariant());
            if (builderVariantKey == null || !builderVariantKey.equalsIgnoreCase(artKey)) continue;
            ItemStack itemStack = builder.build();
            if (itemStack.getType() == Material.PAINTING) return itemStack;
        }
        return null;
    }

    private ItemStack getStoredItemStack(PersistentDataContainer pdc) {
        try {
            return pdc.get(PLACED_PAINTING_ITEM, DataType.ITEM_STACK);
        } catch (Exception exception) {
            Logs.debug(exception);
            return null;
        }
    }

    private void replaceDrop(Item itemDrop, ItemStack itemStack) {
        ItemStack restoredItem = itemStack.clone();
        restoredItem.setAmount(Math.max(1, itemDrop.getItemStack().getAmount()));
        itemDrop.setItemStack(restoredItem);
    }

    private PendingPaintingDrop findPendingDrop(Location location) {
        Iterator<PendingPaintingDrop> iterator = PENDING_PAINTING_DROPS.iterator();
        while (iterator.hasNext()) {
            PendingPaintingDrop pendingDrop = iterator.next();
            if (isSameWorldNear(pendingDrop.location(), location)) return pendingDrop;
        }
        return null;
    }

    private void removePendingDropNear(Location location) {
        PENDING_PAINTING_DROPS.removeIf(pendingDrop -> isSameWorldNear(pendingDrop.location(), location));
    }

    private boolean isSameWorldNear(Location first, Location second) {
        return first.getWorld() != null
                && first.getWorld().equals(second.getWorld())
                && first.distanceSquared(second) <= DROP_MATCH_RADIUS_SQUARED;
    }

    private void consumeItem(Player player, EquipmentSlot hand) {
        if (player.getGameMode() == GameMode.CREATIVE) return;

        ItemStack itemStack = hand == EquipmentSlot.OFF_HAND
                ? player.getInventory().getItemInOffHand()
                : player.getInventory().getItemInMainHand();
        if (ItemUtils.isEmpty(itemStack)) return;

        if (itemStack.getAmount() <= 1) {
            if (hand == EquipmentSlot.OFF_HAND) player.getInventory().setItemInOffHand(null);
            else player.getInventory().setItemInMainHand(null);
            return;
        }

        itemStack.setAmount(itemStack.getAmount() - 1);
    }

    private boolean isWallFace(BlockFace face) {
        return face == BlockFace.NORTH || face == BlockFace.SOUTH || face == BlockFace.EAST || face == BlockFace.WEST;
    }

    private Key parsePaintingVariantKey(String value) {
        String normalized = value.toLowerCase(Locale.ROOT);
        return normalized.contains(":") ? Key.key(normalized) : Key.key("oraxen", normalized);
    }

    private String normalizedPaintingVariantKey(String value) {
        try {
            return parsePaintingVariantKey(value).asString();
        } catch (IllegalArgumentException exception) {
            Logs.debug(exception);
            return null;
        }
    }

    private record PendingPaintingDrop(Location location, ItemStack itemStack) {
    }
}
