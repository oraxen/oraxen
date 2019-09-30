package io.th0rgal.oraxen.recipes;

import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public abstract class RecipeBuilder {

    private static Map<UUID, RecipeBuilder> map = new HashMap<>();

    private Inventory inventory;
    private final String inventoryTitle;
    private final Player player;

    public RecipeBuilder(Player player, String builderName) {
        this.player = player;
        this.inventoryTitle = player.getName() + " " + builderName + " builder§o§r§a§x§e§n"; // watermark
        inventory = map.containsKey(player.getUniqueId())
                ? map.get(player.getUniqueId()).inventory
                : createInventory(player, inventoryTitle);
        player.openInventory(inventory);
        map.put(player.getUniqueId(), this);
    }

    abstract Inventory createInventory(Player player, String inventoryTitle);

    public abstract void saveRecipe();

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
        map.put(this.player.getUniqueId(), this);
    }

    public String getInventoryTitle() {
        return this.inventoryTitle;
    }

    public void open() {
        player.openInventory(this.inventory);
    }

    public static RecipeBuilder get(UUID playerUUID) {
        return map.get(playerUUID);
    }
}
