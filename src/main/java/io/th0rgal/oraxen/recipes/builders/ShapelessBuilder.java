package io.th0rgal.oraxen.recipes.builders;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ShapelessBuilder extends WorkbenchBuilder {

    public ShapelessBuilder(Player player) {
        super(player, "shapeless");
    }

    @Override
    public void saveRecipe(String name) {
        saveRecipe(name, null);
    }

    @Override
    public void saveRecipe(String name, String permission) {
        ItemStack[] content = getInventory().getContents();

        ConfigurationSection newCraftSection = getConfig().createSection(name);
        setSerializedItem(newCraftSection.createSection("result"), content[0]);
        List<Map<String, Object>> ingredients = new ArrayList<>();

        for (int i = 1; i < content.length; i++) {
            ItemStack ingredient = content[i];
            if (ingredient == null || ingredient.isEmpty()) continue;
            ConfigurationSection ingredientSection = new MemoryConfiguration();
            setSerializedItem(ingredientSection, ingredient);
            ingredients.add(ingredientSection.getValues(false));
        }

        newCraftSection.set("ingredients", ingredients);

        if (permission != null && !permission.isEmpty())
            newCraftSection.set("permission", permission);
        saveConfig();
        close();
    }

}
