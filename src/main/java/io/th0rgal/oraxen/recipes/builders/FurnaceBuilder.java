package io.th0rgal.oraxen.recipes.builders;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import io.th0rgal.oraxen.utils.input.InputProvider;

public class FurnaceBuilder extends RecipeBuilder {

    private String cookingTimeInput;
    private String experienceInput;

    private final InputProvider[] providers = new InputProvider[2];

    public FurnaceBuilder(Player player) {
        super(player, "furnace");
    }

    @Override
    Inventory createInventory(Player player, String inventoryTitle) {
        return Bukkit.createInventory(player, InventoryType.FURNACE, inventoryTitle);
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

        if (cookingTimeInput != null)
            newCraftSection.set("cookingTime", Integer.parseInt(cookingTimeInput));
        else
            newCraftSection.set("cookingTime", 200);

        if (experienceInput != null)
            newCraftSection.set("experience", Integer.parseInt(experienceInput));
        else
            newCraftSection.set("experience", 200);

        if (permission != null)
            newCraftSection.set("permission", permission);

        saveConfig();
        close();
    }

    public void setExperienceProvider(InputProvider experienceProvider) {
        if (providers[0] != null)
            providers[0].close();
        providers[0] = experienceProvider.onRespond((player, provider) -> {
            this.experienceInput = provider.getInput()[0];
            return true;
        });
    }

    public void setCookingTimeProvider(InputProvider cookingTimeProvider) {
        if (providers[1] != null)
            providers[1].close();
        providers[1] = cookingTimeProvider.onRespond((player, provider) -> {
            this.cookingTimeInput = provider.getInput()[0];
            return true;
        });
    }

}
