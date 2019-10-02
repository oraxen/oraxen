package io.th0rgal.oraxen.recipes;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.settings.ResourcesManager;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public abstract class RecipeBuilder {

    private static Map<UUID, RecipeBuilder> map = new HashMap<>();

    private Inventory inventory;
    private File configFile;
    private YamlConfiguration config;
    private final String inventoryTitle;
    private final Player player;
    private final String builderName;

    public RecipeBuilder(Player player, String builderName) {
        this.player = player;
        this.builderName = builderName;
        this.inventoryTitle = player.getName() + " " + builderName + " builder§o§r§a§x§e§n"; // watermark
        inventory = map.containsKey(player.getUniqueId())
                ? map.get(player.getUniqueId()).inventory
                : createInventory(player, inventoryTitle);
        player.openInventory(inventory);
        map.put(player.getUniqueId(), this);
    }

    abstract Inventory createInventory(Player player, String inventoryTitle);

    public abstract void saveRecipe(String name);

    protected Inventory getInventory() {
        return this.inventory;
    }

    public YamlConfiguration getConfig() {
        if (configFile == null) {
            configFile = new ResourcesManager(OraxenPlugin.get()).extractConfiguration("recipes/" + builderName + ".yml");
            config = YamlConfiguration.loadConfiguration(configFile);
        }
        return config;
    }

    public void saveConfig() {
        try {
            config.save(configFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

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
