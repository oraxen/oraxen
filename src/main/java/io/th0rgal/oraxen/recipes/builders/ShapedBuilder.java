package io.th0rgal.oraxen.recipes.builders;

import io.th0rgal.oraxen.utils.Logs;

import org.bukkit.entity.Player;

public class ShapedBuilder extends WorkbenchBuilder {

    public ShapedBuilder(Player player) {
        super(player, "shaped");
    }

    @Override
    public void saveRecipe(String name) {

        for (int i = 0; i < getInventory().getSize(); i++)
            Logs.log("item" + getInventory().getItem(i));

    }

}