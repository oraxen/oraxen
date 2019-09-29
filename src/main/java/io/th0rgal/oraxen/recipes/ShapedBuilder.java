package io.th0rgal.oraxen.recipes;

import io.th0rgal.oraxen.utils.Logs;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ShapedBuilder implements RecipeInterface {

    private static Map<UUID, ShapedBuilder> map = new HashMap<>();

    private Inventory inventory;
    private final String inventoryTitle;

    public ShapedBuilder(Player player) {
        this.inventoryTitle = player.getName() + " recipe builder§o§r§a§x§e§n"; // watermark
        if (map.containsKey(player.getUniqueId()))
            inventory = map.get(player.getUniqueId()).inventory;
        else {
            inventory = Bukkit.createInventory(player, InventoryType.WORKBENCH, inventoryTitle);
        }
        player.openInventory(inventory);
        map.put(player.getUniqueId(), this);
    }

    public void setInventory(UUID playerUUID, Inventory inventory) {
        this.inventory = inventory;
        map.put(playerUUID, this);
    }

    public String getInventoryTitle() {
        return inventoryTitle;
    }

    public static ShapedBuilder getShapedBuilder(UUID playerUUID) {
        return map.get(playerUUID);
    }

    public static void saveRecipe(UUID playerUUID) {
        ShapedBuilder builder = map.get(playerUUID);
        Logs.log(builder.inventory.toString());
    }

}