package io.th0rgal.oraxen.recipes.builders;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
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
        Map<ItemStack, Integer> items = new HashMap<>();
        ItemStack[] content = getInventory().getContents();
        for (int i = 1; i < content.length; i++)
            items.put(content[i], items.getOrDefault(content[i], 0) + 1);

        ConfigurationSection newCraftSection = getConfig().createSection(name);
        setSerializedItem(newCraftSection.createSection("result"), content[0]);
        ConfigurationSection ingredients = newCraftSection.createSection("ingredients");

        for (int i = 1; i < items.size(); i++) {
            ConfigurationSection ingredientSection = ingredients.createSection(String.valueOf((char) (64 + i)));
            ingredientSection.set("amount", items.values().toArray()[i]);
            setSerializedItem(ingredientSection, content[i]);
        }
        if (permission != null)
            newCraftSection.set("permission", permission);
        saveConfig();
    }


}