package io.th0rgal.oraxen.recipes.builders;

import io.th0rgal.oraxen.utils.signinput.SignMenuFactory;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class FurnaceBuilder extends RecipeBuilder {

    private SignMenuFactory.Menu cookingTimeMenu;
    private SignMenuFactory.Menu experienceMenu;

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

        if (getCookingTimeMenu() != null)
            newCraftSection.set("cookingTime", Integer.parseInt(getCookingTimeMenu().text.get(0)));
        else
            newCraftSection.set("cookingTime", 200);

        if (getExperienceMenu() != null)
            newCraftSection.set("experience", Integer.parseInt(getExperienceMenu().text.get(0)));
        else
            newCraftSection.set("experience", 200);

        if (permission != null)
            newCraftSection.set("permission", permission);

        saveConfig();
        close();
    }

    public SignMenuFactory.Menu getExperienceMenu() {
        return experienceMenu;
    }

    public void setExperienceMenu(SignMenuFactory.Menu experienceMenu) {
        this.experienceMenu = experienceMenu;
    }

    public SignMenuFactory.Menu getCookingTimeMenu() {
        return cookingTimeMenu;
    }

    public void setCookingTimeMenu(SignMenuFactory.Menu cookingTimeMenu) {
        this.cookingTimeMenu = cookingTimeMenu;
    }

}
