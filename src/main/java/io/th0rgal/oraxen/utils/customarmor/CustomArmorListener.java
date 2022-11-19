package io.th0rgal.oraxen.utils.customarmor;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.api.OraxenItems;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.AnvilInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;

public class CustomArmorListener implements Listener {
    
    private final Map<Player, BukkitTask> taskMap = new HashMap<>();
    
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onCustomArmorRepair(InventoryOpenEvent event) {
        Player player = (Player) event.getPlayer();
        if (taskMap.containsKey(player)) return;
        BukkitTask task = Bukkit.getScheduler().runTaskTimer(OraxenPlugin.get(), () -> {
            if (!(player.getOpenInventory().getTopInventory() instanceof AnvilInventory anvilInventory)) return;
            ItemStack first = anvilInventory.getItem(0);
            ItemStack second = anvilInventory.getItem(1);
            String firstID = OraxenItems.getIdByItem(first);
            String secondID = OraxenItems.getIdByItem(second);

            if (first == null || second == null) return; // Empty slot
            if (firstID == null) return; // Not a custom item
            Material type = first.getType();
            if (type != Material.LEATHER_HELMET && type != Material.LEATHER_CHESTPLATE && type != Material.LEATHER_LEGGINGS && type != Material.LEATHER_BOOTS)
                return; // Not a custom armor

            // Set result next tick
            Bukkit.getScheduler().runTaskLater(OraxenPlugin.get(), () -> {
                if (!firstID.equals(secondID) || second.getType() == Material.LEATHER)
                    anvilInventory.setItem(2, new ItemStack(Material.AIR));
            }, 1L);
        }, 2L, 2L);
        taskMap.putIfAbsent(player, task);

    }

    @EventHandler
    public void onCloseAnvil(InventoryCloseEvent event) {
        closeTasks((Player) event.getPlayer());

    }

    private void closeTasks(Player player) {
        if (taskMap.containsKey(player)) {
            Bukkit.getScheduler().runTaskLater(OraxenPlugin.get(), () -> {
                BukkitTask task = taskMap.get(player);
                if (task != null) task.cancel();
                taskMap.remove(player);
            }, 1L);
        }
    }
}
