package io.th0rgal.oraxen.recipes;

import io.th0rgal.oraxen.items.OraxenItems;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;

public class ShapedBuilder extends RecipeBuilder {

    public ShapedBuilder(Player player) {
        super(player, "shaped");
    }

    @Override
    Inventory createInventory(Player player, String inventoryTitle) {
        return Bukkit.createInventory(player, InventoryType.WORKBENCH, inventoryTitle);
    }

    @Override
    public void saveRecipe(String name) {

        ItemStack[] content = getInventory().getContents();
        ArrayList<Object> input = new ArrayList<>();
        for (int i = 1; i < content.length; i++)
            input.add(getSerializedItem(content[i]));

        ConfigurationSection newCraftSection = getConfig().createSection(name);
        newCraftSection.set("output", getSerializedItem(content[0]));
        newCraftSection.set("input", input);
        saveConfig();
    }

    private Object getSerializedItem(ItemStack itemStack) {

        String itemID = OraxenItems.getIdByItem(itemStack);

        //if our slot is empty
        if (itemStack == null)
            return Material.AIR.toString();

        //if our itemstack is made using oraxen and is not modified
        if (itemID != null && OraxenItems.getItemById(itemID).getItem().equals(itemStack))
            return itemID;

        //if our itemstack is an unmodified vanilla item
        if (itemStack != null && itemStack.equals(new ItemStack(itemStack.getType())))
            return itemStack.getType().toString();

        return itemStack;

    }


}