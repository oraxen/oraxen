package io.th0rgal.oraxen.utils.armorequipevent;

import io.th0rgal.oraxen.items.OraxenItems;
import io.th0rgal.oraxen.mechanics.provided.hat.HatMechanicFactory;
import org.bukkit.inventory.ItemStack;

/**
 * @author Arnah
 * @since Jul 30, 2015
 */
public enum ArmorType {
    HELMET(5),
    CHESTPLATE(6),
    LEGGINGS(7),
    BOOTS(8);

    private final int slot;

    ArmorType(int slot) {
        this.slot = slot;
    }

    /**
     * Attempts to match the ArmorType for the specified ItemStack.
     *
     * @param itemStack The ItemStack to parse the type of.
     * @return The parsed ArmorType, or null if not found.
     */
    public static ArmorType matchType(final ItemStack itemStack) {
        if (ArmorListener.isAirOrNull(itemStack))
            return null;
        String itemID = OraxenItems.getIdByItem(itemStack);
        if (OraxenItems.exists(itemID) && HatMechanicFactory.get() != null
            && !HatMechanicFactory.get().isNotImplementedIn(itemID))
            return HELMET;
        String type = itemStack.getType().name();
        if (type.endsWith("_HELMET") || type.endsWith("_SKULL"))
            return HELMET;
        else if (type.endsWith("_CHESTPLATE") || type.endsWith("ELYTRA"))
            return CHESTPLATE;
        else if (type.endsWith("_LEGGINGS"))
            return LEGGINGS;
        else if (type.endsWith("_BOOTS"))
            return BOOTS;
        else
            return null;
    }

    public int getSlot() {
        return slot;
    }
}
