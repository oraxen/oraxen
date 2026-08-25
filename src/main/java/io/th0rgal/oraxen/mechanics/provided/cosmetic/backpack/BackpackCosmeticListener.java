package io.th0rgal.oraxen.mechanics.provided.cosmetic.backpack;

import io.th0rgal.oraxen.api.OraxenItems;
import io.th0rgal.oraxen.mechanics.Mechanic;
import io.th0rgal.oraxen.nms.NMSHandlers;
import io.th0rgal.oraxen.utils.SchedulerUtil;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityDismountEvent;
import org.bukkit.event.entity.EntityMountEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.EntityToggleGlideEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Listener for backpack cosmetic mechanics.
 * Handles equipment changes, movement, and player lifecycle events.
 */
public class BackpackCosmeticListener implements Listener {

    private final BackpackCosmeticFactory factory;
    private final BackpackCosmeticManager manager;
    private final Set<UUID> hiddenForMovement = ConcurrentHashMap.newKeySet();
    private final Map<UUID, BackpackCosmeticMechanic> hiddenMovementMechanics = new ConcurrentHashMap<>();
    // Armor stand displays: real armor stand UUID -> display data
    private final Map<UUID, StandDisplayData> armorStandDisplays = new ConcurrentHashMap<>();

    // Movement thresholds to reduce unnecessary updates
    // Without mount packets, we need more frequent updates for smooth following
    private static final double POSITION_THRESHOLD = 0.01;
    private static final float YAW_THRESHOLD = 1.0f;

