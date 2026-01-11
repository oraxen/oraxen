package io.th0rgal.oraxen.mechanics.provided.cosmetic.backpack;

import io.th0rgal.oraxen.api.OraxenItems;
import io.th0rgal.oraxen.mechanics.Mechanic;
import io.th0rgal.oraxen.utils.SchedulerUtil;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.*;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.event.entity.EntityPickupItemEvent;

/**
 * Listener for backpack cosmetic mechanics.
 * Handles equipment changes, movement, and player lifecycle events.
 */
public class BackpackCosmeticListener implements Listener {

    private final BackpackCosmeticFactory factory;
    private final BackpackCosmeticManager manager;

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

        // Check if player has backpack item equipped
        SchedulerUtil.runTaskLater(5L, () -> checkAndUpdateBackpack(player));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        manager.hideBackpack(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerDeath(PlayerDeathEvent event) {
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

        // Debug: io.th0rgal.oraxen.utils.logs.Logs.logSuccess("[Backpack] Checking equipment for " + player.getName());

        // Check spectator mode
        if (player.getGameMode() == GameMode.SPECTATOR) {
            manager.hideBackpack(player);
            return;
        }

        PlayerInventory inv = player.getInventory();
        BackpackCosmeticMechanic foundMechanic = null;
        ItemStack foundItem = null;

        // First, check equipment slots for slot-based triggers
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            ItemStack item = inv.getItem(slot);
            if (item == null || item.getType().isAir()) continue;

            String itemId = OraxenItems.getIdByItem(item);
            if (itemId == null) continue;

            Mechanic mechanic = factory.getMechanic(itemId);
            if (mechanic instanceof BackpackCosmeticMechanic backpackMechanic) {
                // Slot-based trigger: must be in the specific slot
                if (!backpackMechanic.triggersFromInventory() && backpackMechanic.getTriggerSlot() == slot) {
                    // Debug: io.th0rgal.oraxen.utils.logs.Logs.logSuccess("[Backpack] Found backpack in trigger slot: " + slot);
                    foundMechanic = backpackMechanic;
                    foundItem = item;
                    break;
                }
            }
        }

        // If no slot-based trigger found, check inventory for inventory-based triggers
        if (foundMechanic == null) {
            // Get held item slot to exclude it (slot 40 is offhand, getHeldItemSlot is main hand)
            int heldSlot = inv.getHeldItemSlot();
            int offHandSlot = 40;

            // Check entire inventory (excluding hands by slot index)
            ItemStack[] contents = inv.getContents();
            for (int slot = 0; slot < contents.length; slot++) {
                // Skip hand slots
                if (slot == heldSlot || slot == offHandSlot) continue;

                ItemStack item = contents[slot];
                if (item == null || item.getType().isAir()) continue;

                String itemId = OraxenItems.getIdByItem(item);
                if (itemId == null) continue;

                Mechanic mechanic = factory.getMechanic(itemId);
                if (mechanic instanceof BackpackCosmeticMechanic backpackMechanic) {
                    // Inventory-based trigger: anywhere except hands
                    if (backpackMechanic.triggersFromInventory()) {
                        // Debug: io.th0rgal.oraxen.utils.logs.Logs.logSuccess("[Backpack] Found backpack in inventory (not hands)");
                        foundMechanic = backpackMechanic;
                        foundItem = item;
                        break;
                    }
                }
            }
        }

        if (foundMechanic != null) {
            // Show or update backpack
            BackpackCosmeticManager.BackpackData currentData = manager.getBackpackData(player);
            boolean needsUpdate = currentData == null ||
                currentData.getMechanic() != foundMechanic ||
                !foundItem.isSimilar(currentData.getDisplayItem());

            if (needsUpdate) {
                // Debug: io.th0rgal.oraxen.utils.logs.Logs.logSuccess("[Backpack] Showing backpack for " + player.getName());
                manager.showBackpack(player, foundMechanic, foundItem);
            }
        } else {
            // Hide backpack if no backpack item found
            manager.hideBackpack(player);
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
}
