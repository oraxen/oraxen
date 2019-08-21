package io.th0rgal.oraxen.items;

import io.th0rgal.oraxen.utils.NMS;
import org.bukkit.inventory.ItemStack;

import java.lang.reflect.InvocationTargetException;

public class ItemUtils {

    private static Class<?> CraftItemStack = NMS.CRAFT_ITEM_STACK.toClass();
    private static Class<?> NMSItemStack = NMS.ITEM_STACK.toClass();
    private static Class<?> NBTTagCompound = NMS.NBT_TAG_COMPOUND.toClass();

    public static String getStringField(ItemStack itemStack, String field) {
        Object result = getFieldContent(itemStack, field);
        if (result == null)
            return null;
        else
            return result.toString();
    }

    public static Object getFieldContent(ItemStack itemStack, String field) {
        return getNBTBase(getNBTTagCompound(getNMSCopy(itemStack)), field);
    }

    public static void setIntNBTTag(Object itemTag, String field, int value) {
        try {
            NBTTagCompound.getMethod("setInt", String.class, int.class).invoke(itemTag, field, value);
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            e.printStackTrace();
        }
    }

    public static void setStringNBTTag(Object itemTag, String field, String value) {
        try {
            NBTTagCompound.getMethod("setString", String.class, String.class).invoke(itemTag, field, value);
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            e.printStackTrace();
        }
    }

    public static void setBooleanNBTTag(Object itemTag, String field, boolean value) {
        try {
            NBTTagCompound.getMethod("setBoolean", String.class, boolean.class).invoke(itemTag, field, value);
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            e.printStackTrace();
        }
    }

    public static Object getNBTBase(Object itemTag, String field) {
        try {
            return NBTTagCompound.getMethod("get", String.class).invoke(itemTag, field);
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw (new RuntimeException());
        }
    }

    public static Object getNMSCopy(ItemStack itemStack) {
        try {
            return CraftItemStack.getMethod("asNMSCopy", ItemStack.class).invoke(CraftItemStack, itemStack);

        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw (new RuntimeException());
        }
    }

    public static ItemStack fromNMS(Object NMSitemStack) {
        try {
            return (ItemStack) CraftItemStack.getMethod("asCraftMirror", NMSItemStack).invoke(CraftItemStack, NMSitemStack);
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw (new RuntimeException());
        }
    }

    public static Object getNBTTagCompound(Object NMSitemStack) {
        try {
            return ((boolean) NMSItemStack.getMethod("hasTag").invoke(NMSitemStack) ?
                    NMSItemStack.getMethod("getTag").invoke(NMSitemStack) :
                    NBTTagCompound.newInstance());
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException | InstantiationException e) {
            throw (new RuntimeException());
        }
    }

    public static void setNBTTagCompound(Object NMSitemStack, Object itemTag) {
        try {
            NMSItemStack.getMethod("setTag", NBTTagCompound).invoke(NMSitemStack, itemTag);
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            e.printStackTrace();
        }
    }

}
