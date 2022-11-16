package io.th0rgal.oraxen.utils.customarmor;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.api.OraxenItems;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.AnvilInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;

public class CustomArmorListener implements Listener {
    
    private final Map<Player, BukkitTask> taskMap = new HashMap<>();
    
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onCustomArmorRepair(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        if (taskMap.containsKey(player)) return;
        BukkitTask task = Bukkit.getScheduler().runTaskTimer(OraxenPlugin.get(), () -> {
            if (!(player.getOpenInventory().getTopInventory() instanceof AnvilInventory anvilInventory)) return;
            ItemStack first = event.getSlot() == 0 ? event.getCurrentItem() : anvilInventory.getItem(0);
            ItemStack second = event.getSlot() == 1 ? event.getCurrentItem() : anvilInventory.getItem(1);

            if (first == null || second == null) return;
            Material type = first.getType();
            if (type != Material.LEATHER_HELMET && type != Material.LEATHER_CHESTPLATE && type != Material.LEATHER_LEGGINGS && type != Material.LEATHER_BOOTS)
                return;

            Bukkit.getScheduler().runTaskLater(OraxenPlugin.get(), () -> {
                if (!OraxenItems.getIdByItem(first).equals(OraxenItems.getIdByItem(second)) || !OraxenItems.exists(first) || second.getType() == Material.LEATHER)
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
                taskMap.get(player).cancel();
                taskMap.remove(player);
            }, 1L);
        }
    }
}
