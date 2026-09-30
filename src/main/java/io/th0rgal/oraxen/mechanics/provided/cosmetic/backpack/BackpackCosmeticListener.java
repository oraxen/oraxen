package io.th0rgal.oraxen.mechanics.provided.cosmetic.backpack;

import io.th0rgal.oraxen.api.OraxenItems;
import io.th0rgal.oraxen.mechanics.Mechanic;
import io.th0rgal.oraxen.utils.SchedulerUtil;
import io.th0rgal.oraxen.utils.VersionUtil;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.EntityToggleGlideEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Method;
import java.util.HashSet;
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
    private final Map<UUID, BackpackCosmeticManager.BackpackData> armorStandDisplays = new ConcurrentHashMap<>();
    private final Map<UUID, SchedulerUtil.ScheduledTask> armorStandViewerTasks = new ConcurrentHashMap<>();
    private volatile boolean armorStandTasksClosed;

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

        SchedulerUtil.runForEntityLater(player, 5L, () -> {
            checkAndUpdateBackpack(player);
            startArmorStandViewerTask(player);
            refreshArmorStandDisplays(player);
        });
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();
        clearMovementHidden(playerId);
        manager.hideBackpack(player);

        SchedulerUtil.ScheduledTask task = armorStandViewerTasks.remove(playerId);
        if (task != null) task.cancel();
        for (Map.Entry<UUID, BackpackCosmeticManager.BackpackData> entry : armorStandDisplays.entrySet()) {
            BackpackCosmeticManager.BackpackData data = entry.getValue();
            // Only prune when this player was a viewer; an empty set may just be awaiting its first viewer.
            if (data.getViewers().remove(playerId) && data.getViewers().isEmpty()) {
                armorStandDisplays.remove(entry.getKey(), data);
            }
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
        SchedulerUtil.runForEntityLater(player, 5L, () -> checkAndUpdateBackpack(player));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerChangedWorld(PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();

        // Hide backpack first, then re-show after world change
        manager.hideBackpack(player);
        SchedulerUtil.runForEntityLater(player, 5L, () -> checkAndUpdateBackpack(player));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        // Check if clicking in player's inventory
        if (event.getClickedInventory() instanceof PlayerInventory) {
            // Delay check to after the inventory update
            SchedulerUtil.runForEntityLater(player, 1L, () -> checkAndUpdateBackpack(player));
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        // Handle right-click to equip armor
        if (event.getAction().toString().contains("RIGHT_CLICK")) {
            ItemStack item = event.getItem();
            if (item != null && isArmorItem(item)) {
                Player player = event.getPlayer();
                SchedulerUtil.runForEntityLater(player, 1L, () -> checkAndUpdateBackpack(player));
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

        SchedulerUtil.runForEntityLater(player, 1L, () -> checkAndUpdateBackpack(player));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onArmorStandManipulate(PlayerArmorStandManipulateEvent event) {
        ArmorStand stand = event.getRightClicked();
        Player player = event.getPlayer();
        if (!factory.isArmorStandEnabled() || event.getSlot() != EquipmentSlot.CHEST) return;

        SchedulerUtil.runForEntityLater(stand, 1L, () -> checkArmorStandDisplay(stand));
        SchedulerUtil.runForEntityLater(player, 2L, () -> refreshArmorStandDisplays(player));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onArmorStandDeath(EntityDeathEvent event) {
        if (event.getEntity() instanceof ArmorStand stand) {
            removeArmorStandDisplay(stand.getUniqueId());
        }
    }

    /**
     * Creates the mount/dismount listener for backpack resyncs.
     * The mount events moved from org.spigotmc to org.bukkit.event.entity in 1.20.5;
     * referencing the modern classes in this listener would make Bukkit reject
     * registration of every handler of this class on older servers, so they live
     * in a dedicated listener that is only used where the classes exist.
     */
    public Listener createMountListener(JavaPlugin plugin) {
        if (VersionUtil.atOrAbove("1.20.5")) return new MountListener();

        Listener legacyListener = new Listener() {
        };
        registerLegacyMountEvent(plugin, legacyListener, "org.spigotmc.event.entity.EntityMountEvent");
        registerLegacyMountEvent(plugin, legacyListener, "org.spigotmc.event.entity.EntityDismountEvent");
        return legacyListener;
    }

    /**
     * Mount/dismount handlers for 1.20.5+ servers, where the events live in org.bukkit.event.entity.
     */
    public class MountListener implements Listener {

        @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
        public void onEntityMount(org.bukkit.event.entity.EntityMountEvent event) {
            handleMountChange(event.getEntity());
            handleArmorStandMountChange(event.getMount());
        }

        @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
        public void onEntityDismount(org.bukkit.event.entity.EntityDismountEvent event) {
            handleMountChange(event.getEntity());
            handleArmorStandMountChange(event.getDismounted());
        }
    }

    /**
     * Reflectively registers a handler for the legacy org.spigotmc mount events used before 1.20.5.
     */
    private void registerLegacyMountEvent(JavaPlugin plugin, Listener listener, String eventClassName) {
        try {
            Class<? extends Event> eventClass = Class.forName(eventClassName).asSubclass(Event.class);
            Method entityGetter = eventClass.getMethod("getEntity");
            String vehicleGetterName = eventClassName.endsWith("EntityMountEvent") ? "getMount" : "getDismounted";
            Method vehicleGetter;
            try {
                vehicleGetter = eventClass.getMethod(vehicleGetterName);
            } catch (NoSuchMethodException ignored) {
                vehicleGetter = null;
            }
            Method finalVehicleGetter = vehicleGetter;
            Bukkit.getPluginManager().registerEvent(eventClass, listener, EventPriority.MONITOR, (l, event) -> {
                if (!eventClass.isInstance(event)) return;
                try {
                    if (entityGetter.invoke(event) instanceof Entity entity) handleMountChange(entity);
                    if (finalVehicleGetter != null && finalVehicleGetter.invoke(event) instanceof Entity vehicle) {
                        handleArmorStandMountChange(vehicle);
                    }
                } catch (ReflectiveOperationException ignored) {
                }
            }, plugin, true);
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            // Event class not present or not accessible on this server; mount resyncs are skipped.
        }
    }

    private void handleMountChange(Entity mounted) {
        if (!(mounted instanceof Player player)) return;
        if (!manager.hasBackpack(player)) return;

        scheduleBackpackMountResync(player);
    }

    private void handleArmorStandMountChange(Entity vehicle) {
        if (!factory.isArmorStandEnabled() || !(vehicle instanceof ArmorStand stand)) return;

        BackpackCosmeticManager.BackpackData data = armorStandDisplays.get(stand.getUniqueId());
        if (data == null) return;

        SchedulerUtil.runForEntity(stand, () -> {
            if (armorStandDisplays.get(stand.getUniqueId()) != data || !stand.isValid()) return;

            int[] passengerIds = manager.getMergedPassengerIds(stand, data.getEntityId());
            int vehicleId = stand.getEntityId();
            float yaw = stand.getYaw();
            for (UUID viewerId : data.getViewers()) {
                Player viewer = Bukkit.getPlayer(viewerId);
                if (viewer == null) continue;

                SchedulerUtil.runForEntity(viewer, () -> {
                    if (viewer.isOnline() && data.getViewers().contains(viewerId)
                            && armorStandDisplays.get(stand.getUniqueId()) == data) {
                        manager.sendBackpackMount(viewer, data, vehicleId, yaw, passengerIds, false);
                    }
                });
            }
        });
    }

    private void scheduleBackpackMountResync(Player player) {
        manager.requestResync(player);
        SchedulerUtil.runForEntityLater(player, 1L, () -> manager.resyncBackpackMount(player));
        SchedulerUtil.runForEntityLater(player, 2L, () -> manager.resyncBackpackMount(player));
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
                SchedulerUtil.runForEntityLater(player, 1L, () -> checkAndUpdateBackpack(player));
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onItemPickup(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        // Check after pickup completes
        SchedulerUtil.runForEntityLater(player, 1L, () -> checkAndUpdateBackpack(player));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerItemHeld(PlayerItemHeldEvent event) {
        // When player switches held item, check if backpack visibility should change
        Player player = event.getPlayer();
        SchedulerUtil.runForEntityLater(player, 1L, () -> checkAndUpdateBackpack(player));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        // When player drops item, check if backpack visibility should change
        Player player = event.getPlayer();
        SchedulerUtil.runForEntityLater(player, 1L, () -> checkAndUpdateBackpack(player));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerSwapHandItems(PlayerSwapHandItemsEvent event) {
        // When player swaps hand items, check if backpack visibility should change
        Player player = event.getPlayer();
        SchedulerUtil.runForEntityLater(player, 1L, () -> checkAndUpdateBackpack(player));
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
            SchedulerUtil.runForEntityLater(player, 1L, () -> checkAndUpdateBackpack(player));
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

    void startExistingArmorStandViewerTasks() {
        if (!factory.isArmorStandEnabled()) return;

        for (Player player : Bukkit.getOnlinePlayers()) {
            startArmorStandViewerTask(player);
        }
    }

    private void startArmorStandViewerTask(Player viewer) {
        if (!factory.isArmorStandEnabled() || armorStandTasksClosed || !viewer.isOnline()) return;

        UUID viewerId = viewer.getUniqueId();
        if (armorStandViewerTasks.containsKey(viewerId)) return;

        SchedulerUtil.ScheduledTask task = SchedulerUtil.runForEntityTimer(viewer, 1L, 20L,
                () -> refreshArmorStandDisplays(viewer),
                () -> armorStandViewerTasks.remove(viewerId));
        if (task == null) return;

        // Per-player tasks are not registered with MechanicsManager: they are cancelled on quit
        // and by cleanupArmorStandDisplays() on unregister, so they never outlive their player.
        SchedulerUtil.ScheduledTask previous = armorStandViewerTasks.putIfAbsent(viewerId, task);
        if (previous != null || armorStandTasksClosed) {
            if (previous == null) armorStandViewerTasks.remove(viewerId, task);
            task.cancel();
        }
    }

    private void refreshArmorStandDisplays(Player viewer) {
        if (!factory.isArmorStandEnabled() || !viewer.isOnline()) return;

        int armorStandRange = factory.getArmorStandRange();
        Set<UUID> nearbyStandIds = new HashSet<>();
        for (Entity entity : viewer.getNearbyEntities(
                armorStandRange, armorStandRange, armorStandRange)) {
            if (entity instanceof ArmorStand stand) {
                nearbyStandIds.add(stand.getUniqueId());
                refreshArmorStandForViewer(viewer, stand);
            }
        }

        for (Map.Entry<UUID, BackpackCosmeticManager.BackpackData> entry : armorStandDisplays.entrySet()) {
            if (!nearbyStandIds.contains(entry.getKey())) {
                removeArmorStandViewer(viewer, entry.getKey(), entry.getValue());
            }
        }
    }

    private void refreshArmorStandForViewer(Player viewer, ArmorStand stand) {
        SchedulerUtil.runForEntity(stand, () -> {
            if (!stand.isValid()) {
                removeArmorStandDisplay(stand.getUniqueId());
                return;
            }

            ItemStack chestItem = stand.getEquipment() == null ? null : stand.getEquipment().getChestplate();
            BackpackCosmeticMechanic mechanic = getBackpackMechanic(chestItem);
            if (mechanic == null) {
                removeArmorStandDisplay(stand.getUniqueId());
                return;
            }

            BackpackCosmeticManager.BackpackData data = ensureArmorStandDisplay(stand, mechanic, chestItem);
            Location location = stand.getLocation().clone();
            int vehicleId = stand.getEntityId();
            float yaw = stand.getYaw();
            int[] passengerIds = manager.getMergedPassengerIds(stand, data.getEntityId());

            SchedulerUtil.runForEntity(viewer, () -> {
                if (armorStandDisplays.get(stand.getUniqueId()) != data) return;
                UUID viewerId = viewer.getUniqueId();
                boolean wasViewer = data.getViewers().contains(viewerId);
                manager.updateBackpackViewer(viewer, data, location, vehicleId, yaw, passengerIds);
                // A fresh display has no viewers until the nearby player's update lands, so only
                // prune when this viewer was actually removed from it.
                if (wasViewer && !data.getViewers().contains(viewerId) && data.getViewers().isEmpty())
                    armorStandDisplays.remove(stand.getUniqueId(), data);
            });
        }, () -> removeArmorStandDisplay(stand.getUniqueId()));
    }

    private BackpackCosmeticManager.BackpackData ensureArmorStandDisplay(ArmorStand stand,
                                                                          BackpackCosmeticMechanic mechanic,
                                                                          ItemStack displayItem) {
        UUID standId = stand.getUniqueId();
        BackpackCosmeticManager.BackpackData data = armorStandDisplays.get(standId);
        if (data != null && data.getMechanic() == mechanic && displayItem.isSimilar(data.getDisplayItem())) {
            return data;
        }

        removeArmorStandDisplay(standId);
        data = manager.createBackpackData(stand.getWorld(), mechanic, displayItem.clone());
        armorStandDisplays.put(standId, data);
        return data;
    }

    private void removeArmorStandViewer(Player viewer, UUID standId, BackpackCosmeticManager.BackpackData data) {
        if (!data.getViewers().contains(viewer.getUniqueId())) return;
        manager.removeBackpackViewer(viewer, data);
        if (data.getViewers().isEmpty()) armorStandDisplays.remove(standId, data);
    }

    private void checkArmorStandDisplay(ArmorStand stand) {
        if (!stand.isValid()) {
            removeArmorStandDisplay(stand.getUniqueId());
            return;
        }

        ItemStack chestItem = stand.getEquipment() == null ? null : stand.getEquipment().getChestplate();
        BackpackCosmeticMechanic mechanic = getBackpackMechanic(chestItem);
        if (mechanic == null) {
            removeArmorStandDisplay(stand.getUniqueId());
            return;
        }

        ensureArmorStandDisplay(stand, mechanic, chestItem);
    }

    private void removeArmorStandDisplay(UUID standId) {
        BackpackCosmeticManager.BackpackData data = armorStandDisplays.remove(standId);
        if (data == null) return;
        manager.scheduleBackpackDestroyForViewers(data);
    }

    void cleanupArmorStandDisplays() {
        armorStandTasksClosed = true;
        for (SchedulerUtil.ScheduledTask task : armorStandViewerTasks.values()) {
            task.cancel();
        }
        armorStandViewerTasks.clear();

        armorStandDisplays.values().forEach(manager::scheduleBackpackDestroyForViewers);
        armorStandDisplays.clear();
    }
}