    public BackpackCosmeticListener(BackpackCosmeticFactory factory) {
        this.factory = factory;
        this.manager = BackpackCosmeticManager.getInstance();
        // Note: Refresh task is registered in BackpackCosmeticFactory with MechanicsManager
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        SchedulerUtil.runTaskLater(5L, () -> {
            checkAndUpdateBackpack(player);
            // Immediate pass; the periodic refresh below is the safety net that makes
            // display restoration independent of entity-loading timing on relogin.
            refreshArmorStandDisplays();
        });
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        clearMovementHidden(player.getUniqueId());
        manager.hideBackpack(player);
        // Detach the quitting viewer from every armor stand display (client resets anyway)
        for (StandDisplayData data : armorStandDisplays.values()) {
            data.getViewers().remove(player.getUniqueId());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerDeath(PlayerDeathEvent event) {
        clearMovementHidden(event.getEntity().getUniqueId());
        manager.hideBackpack(event.getEntity());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();

        // Re-check equipment after respawn
        SchedulerUtil.runTaskLater(5L, () -> checkAndUpdateBackpack(player));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerChangedWorld(PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();

        // Hide backpack first, then re-show after world change
        manager.hideBackpack(player);
        SchedulerUtil.runTaskLater(5L, () -> checkAndUpdateBackpack(player));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        // Check if clicking in player's inventory
        if (event.getClickedInventory() instanceof PlayerInventory) {
            // Delay check to after the inventory update
            SchedulerUtil.runTaskLater(1L, () -> checkAndUpdateBackpack(player));
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        // Handle right-click to equip armor
        if (event.getAction().toString().contains("RIGHT_CLICK")) {
            ItemStack item = event.getItem();
            if (item != null && isArmorItem(item)) {
                SchedulerUtil.runTaskLater(1L, () -> checkAndUpdateBackpack(event.getPlayer()));
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();

        if (updateBackpackVisibilityForMovement(player)) return;
        if (!manager.hasBackpack(player)) return;

        // getTo() can return null in some edge cases
        if (event.getTo() == null) return;

        // Check if position or yaw changed significantly
        if (event.getFrom().getWorld() != event.getTo().getWorld()) return;

        double distSq = event.getFrom().distanceSquared(event.getTo());
        float yawDiff = Math.abs(event.getFrom().getYaw() - event.getTo().getYaw());

        if (distSq > POSITION_THRESHOLD * POSITION_THRESHOLD || yawDiff > YAW_THRESHOLD) {
            manager.updateBackpackPosition(player);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityToggleGlide(EntityToggleGlideEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;

        SchedulerUtil.runTaskLater(1L, () -> checkAndUpdateBackpack(player));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityMount(EntityMountEvent event) {
        if (!(event.getMount() instanceof Player player)) return;
        if (!manager.hasBackpack(player)) return;

        scheduleBackpackMountResync(player);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDismount(EntityDismountEvent event) {
        if (!(event.getDismounted() instanceof Player player)) return;
        if (!manager.hasBackpack(player)) return;

        scheduleBackpackMountResync(player);
    }

    // Schedules two resyncs because mount/dismount packets can arrive out of order with the
    // passenger-list updates the client uses; the second pass is a safety net for that race.
    private void scheduleBackpackMountResync(Player player) {
        manager.requestResync(player);
        SchedulerUtil.runTaskLater(1L, () -> manager.resyncBackpackMount(player));
        SchedulerUtil.runTaskLater(2L, () -> manager.resyncBackpackMount(player));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerGameModeChange(PlayerGameModeChangeEvent event) {
        Player player = event.getPlayer();
        BackpackCosmeticManager.BackpackData data = manager.getBackpackData(player);

        if (data == null) return;

        BackpackCosmeticMechanic mechanic = data.getMechanic();
        if (mechanic.hideInSpectator()) {
            if (event.getNewGameMode() == GameMode.SPECTATOR) {
                manager.hideBackpack(player);
            } else if (player.getGameMode() == GameMode.SPECTATOR) {
                // Re-show when leaving spectator
                SchedulerUtil.runTaskLater(1L, () -> checkAndUpdateBackpack(player));
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onItemPickup(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        // Check after pickup completes
        SchedulerUtil.runTaskLater(1L, () -> checkAndUpdateBackpack(player));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerItemHeld(PlayerItemHeldEvent event) {
        // When player switches held item, check if backpack visibility should change
        SchedulerUtil.runTaskLater(1L, () -> checkAndUpdateBackpack(event.getPlayer()));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        // When player drops item, check if backpack visibility should change
        SchedulerUtil.runTaskLater(1L, () -> checkAndUpdateBackpack(event.getPlayer()));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerSwapHandItems(PlayerSwapHandItemsEvent event) {
        // When player swaps hand items, check if backpack visibility should change
        SchedulerUtil.runTaskLater(1L, () -> checkAndUpdateBackpack(event.getPlayer()));
    }

    /**
     * Check player's equipment/inventory and update backpack display accordingly
     */
    private void checkAndUpdateBackpack(Player player) {
        if (!player.isOnline()) return;

        BackpackSearchResult result = findBackpackItem(player);

        if (result == null) {
            clearMovementHidden(player.getUniqueId());
            manager.hideBackpack(player);
            return;
        }

        if (player.getGameMode() == GameMode.SPECTATOR && result.mechanic.hideInSpectator()) {
            clearMovementHidden(player.getUniqueId());
            manager.hideBackpack(player);
            return;
        }

        if (shouldHideBackpackForMovement(player, result.mechanic)) {
            setMovementHidden(player.getUniqueId(), result.mechanic);
            manager.hideBackpack(player);
            return;
        }

        clearMovementHidden(player.getUniqueId());
        updateBackpackDisplay(player, result.mechanic, result.item);
    }

    /**
     * Update backpack visibility when the player enters or leaves movement states
     * that should hide the cosmetic backpack.
     */
    private boolean updateBackpackVisibilityForMovement(Player player) {
        UUID playerId = player.getUniqueId();
        BackpackCosmeticManager.BackpackData data = manager.getBackpackData(player);
        BackpackCosmeticMechanic mechanic = data != null ? data.getMechanic() : hiddenMovementMechanics.get(playerId);

        if (mechanic == null) return false;

        if (shouldHideBackpackForMovement(player, mechanic)) {
            if (manager.hasBackpack(player)) {
                setMovementHidden(playerId, mechanic);
                manager.hideBackpack(player);
            }
            return true;
        }

        if (hiddenForMovement.remove(playerId)) {
            hiddenMovementMechanics.remove(playerId);
            SchedulerUtil.runTaskLater(1L, () -> checkAndUpdateBackpack(player));
        }
        return false;
    }

    private void setMovementHidden(UUID playerId, BackpackCosmeticMechanic mechanic) {
        hiddenForMovement.add(playerId);
        hiddenMovementMechanics.put(playerId, mechanic);
    }

    private void clearMovementHidden(UUID playerId) {
        hiddenForMovement.remove(playerId);
        hiddenMovementMechanics.remove(playerId);
    }

    private boolean shouldHideBackpackForMovement(Player player, BackpackCosmeticMechanic mechanic) {
        return mechanic.hideWhileGliding() && player.isGliding()
            || mechanic.hideWhileSwimming() && player.isSwimming();
    }

    /**
     * Search result containing the found backpack mechanic and item.
     */
    private record BackpackSearchResult(BackpackCosmeticMechanic mechanic, ItemStack item) {}

    /**
     * Find a backpack item in the player's equipment or inventory.
     * First checks equipment slots for slot-based triggers, then inventory for inventory-based triggers.
     */
    private BackpackSearchResult findBackpackItem(Player player) {
        PlayerInventory inv = player.getInventory();

        // First, check equipment slots for slot-based triggers
        BackpackSearchResult slotResult = findSlotBasedBackpack(inv);
        if (slotResult != null) {
            return slotResult;
        }

        // If no slot-based trigger found, check inventory for inventory-based triggers
        return findInventoryBasedBackpack(inv);
    }

    /**
     * Check equipment slots for slot-based backpack triggers.
     */
    private BackpackSearchResult findSlotBasedBackpack(PlayerInventory inv) {
        // Only check player-valid slots (skip BODY which is for wolf armor)
        EquipmentSlot[] playerSlots = {EquipmentSlot.HAND, EquipmentSlot.OFF_HAND, EquipmentSlot.FEET, EquipmentSlot.LEGS, EquipmentSlot.CHEST, EquipmentSlot.HEAD};
        for (EquipmentSlot slot : playerSlots) {
            BackpackSearchResult result = findSlotBasedBackpack(inv, slot);
            if (result != null) return result;
        }
        return null;
    }

    /**
     * Check inventory (excluding hands) for inventory-based backpack triggers.
     */
    private BackpackSearchResult findInventoryBasedBackpack(PlayerInventory inv) {
        int heldSlot = inv.getHeldItemSlot();
        int offHandSlot = 40;

        ItemStack[] contents = inv.getContents();
        for (int slot = 0; slot < contents.length; slot++) {
            BackpackSearchResult result = findInventoryBackpack(contents, slot, heldSlot, offHandSlot);
            if (result != null) return result;
        }
        return null;
    }

    private BackpackSearchResult findSlotBasedBackpack(PlayerInventory inv, EquipmentSlot slot) {
        ItemStack item = inv.getItem(slot);
        if (isEmptyItem(item)) return null;

        BackpackCosmeticMechanic mechanic = getBackpackMechanic(item);
        if (mechanic == null) return null;
        if (mechanic.triggersFromInventory() || mechanic.getTriggerSlot() != slot) return null;

        return new BackpackSearchResult(mechanic, item);
    }

    private BackpackSearchResult findInventoryBackpack(ItemStack[] contents, int slot, int heldSlot, int offHandSlot) {
        if (slot == heldSlot || slot == offHandSlot) return null;

        ItemStack item = contents[slot];
        if (isEmptyItem(item)) return null;

        BackpackCosmeticMechanic mechanic = getBackpackMechanic(item);
        if (mechanic == null || !mechanic.triggersFromInventory()) return null;

        return new BackpackSearchResult(mechanic, item);
    }

    private boolean isEmptyItem(ItemStack item) {
        return item == null || item.getType().isAir();
    }

    /**
     * Get the BackpackCosmeticMechanic for an item, or null if not a backpack item.
     */
    private BackpackCosmeticMechanic getBackpackMechanic(ItemStack item) {
        String itemId = OraxenItems.getIdByItem(item);
        if (itemId == null) return null;

        Mechanic mechanic = factory.getMechanic(itemId);
        if (mechanic instanceof BackpackCosmeticMechanic backpackMechanic) {
            return backpackMechanic;
        }
        return null;
    }

    /**
     * Update the backpack display if needed.
     */
    private void updateBackpackDisplay(Player player, BackpackCosmeticMechanic mechanic, ItemStack item) {
        BackpackCosmeticManager.BackpackData currentData = manager.getBackpackData(player);
        boolean needsUpdate = currentData == null ||
            currentData.getMechanic() != mechanic ||
            !item.isSimilar(currentData.getDisplayItem());

        if (needsUpdate) {
            manager.showBackpack(player, mechanic, item);
        }
    }

    private boolean isArmorItem(ItemStack item) {
        String typeName = item.getType().name();
        return typeName.endsWith("_HELMET") ||
               typeName.endsWith("_CHESTPLATE") ||
               typeName.endsWith("_LEGGINGS") ||
               typeName.endsWith("_BOOTS") ||
               typeName.equals("ELYTRA");
    }

    // ──────────────────────────────────────────────
    //  Armor stand display support
    //
    //  Mirrors the player-path architecture: displays are tracked with their
    //  viewer sets and maintained by a periodic refresh, so restoration is
    //  independent of join/entity-load timing (fixes relogin desyncs where a
    //  one-shot join restore races the client learning the vehicle entity).
    // ──────────────────────────────────────────────

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onArmorStandManipulate(PlayerArmorStandManipulateEvent event) {
        ArmorStand stand = event.getRightClicked();
        // We only care about the chest slot (where back cosmetics go)
        if (event.getSlot() != EquipmentSlot.CHEST) return;

        // Schedule a delayed check after the item swap completes
        SchedulerUtil.runTaskLater(1L, () -> checkArmorStandDisplay(stand));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onArmorStandDeath(EntityDeathEvent event) {
        if (event.getEntity() instanceof ArmorStand stand) {
            removeArmorStandDisplay(stand.getUniqueId());
        }
    }

    /**
     * Check whether an armor stand's chest slot holds a backpack cosmetic and
     * spawn or destroy the display accordingly.
     */
    private void checkArmorStandDisplay(ArmorStand stand) {
        if (!stand.isValid()) {
            removeArmorStandDisplay(stand.getUniqueId());
            return;
        }

        ItemStack chestItem = stand.getEquipment() != null ? stand.getEquipment().getChestplate() : null;
        BackpackCosmeticMechanic mechanic = getBackpackMechanic(chestItem);

        if (mechanic != null) {
            // Item is a backpack cosmetic — ensure a display exists (fresh entity id
            // whenever the mechanic/item changed) and spawn for everyone in range.
            StandDisplayData data = ensureStandDisplay(stand, mechanic, chestItem);
            for (Player viewer : stand.getWorld().getPlayers()) {
                spawnArmorStandDisplayForViewer(viewer, stand, data);
            }
        } else {
            // No backpack item — remove any existing display
            removeArmorStandDisplay(stand.getUniqueId());
        }
    }

    /**
     * Get the tracked display for this stand, recreating it when absent or when
     * the mechanic/display item changed. Recreating issues a fresh entity id,
     * which avoids stale ghost entities on clients that saw the old display.
     */
    private StandDisplayData ensureStandDisplay(ArmorStand stand, BackpackCosmeticMechanic mechanic, ItemStack displayItem) {
        StandDisplayData data = armorStandDisplays.get(stand.getUniqueId());
        if (data != null && data.getMechanic() == mechanic && displayItem.isSimilar(data.getDisplayItem())) {
            return data;
        }
        removeArmorStandDisplay(stand.getUniqueId());

        data = new StandDisplayData(NMSHandlers.getHandler().getNextEntityId(), stand.getUniqueId(), mechanic, displayItem);
        armorStandDisplays.put(stand.getUniqueId(), data);
        return data;
    }

    /**
     * Periodic maintenance (every second):
     *  - destroy displays whose stand died/vanished or whose item is no longer a cosmetic
     *  - add in-range viewers (covers joins and walk-ins regardless of event timing)
     *  - drop out-of-range/offline viewers
     *  - discover stands that gained a cosmetic while untracked
     */
    public void refreshArmorStandDisplays() {
        // Maintain tracked displays
        Iterator<Map.Entry<UUID, StandDisplayData>> iterator = armorStandDisplays.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, StandDisplayData> entry = iterator.next();
            StandDisplayData data = entry.getValue();

            Entity entity = Bukkit.getEntity(data.getStandUuid());
            if (!(entity instanceof ArmorStand stand) || !stand.isValid()) {
                destroyDisplay(data);
                iterator.remove();
                continue;
            }

            ItemStack chestItem = stand.getEquipment() != null ? stand.getEquipment().getChestplate() : null;
            if (getBackpackMechanic(chestItem) == null) {
                destroyDisplay(data);
                iterator.remove();
                continue;
            }

            double maxRangeSq = (double) data.getMechanic().getViewDistance() * data.getMechanic().getViewDistance();

            for (Player viewer : stand.getWorld().getPlayers()) {
                boolean inRange = viewer.getLocation().distanceSquared(stand.getLocation()) <= maxRangeSq;
                boolean isViewer = data.getViewers().contains(viewer.getUniqueId());

                if (inRange && !isViewer) {
                    spawnArmorStandDisplayForViewer(viewer, stand, data);
                } else if (!inRange && isViewer) {
                    data.getViewers().remove(viewer.getUniqueId());
                    NMSHandlers.getHandler().sendEntityDestroy(viewer, data.getEntityId());
                }
            }

            data.getViewers().removeIf(viewerId -> {
                Player viewer = Bukkit.getPlayer(viewerId);
                return viewer == null || !viewer.isOnline();
            });
        }

        // Discover untracked stands (cosmetic added while nobody was around, etc.)
        for (World world : Bukkit.getWorlds()) {
            for (ArmorStand stand : world.getEntitiesByClass(ArmorStand.class)) {
                if (armorStandDisplays.containsKey(stand.getUniqueId())) continue;

                ItemStack chestItem = stand.getEquipment() != null ? stand.getEquipment().getChestplate() : null;
                BackpackCosmeticMechanic mechanic = getBackpackMechanic(chestItem);
                if (mechanic == null) continue;

                StandDisplayData data = ensureStandDisplay(stand, mechanic, chestItem);
                for (Player viewer : stand.getWorld().getPlayers()) {
                    spawnArmorStandDisplayForViewer(viewer, stand, data);
                }
            }
        }
    }

    /**
     * Spawn the display for one viewer. Idempotent per viewer; follows with two
     * delayed mount resyncs so the mount survives the client learning the
     * vehicle entity late (the same race the player path guards against).
     */
    private void spawnArmorStandDisplayForViewer(Player viewer, ArmorStand stand, StandDisplayData data) {
        if (!viewer.isOnline() || !viewer.getWorld().equals(stand.getWorld())) return;

        double maxRangeSq = (double) data.getMechanic().getViewDistance() * data.getMechanic().getViewDistance();
        if (viewer.getLocation().distanceSquared(stand.getLocation()) > maxRangeSq) return;
        if (!data.getViewers().add(viewer.getUniqueId())) return; // already viewing

        Location spawnLoc = stand.getLocation().clone();
        NMSHandlers.getHandler().spawnBackpackArmorStand(
            viewer, data.getEntityId(), spawnLoc, data.getDisplayItem(), data.getMechanic().isSmallArmorStand()
        );

        // Mount the display as a passenger of the real armor stand so it follows
        // position and rotation automatically.
        NMSHandlers.getHandler().sendMountPacket(viewer, stand.getEntityId(), data.getEntityId());

        // Correct the initial body-vs-head rotation offset on the displayed model.
        NMSHandlers.getHandler().sendEntityHeadRotation(viewer, data.getEntityId(), stand.getYaw());

        // Mount resyncs: if these packets raced the client's knowledge of the
        // vehicle entity, re-sending the mount shortly after repairs it.
        UUID viewerId = viewer.getUniqueId();
        UUID standId = stand.getUniqueId();
        SchedulerUtil.runTaskLater(1L, () -> resyncStandMount(viewerId, standId));
        SchedulerUtil.runTaskLater(2L, () -> resyncStandMount(viewerId, standId));
    }

    private void resyncStandMount(UUID viewerId, UUID standId) {
        StandDisplayData data = armorStandDisplays.get(standId);
        if (data == null || !data.getViewers().contains(viewerId)) return;

        Player viewer = Bukkit.getPlayer(viewerId);
        Entity stand = Bukkit.getEntity(standId);
        if (viewer == null || !viewer.isOnline() || !(stand instanceof ArmorStand)) return;

        NMSHandlers.getHandler().sendMountPacket(viewer, stand.getEntityId(), data.getEntityId());
    }

    /**
     * Remove a backpack cosmetic display from an armor stand.
     */
    private void removeArmorStandDisplay(UUID standId) {
        StandDisplayData data = armorStandDisplays.remove(standId);
        if (data == null) return;

        destroyDisplay(data);
    }

    private void destroyDisplay(StandDisplayData data) {
        for (UUID viewerId : data.getViewers()) {
            Player viewer = Bukkit.getPlayer(viewerId);
            if (viewer != null && viewer.isOnline()) {
                NMSHandlers.getHandler().sendEntityDestroy(viewer, data.getEntityId());
            }
        }
        data.getViewers().clear();
    }

    /**
     * Tracked display for one armor stand. Viewer-set based, like the player
     * path's BackpackData, so restoration is driven by the refresh loop rather
     * than fragile one-shot join hooks.
     */
    private static class StandDisplayData {
        private final int entityId;
        private final UUID standUuid;
        private final BackpackCosmeticMechanic mechanic;
        private final ItemStack displayItem;
        private final Set<UUID> viewers = ConcurrentHashMap.newKeySet();

        private StandDisplayData(int entityId, UUID standUuid, BackpackCosmeticMechanic mechanic, ItemStack displayItem) {
            this.entityId = entityId;
            this.standUuid = standUuid;
            this.mechanic = mechanic;
            this.displayItem = displayItem;
        }

        int getEntityId() {
            return entityId;
        }

        UUID getStandUuid() {
            return standUuid;
        }

        BackpackCosmeticMechanic getMechanic() {
            return mechanic;
        }

        ItemStack getDisplayItem() {
            return displayItem;
        }

        Set<UUID> getViewers() {
            return viewers;
        }
    }
}