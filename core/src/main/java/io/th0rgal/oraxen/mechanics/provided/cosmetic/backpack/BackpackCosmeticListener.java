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

/**
 * Listener for backpack cosmetic mechanics.
 * Handles equipment changes, movement, and player lifecycle events.
 */
public class BackpackCosmeticListener implements Listener {

    private final BackpackCosmeticFactory factory;
    private final BackpackCosmeticManager manager;

    // Movement thresholds to reduce unnecessary updates
    private static final double POSITION_THRESHOLD = 0.1;
    private static final float YAW_THRESHOLD = 5.0f;

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

        // Check if clicking in player's armor slots
        if (event.getClickedInventory() instanceof PlayerInventory) {
            InventoryType.SlotType slotType = event.getSlotType();
            if (slotType == InventoryType.SlotType.ARMOR) {
                // Delay check to after the inventory update
                SchedulerUtil.runTaskLater(1L, () -> checkAndUpdateBackpack(player));
            }
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

    @EventHandler(priority = EventPriority.MONITOR)
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

    /**
     * Check player's equipment and update backpack display accordingly
     */
    private void checkAndUpdateBackpack(Player player) {
        if (!player.isOnline()) return;

        io.th0rgal.oraxen.utils.logs.Logs.logSuccess("[Backpack] Checking equipment for " + player.getName());

        // Check spectator mode
        if (player.getGameMode() == GameMode.SPECTATOR) {
            manager.hideBackpack(player);
            return;
        }

        // Check each equipment slot for backpack items
        PlayerInventory inv = player.getInventory();
        BackpackCosmeticMechanic foundMechanic = null;
        ItemStack foundItem = null;

        for (EquipmentSlot slot : EquipmentSlot.values()) {
            ItemStack item = inv.getItem(slot);
            if (item == null || item.getType().isAir()) continue;

            String itemId = OraxenItems.getIdByItem(item);
            io.th0rgal.oraxen.utils.logs.Logs.logSuccess("[Backpack] Slot " + slot + ": " + item.getType() + " (oraxen: " + itemId + ")");

            if (itemId == null) continue;

            Mechanic mechanic = factory.getMechanic(itemId);
            if (mechanic instanceof BackpackCosmeticMechanic backpackMechanic) {
                io.th0rgal.oraxen.utils.logs.Logs.logSuccess("[Backpack] Found backpack mechanic! Trigger slot: " + backpackMechanic.getTriggerSlot() + ", current slot: " + slot);
                if (backpackMechanic.getTriggerSlot() == slot) {
                    foundMechanic = backpackMechanic;
                    foundItem = item;
                    break;
                }
            }
        }

        if (foundMechanic != null) {
            // Show or update backpack
            io.th0rgal.oraxen.utils.logs.Logs.logSuccess("[Backpack] Showing backpack for " + player.getName());
            if (!manager.hasBackpack(player)) {
                manager.showBackpack(player, foundMechanic, foundItem);
            }
        } else {
            // Hide backpack if no backpack item equipped
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
