package io.th0rgal.oraxen.mechanics.provided.cosmetic.backpack;

import io.th0rgal.oraxen.api.OraxenItems;
import io.th0rgal.oraxen.nms.NMSHandlers;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.util.Iterator;
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

        // Create backpack data with initial yaw locked to body yaw
        float initialYaw = player.getBodyYaw();
        BackpackData data = new BackpackData(entityId, mechanic, displayItem, initialYaw, player.getLocation());
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
            destroyBackpackForViewers(player, data);
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

            // Also ensure owner can see their own backpack if enabled
            if (mechanic.isVisibleToSelf() && !data.getViewers().contains(owner.getUniqueId())) {
                spawnBackpackForViewer(owner, owner, data);
                data.addViewer(owner.getUniqueId());
            }

            // Find new viewers
            for (Player potentialViewer : owner.getWorld().getPlayers()) {
                if (potentialViewer.equals(owner)) continue;

                boolean inRange = isWithinViewDistance(potentialViewer, owner, viewDistance);
                boolean isViewer = data.getViewers().contains(potentialViewer.getUniqueId());

                if (inRange && !isViewer) {
                    spawnBackpackForViewer(potentialViewer, owner, data);
                    data.addViewer(potentialViewer.getUniqueId());
                } else if (!inRange && isViewer) {
                    data.getViewers().remove(potentialViewer.getUniqueId());
                    NMSHandlers.getHandler().sendEntityDestroy(potentialViewer, data.getEntityId());
                }
            }

            // Remove offline viewers
            data.getViewers().removeIf(viewerId -> {
                Player viewer = Bukkit.getPlayer(viewerId);
                return viewer == null || !viewer.isOnline();
            });
        }
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
        Location spawnLoc = owner.getLocation().clone();

        // Spawn invisible armor stand - use small=true to lower the display by ~1 block
        // since mounting positions the entity at the player's head level
        NMSHandlers.getHandler().spawnBackpackArmorStand(
            viewer,
            data.getEntityId(),
            spawnLoc,
            data.getDisplayItem(),
            true  // Force small armor stand to lower display position
        );

        // Mount the armor stand to the player for instant position following
        NMSHandlers.getHandler().sendMountPacket(viewer, owner.getEntityId(), data.getEntityId());

        // Send initial head rotation to face the right direction
        NMSHandlers.getHandler().sendEntityHeadRotation(viewer, data.getEntityId(), owner.getBodyYaw());

        io.th0rgal.oraxen.utils.logs.Logs.logSuccess("[Backpack] Spawned and mounted armor stand for " + owner.getName());
    }

    private void destroyBackpackForViewers(Player owner, BackpackData data) {
        for (UUID viewerId : data.getViewers()) {
            Player viewer = Bukkit.getPlayer(viewerId);
            if (viewer != null && viewer.isOnline()) {
                NMSHandlers.getHandler().sendEntityDestroy(viewer, data.getEntityId());
            }
        }
        data.getViewers().clear();
    }

    private Location calculateBackpackLocation(Player player, BackpackData data) {
        Location loc = player.getLocation().clone();
        BackpackCosmeticMechanic mechanic = data.getMechanic();

        // Use body yaw directly - in Minecraft, body yaw only changes when player
        // actually moves/turns their body, not when just looking around with head.
        // This provides natural backpack behavior without complex tracking.
        float bodyYaw = player.getBodyYaw();
        double yawRad = Math.toRadians(bodyYaw);

        double offsetX = mechanic.getOffsetX();
        double offsetZ = mechanic.getOffsetZ();

        // Rotate offset based on body direction
        double rotatedX = offsetX * Math.cos(yawRad) - offsetZ * Math.sin(yawRad);
        double rotatedZ = offsetX * Math.sin(yawRad) + offsetZ * Math.cos(yawRad);

        loc.add(rotatedX, mechanic.getOffsetY(), rotatedZ);

        // Set yaw to body yaw and pitch to 0
        loc.setYaw(bodyYaw);
        loc.setPitch(0);

        return loc;
    }

    private boolean isWithinViewDistance(Player viewer, Player target, int viewDistance) {
        if (!viewer.getWorld().equals(target.getWorld())) return false;
        return viewer.getLocation().distanceSquared(target.getLocation()) <= viewDistance * viewDistance;
    }

    /**
     * Clean up when plugin disables
     */
    public void cleanup() {
        for (Map.Entry<UUID, BackpackData> entry : activeBackpacks.entrySet()) {
            Player owner = Bukkit.getPlayer(entry.getKey());
            if (owner != null) {
                destroyBackpackForViewers(owner, entry.getValue());
            }
        }
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
        private float lockedYaw;
        private Location lastPosition;

        public BackpackData(int entityId, BackpackCosmeticMechanic mechanic, ItemStack displayItem, float initialYaw, Location initialPosition) {
            this.entityId = entityId;
            this.mechanic = mechanic;
            this.displayItem = displayItem;
            this.lockedYaw = initialYaw;
            this.lastPosition = initialPosition.clone();
        }

        public float getLockedYaw() {
            return lockedYaw;
        }

        public void setLockedYaw(float yaw) {
            this.lockedYaw = yaw;
        }

        public Location getLastPosition() {
            return lastPosition;
        }

        public void setLastPosition(Location pos) {
            this.lastPosition = pos.clone();
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
    }
}
