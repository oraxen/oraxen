package io.th0rgal.oraxen.recipes.builders;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class ShapedBuilder extends WorkbenchBuilder {

    public ShapedBuilder(Player player) {
        super(player, "shaped");
    }

    @Override
    public void saveRecipe(String name) {

        Map<ItemStack, Character> letterByItem = new HashMap<>();
        char letter = 'A';
        String[] shapes = new String[3];
        StringBuilder shape = new StringBuilder();

        for (int i = 1; i < getInventory().getSize(); i++) {
            ItemStack item = getInventory().getItem(i);
            if (item == null)
                shape.append("_");
            else if (letterByItem.containsKey(item))
                shape.append(letterByItem.get(item));
            else {
                shape.append(letter);
                letterByItem.put(getInventory().getItem(i), letter);
                letter++;
            }

            if (shape.length() == 3) {
                shapes[(i + 1) / 3 - 1] = shape.toString();
                shape = new StringBuilder();
            }
        }

        ConfigurationSection newCraftSection = getConfig().createSection(name);
        newCraftSection.set("shape", shapes);
        setSerializedItem(newCraftSection.createSection("result"), getInventory().getItem(0));
        ConfigurationSection ingredients = newCraftSection.createSection("ingredients");
        for (Map.Entry<ItemStack, Character> entry : letterByItem.entrySet()) {
            ConfigurationSection ingredientSection = ingredients.createSection(String.valueOf(entry.getValue()));
            setSerializedItem(ingredientSection, entry.getKey());
        }

        saveConfig();
    }

}