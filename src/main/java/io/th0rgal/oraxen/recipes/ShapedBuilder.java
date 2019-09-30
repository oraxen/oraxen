package io.th0rgal.oraxen.recipes;

import io.th0rgal.oraxen.items.OraxenItems;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

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
        Object[] output = new Object[content.length];
        for (int i = 0; i < content.length; i++) {
            ItemStack itemStack = content[i];

            //if our itemstack is made using oraxen and is not modified
            String itemID = OraxenItems.getIdByItem(itemStack);
            if (itemID != null && OraxenItems.getItemById(itemID).getItem().equals(itemStack)) {
                output[i] = itemID;

                //if our itemstack is an unmodified vanilla item
            } else if (itemStack.equals(new ItemStack(itemStack.getType()))) {
                output[i] = itemStack.getType();

            } else {
                output[i] = itemStack;
            }
        }

    }

}