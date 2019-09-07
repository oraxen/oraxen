package io.th0rgal.oraxen.items;

import io.th0rgal.oraxen.utils.NMS;
import org.bukkit.inventory.ItemStack;

import java.lang.reflect.InvocationTargetException;

public class ItemUtils {

    private static Class<?> CraftItemStack = NMS.CRAFT_ITEM_STACK.toClass();
    private static Class<?> NMSItemStack = NMS.ITEM_STACK.toClass();
    private static Class<?> NBTTagCompound = NMS.NBT_TAG_COMPOUND.toClass();

    public static Class<?> getNBTTagCompoundClass() {
        return NBTTagCompound;
    }

    public static Object getFieldContent(ItemStack itemStack, String field) {
        return getNBTBase(getNBTTagCompound(getNMSCopy(itemStack)), field);
    }

    public static byte getNBTBaseTypeID(Object NBTBase) {
        try {
            return (byte) NMS.NBT_BASE.toClass().getMethod("getTypeId").invoke(NBTBase);
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw (new RuntimeException());
        }
    }

    public static int getIntField(ItemStack itemStack, String field) {
        Object NBTBase = getFieldContent(itemStack, field);
        try {
            return (int) NMS.NBT_TAG_INT.toClass().getMethod("asInt").invoke(NBTBase);
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw (new RuntimeException());
        }
    }

    public static void setIntNBTTag(Object itemTag, String field, int value) {
        try {
            NBTTagCompound.getMethod("setInt", String.class, int.class).invoke(itemTag, field, value);
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            e.printStackTrace();
        }
    }

    public static String getStringField(ItemStack itemStack, String field) {
        Object NBTBase = getFieldContent(itemStack, field);
        if (NBTBase != null && getNBTBaseTypeID(NBTBase) == 8)
            try {
                return (String) NMS.NBT_TAG_STRING.toClass().getMethod("asString").invoke(NBTBase);
            } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                throw (new RuntimeException());
            }
        else return null;
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
