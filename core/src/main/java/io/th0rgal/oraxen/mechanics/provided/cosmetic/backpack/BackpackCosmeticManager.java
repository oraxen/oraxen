package io.th0rgal.oraxen.mechanics.provided.cosmetic.backpack;

import io.th0rgal.oraxen.nms.NMSHandlers;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages backpack cosmetic entities for all players.
 * Each player can have one active backpack displayed via packets.
 */
public class BackpackCosmeticManager {

    private static final BackpackCosmeticManager INSTANCE = new BackpackCosmeticManager();

    // Map of player UUID -> their backpack data
    private final Map<UUID, BackpackData> activeBackpacks = new ConcurrentHashMap<>();

    private BackpackCosmeticManager() {}

    public static BackpackCosmeticManager getInstance() {
        return INSTANCE;
    }

    /**
     * Show a backpack on a player
     */
    public void showBackpack(Player player, BackpackCosmeticMechanic mechanic, ItemStack displayItem) {
        UUID playerId = player.getUniqueId();

        // Remove existing backpack first
        hideBackpack(player);

        // Generate a unique entity ID for the armor stand
        int entityId = NMSHandlers.getHandler().getNextEntityId();

        BackpackData data = new BackpackData(entityId, mechanic, displayItem);
        activeBackpacks.put(playerId, data);

        // Spawn the backpack for all nearby players
        spawnBackpackForViewers(player, data);
    }

    /**
     * Hide a player's backpack
     */
    public void hideBackpack(Player player) {
        UUID playerId = player.getUniqueId();
        BackpackData data = activeBackpacks.remove(playerId);

        if (data != null) {
            // Destroy the entity for all viewers
            destroyBackpackForViewers(data);
        }
    }

    /**
     * Check if a player has an active backpack
     */
    public boolean hasBackpack(Player player) {
        return activeBackpacks.containsKey(player.getUniqueId());
    }

    /**
     * Get a player's backpack data
     */
    public BackpackData getBackpackData(Player player) {
        return activeBackpacks.get(player.getUniqueId());
    }

    /**
     * Update backpack rotation when player moves.
     * Position is handled automatically by mounting.
     */
    public void updateBackpackPosition(Player player) {
        BackpackData data = activeBackpacks.get(player.getUniqueId());
        if (data == null) return;

        // Only need to update rotation - mounting handles position automatically
        float bodyYaw = player.getBodyYaw();
        for (UUID viewerId : data.getViewers()) {
            Player viewer = Bukkit.getPlayer(viewerId);
            if (viewer != null && viewer.isOnline()) {
                NMSHandlers.getHandler().sendEntityHeadRotation(viewer, data.getEntityId(), bodyYaw);
            }
        }
    }

    /**
     * Update all backpack rotations (called by tick task).
     * Position is handled automatically by mounting.
     */
    public void updateAllBackpackPositions() {
        for (Map.Entry<UUID, BackpackData> entry : activeBackpacks.entrySet()) {
            Player owner = Bukkit.getPlayer(entry.getKey());
            if (owner == null || !owner.isOnline()) continue;

            BackpackData data = entry.getValue();
            float bodyYaw = owner.getBodyYaw();

            // Only update rotation - mounting handles position automatically
            for (UUID viewerId : data.getViewers()) {
                Player viewer = Bukkit.getPlayer(viewerId);
                if (viewer != null && viewer.isOnline()) {
                    NMSHandlers.getHandler().sendEntityHeadRotation(viewer, data.getEntityId(), bodyYaw);
                }
            }
        }
    }

    /**
     * Re-send the backpack mount packet using the owner's current passengers.
     * This keeps the fake backpack passenger from replacing real passengers added by
     * other plugins, such as sitting/riding plugins.
     */
    public void resyncBackpackMount(Player owner) {
        BackpackData data = activeBackpacks.get(owner.getUniqueId());
        if (data == null) return;

        for (UUID viewerId : data.getViewers()) {
            Player viewer = Bukkit.getPlayer(viewerId);
            if (viewer != null && viewer.isOnline()) {
                sendBackpackMountPacket(viewer, owner, data);
            }
        }
    }

