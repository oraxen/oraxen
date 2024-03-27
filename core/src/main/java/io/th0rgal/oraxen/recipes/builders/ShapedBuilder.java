package io.th0rgal.oraxen.recipes.builders;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;

public class ShapedBuilder extends WorkbenchBuilder {

    public ShapedBuilder(Player player) {
        super(player, "shaped");
    }

    @Override
    public void saveRecipe(String name) {
        saveRecipe(name, null);
    }

    @Override
    public void saveRecipe(String name, String permission) {

        Map<ItemStack, Character> letterByItem = new HashMap<>();
        char letter = 'A';
        String[] shapes = new String[3];
        StringBuilder shape = new StringBuilder();
        Inventory inventory = getInventory();

        for (int i = 1; i < inventory.getSize(); i++) {
            ItemStack item = inventory.getItem(i);
            if (item == null)
                shape.append("_");
            else if (letterByItem.containsKey(item))
                shape.append(letterByItem.get(item));
            else {
                shape.append(letter);
                letterByItem.put(inventory.getItem(i), letter);
                letter++;
            }

            if (shape.length() == 3) {
                shapes[(i + 1) / 3 - 1] = shape.toString();
                shape = new StringBuilder();
            }
        }

        ConfigurationSection newCraftSection;
        ConfigurationSection resultSection;
        ConfigurationSection ingredients;
        if (getConfig().isConfigurationSection(name)) {
            newCraftSection = getConfig().getConfigurationSection(name);
            resultSection = newCraftSection.getConfigurationSection("result");
            ingredients = newCraftSection.getConfigurationSection("ingredients");
        } else {
            newCraftSection = getConfig().createSection(name);
            resultSection = newCraftSection.createSection("result");
            ingredients = newCraftSection.createSection("ingredients");
        }
        newCraftSection.set("shape", shapes);
        setSerializedItem(resultSection, inventory.getItem(0));
        for (Map.Entry<ItemStack, Character> entry : letterByItem.entrySet()) {
            ConfigurationSection ingredientSection = ingredients.createSection(String.valueOf(entry.getValue()));
            setSerializedItem(ingredientSection, entry.getKey());
        }
        if (permission != null && !permission.isEmpty())
            newCraftSection.set("permission", permission);
        saveConfig();
        close();
    }

}
