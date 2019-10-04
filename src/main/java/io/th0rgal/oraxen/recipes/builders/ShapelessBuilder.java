package io.th0rgal.oraxen.recipes.builders;

import io.th0rgal.oraxen.recipes.WorkbenchBuilder;

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
        for (ItemStack item : getInventory().getContents())
            items.put(item, items.getOrDefault(item, 0)+1);

        ConfigurationSection newCraftSection = getConfig().createSection(name);
        //newCraftSection.set("result", getSerializedItem(content[0]));
        //newCraftSection.set("ingredients", input);
        saveConfig();
    }

}