    /**
     * Handle player entering view range of another player
     */
    public void onPlayerEnterViewRange(Player viewer, Player backpackOwner) {
        BackpackData data = activeBackpacks.get(backpackOwner.getUniqueId());
        if (data == null) return;

        // Check if already viewing
        if (data.getViewers().contains(viewer.getUniqueId())) return;

        // Check view distance
        if (!isWithinViewDistance(viewer, backpackOwner, data.getMechanic().getViewDistance())) return;

        // Spawn backpack for this viewer
        spawnBackpackForViewer(viewer, backpackOwner, data);
        data.addViewer(viewer.getUniqueId());
    }

    /**
     * Handle player leaving view range
     */
    public void onPlayerLeaveViewRange(Player viewer, Player backpackOwner) {
        BackpackData data = activeBackpacks.get(backpackOwner.getUniqueId());
        if (data == null) return;

        if (data.getViewers().remove(viewer.getUniqueId())) {
            NMSHandlers.getHandler().sendEntityDestroy(viewer, data.getEntityId());
        }
    }

    /**
     * Refresh viewers for all backpacks (called periodically)
     */
    public void refreshAllViewers() {
        Iterator<Map.Entry<UUID, BackpackData>> iterator = activeBackpacks.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, BackpackData> entry = iterator.next();
            Player owner = Bukkit.getPlayer(entry.getKey());
            if (owner == null || !owner.isOnline()) {
                iterator.remove();
                continue;
            }

            BackpackData data = entry.getValue();
            BackpackCosmeticMechanic mechanic = data.getMechanic();
            int viewDistance = mechanic.getViewDistance();
            boolean viewersChanged = false;

            // Also ensure owner can see their own backpack if enabled
            if (mechanic.isVisibleToSelf() && !data.getViewers().contains(owner.getUniqueId())) {
                spawnBackpackForViewer(owner, owner, data);
                data.addViewer(owner.getUniqueId());
                viewersChanged = true;
            }

            // Find new viewers
            for (Player potentialViewer : owner.getWorld().getPlayers()) {
                if (potentialViewer.equals(owner)) continue;

                boolean inRange = isWithinViewDistance(potentialViewer, owner, viewDistance);
                boolean isViewer = data.getViewers().contains(potentialViewer.getUniqueId());

                if (inRange && !isViewer) {
                    spawnBackpackForViewer(potentialViewer, owner, data);
                    data.addViewer(potentialViewer.getUniqueId());
                    viewersChanged = true;
                } else if (!inRange && isViewer) {
                    data.getViewers().remove(potentialViewer.getUniqueId());
                    NMSHandlers.getHandler().sendEntityDestroy(potentialViewer, data.getEntityId());
                    viewersChanged = true;
                }
            }

            // Remove offline viewers
            viewersChanged |= data.getViewers().removeIf(viewerId -> {
                Player viewer = Bukkit.getPlayer(viewerId);
                return viewer == null || !viewer.isOnline();
            });

            // Only resync mount packets when the viewer set or passenger list actually changed;
            // periodic resyncs every tick are an O(N*M) packet storm.
            int[] currentPassengers = getMergedPassengerIds(owner, data.getEntityId());
            if (viewersChanged || data.consumeNeedsResync()
                    || !Arrays.equals(currentPassengers, data.getLastSyncedPassengers())) {
                resyncBackpackMount(owner);
                data.setLastSyncedPassengers(currentPassengers);
            }
        }
    }

    /**
     * Force a resync of the mount packets on the next refresh tick.
     * Called from passenger mount/dismount events.
     */
    public void requestResync(Player owner) {
        BackpackData data = activeBackpacks.get(owner.getUniqueId());
        if (data != null) data.markNeedsResync();
    }

    private void spawnBackpackForViewers(Player owner, BackpackData data) {
        BackpackCosmeticMechanic mechanic = data.getMechanic();

        // Also spawn for the owner themselves so they can see their own backpack
        if (mechanic.isVisibleToSelf()) {
            spawnBackpackForViewer(owner, owner, data);
            data.addViewer(owner.getUniqueId());
        }

        for (Player viewer : owner.getWorld().getPlayers()) {
            if (viewer.equals(owner)) continue;

            if (isWithinViewDistance(viewer, owner, mechanic.getViewDistance())) {
                spawnBackpackForViewer(viewer, owner, data);
                data.addViewer(viewer.getUniqueId());
            }
        }
    }

    private void spawnBackpackForViewer(Player viewer, Player owner, BackpackData data) {
        BackpackCosmeticMechanic mechanic = data.getMechanic();
        Location spawnLoc = owner.getLocation().clone();

        // Spawn invisible armor stand using mechanic's small configuration
        NMSHandlers.getHandler().spawnBackpackArmorStand(
            viewer,
            data.getEntityId(),
            spawnLoc,
            data.getDisplayItem(),
            mechanic.isSmallArmorStand()
        );

        // Mount the armor stand to the player for instant position following.
        sendBackpackMountPacket(viewer, owner, data);

        // Send initial head rotation to face the right direction
        NMSHandlers.getHandler().sendEntityHeadRotation(viewer, data.getEntityId(), owner.getBodyYaw());
    }

    private void sendBackpackMountPacket(Player viewer, Player owner, BackpackData data) {
        int[] passengerIds = getMergedPassengerIds(owner, data.getEntityId());
        sendBackpackMountPacket(viewer, owner, passengerIds);
    }

    private void sendBackpackMountPacket(Player viewer, Player owner, int[] passengerIds) {
        NMSHandlers.getHandler().sendMountPacket(viewer, owner.getEntityId(), passengerIds);
    }

    private int[] getMergedPassengerIds(Player owner, int backpackEntityId) {
        Set<Integer> passengerIds = new LinkedHashSet<>();
        for (Entity passenger : owner.getPassengers()) {
            passengerIds.add(passenger.getEntityId());
        }
        passengerIds.add(backpackEntityId);

        return passengerIds.stream().mapToInt(Integer::intValue).toArray();
    }

    private void destroyBackpackForViewers(BackpackData data) {
        for (UUID viewerId : data.getViewers()) {
            Player viewer = Bukkit.getPlayer(viewerId);
            if (viewer != null && viewer.isOnline()) {
                NMSHandlers.getHandler().sendEntityDestroy(viewer, data.getEntityId());
            }
        }
        data.getViewers().clear();
    }

    private boolean isWithinViewDistance(Player viewer, Player target, int viewDistance) {
        if (!viewer.getWorld().equals(target.getWorld())) return false;
        return viewer.getLocation().distanceSquared(target.getLocation()) <= viewDistance * viewDistance;
    }

    /**
     * Clean up when plugin disables
     */
    public void cleanup() {
        activeBackpacks.values().forEach(this::destroyBackpackForViewers);
        activeBackpacks.clear();
    }

    /**
     * Data class for tracking a player's backpack
     */
    public static class BackpackData {
        private final int entityId;
        private final BackpackCosmeticMechanic mechanic;
        private final ItemStack displayItem;
        private final Set<UUID> viewers = ConcurrentHashMap.newKeySet();
        private volatile boolean needsResync;
        private volatile int[] lastSyncedPassengers = new int[0];

        public BackpackData(int entityId, BackpackCosmeticMechanic mechanic, ItemStack displayItem) {
            this.entityId = entityId;
            this.mechanic = mechanic;
            this.displayItem = displayItem;
        }

        public int getEntityId() {
            return entityId;
        }

        public BackpackCosmeticMechanic getMechanic() {
            return mechanic;
        }

        public ItemStack getDisplayItem() {
            return displayItem;
        }

        public Set<UUID> getViewers() {
            return viewers;
        }

        public void addViewer(UUID viewerId) {
            viewers.add(viewerId);
        }

        void markNeedsResync() {
            this.needsResync = true;
        }

        boolean consumeNeedsResync() {
            if (!needsResync) return false;
            needsResync = false;
            return true;
        }

        int[] getLastSyncedPassengers() {
            return lastSyncedPassengers;
        }

        void setLastSyncedPassengers(int[] passengers) {
            this.lastSyncedPassengers = passengers;
        }
    }
}
