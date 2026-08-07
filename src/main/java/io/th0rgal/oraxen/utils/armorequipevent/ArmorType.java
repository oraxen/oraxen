package io.th0rgal.oraxen.utils.armorequipevent;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

/**
 * Represents the different types of armor.
 *
 * @author Arnah
 * @since Jul 30, 2015
 */
public enum ArmorType{
    /**
     * Represents armor belonging to the helmet slot, e.g. helmets, skulls, and carved pumpkins.
     */
    HELMET(5),
    /**
     * Represents armor belonging to the chestplate slot, e.g. chestplates and elytras.
     */
    CHESTPLATE(6),
    /**
     * Represents leggings.
     */
    LEGGINGS(7),
    /**
     * Represents boots.
     */
    BOOTS(8);

    private final int slot;

    ArmorType(int slot){
        this.slot = slot;
    }

    /**
     * Attempts to match the ArmorType for the specified ItemStack.
     *
     * @param itemStack The ItemStack to parse the type of.
     * @return The parsed ArmorType, or null if not found.
     */
    public static ArmorType matchType(final ItemStack itemStack){
        if(ArmorListener.isEmpty(itemStack)) return null;
        Material type = itemStack.getType();
        String typeName = type.name();
        if(typeName.endsWith("_HELMET") || typeName.endsWith("_SKULL") || typeName.endsWith("_HEAD") || type == Material.CARVED_PUMPKIN) return HELMET;
        else if(typeName.endsWith("_CHESTPLATE") || typeName.equals("ELYTRA")) return CHESTPLATE;
        else if(typeName.endsWith("_LEGGINGS")) return LEGGINGS;
        else if(typeName.endsWith("_BOOTS")) return BOOTS;
        else return null;
    }

    /**
     * Returns the raw slot where this piece of armor usually gets equipped
     * @return Raw slot belonging to this piece of armor
     */
    public int getSlot(){
        return slot;
    }
}
