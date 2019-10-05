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

        Map<ItemStack, Integer> items = new HashMap<>();
        ItemStack[] content = getInventory().getContents();
        for (int i = 1; i < content.length; i++)
            items.put(content[i], items.getOrDefault(content[i], 0) + 1);

        ConfigurationSection newCraftSection = getConfig().createSection(name);
        newCraftSection.set("result", getSerializedItem(content[0]));

        ConfigurationSection ingredients = newCraftSection.createSection("ingredients");
        for (int i = 1; i < items.keySet().size(); i++) {
            items.get(items.keySet().toArray()[i]);
            ingredients.set(String.valueOf((char) 64 + i), getSerializedItem(content[i]));
        }

        saveConfig();
    }


}