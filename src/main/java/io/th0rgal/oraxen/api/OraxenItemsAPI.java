package io.th0rgal.oraxen.api;

import io.th0rgal.oraxen.items.OraxenItems;
import org.bukkit.inventory.ItemStack;

/**
 * Class for Oraxen custom items
 */
public class OraxenItemsAPI {
    /**
     * Return Oraxen item ID of the provided {@link ItemStack}.
     * <br> May return null if the provided {@link ItemStack} is not an Oraxen item.
     *
     * @param itemStack {@link ItemStack} to get the ID
     * @return Possibly-null Oraxen item ID
     */
    public static String getOraxenItemID(ItemStack itemStack) {
        String itemID = OraxenItems.getIdByItem(itemStack);
        if (itemID == null) return null;
        return itemID;
    }

    /**
     * Return ItemStack of the provided Oraxen item ID.
     *
     * @param itemID Oraxen item ID
     * @return {@link ItemStack}
     */
    public static ItemStack getOraxenItemStack(String itemID) {
        return OraxenItems.getItemById(itemID).build();
    }
}
