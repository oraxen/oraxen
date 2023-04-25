package io.th0rgal.oraxen.recipes.builders;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;


public class CookingBuilder extends RecipeBuilder {

    private int cookingTime;
    private int experience;
    private final InventoryType inventoryType;

    public CookingBuilder(Player player, String builderName, InventoryType inventoryType) {
        super(player, builderName);
        this.inventoryType = inventoryType;
    }

    @Override
    Inventory createInventory(Player player, String inventoryTitle) {
        return Bukkit.createInventory(player, inventoryType, inventoryTitle);
    }

    @Override
    public void saveRecipe(String name) {
        saveRecipe(name, null);
    }

    @Override
    public void saveRecipe(String name, String permission) {

        ItemStack[] content = getInventory().getContents();
        ConfigurationSection newCraftSection = getConfig().createSection(name);
        setSerializedItem(newCraftSection.createSection("result"), content[2]);
        setSerializedItem(newCraftSection.createSection("input"), content[0]);
        newCraftSection.set("cookingTime", cookingTime);
        newCraftSection.set("experience", experience);

        if (permission != null)
            newCraftSection.set("permission", permission);

        saveConfig();
        close();
    }

    public void setCookingTime(int cookingTime) {
        this.cookingTime = cookingTime;
    }

    public void setExperience(int experience) {
        this.experience = experience;
    }

}
