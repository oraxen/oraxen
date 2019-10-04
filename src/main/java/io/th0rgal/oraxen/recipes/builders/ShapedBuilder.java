package io.th0rgal.oraxen.recipes.builders;

import io.th0rgal.oraxen.recipes.WorkbenchBuilder;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;

public class ShapedBuilder extends WorkbenchBuilder {

    public ShapedBuilder(Player player) {
        super(player, "shaped");
    }

    @Override
    public void saveRecipe(String name) {

        ItemStack[] content = getInventory().getContents();
        ArrayList<Object> input = new ArrayList<>();
        for (int i = 1; i < content.length; i++)
            input.add(getSerializedItem(content[i]));

        ConfigurationSection newCraftSection = getConfig().createSection(name);
        newCraftSection.set("result", getSerializedItem(content[0]));
        newCraftSection.set("ingredients", input);
        saveConfig();
    }

}