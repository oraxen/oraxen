package io.th0rgal.oraxen.recipes.builders;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.api.OraxenItems;
import io.th0rgal.oraxen.utils.OraxenYaml;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public abstract class RecipeBuilder {

    private static final Map<UUID, RecipeBuilder> MAP = new HashMap<>();

    private Inventory inventory;
    private File configFile;
    private YamlConfiguration config;
    private final String inventoryTitle;
    private final Player player;
    private final String builderName;

    protected RecipeBuilder(Player player, String builderName) {
        this.player = player;
        this.builderName = builderName;
        this.inventoryTitle = player.getName() + " " + builderName + " builder";
        UUID playerId = player.getUniqueId();
        inventory = MAP.containsKey(playerId) && MAP.get(playerId).builderName.equals(builderName)
                ? MAP.get(playerId).inventory
                : createInventory(player, inventoryTitle);
        player.openInventory(inventory);
        MAP.put(playerId, this);
    }

    abstract Inventory createInventory(Player player, String inventoryTitle);

    void close() {
        MAP.remove(player.getUniqueId());
    }

    public abstract void saveRecipe(String name);

    public abstract void saveRecipe(String name, String permission);

    protected Inventory getInventory() {
        return this.inventory;
    }

    protected void setSerializedItem(ConfigurationSection section, @NotNull ItemStack itemStack) {

        // if our itemstack is made using oraxen and is not modified
        if (OraxenItems.exists(itemStack))
            section.set("oraxen_item", OraxenItems.getIdByItem(itemStack));
        // if our itemstack is an unmodified vanilla item outside of amount
        else if (itemStack.isSimilar(new ItemStack(itemStack.getType())))
            section.set("minecraft_type", itemStack.getType().name());
        else section.set("minecraft_item", itemStack);

        if (itemStack.getAmount() > 1) section.set("amount", itemStack.getAmount());
    }

    public YamlConfiguration getConfig() {
        if (configFile == null) {
            configFile = OraxenPlugin.get().getResourceManager()
                    .extractConfiguration("recipes/" + builderName + ".yml");
            config = OraxenYaml.loadConfiguration(configFile);
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
        MAP.put(player.getUniqueId(), this);
    }

    public String getInventoryTitle() {
        return inventoryTitle;
    }

    public Player getPlayer() {
        return player;
    }

    public void open() {
        player.openInventory(inventory);
    }

    public static RecipeBuilder get(UUID playerUUID) {
        return MAP.get(playerUUID);
    }
}
