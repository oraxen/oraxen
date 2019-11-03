package io.th0rgal.oraxen.utils;

import org.bukkit.inventory.ItemStack;

import java.lang.reflect.InvocationTargetException;

public class ItemUtils {

    private static final Class<?> CRAFT_ITEM_STACK = NMS.CRAFT_ITEM_STACK.toClass();
    private static final Class<?> NMS_ITEM_STACK = NMS.ITEM_STACK.toClass();
    private static final Class<?> NBT_TAG_COMPOUND = NMS.NBT_TAG_COMPOUND.toClass();

    public static Class<?> getNBTTagCompoundClass() {
        return NBT_TAG_COMPOUND;
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
            NBT_TAG_COMPOUND.getMethod("setInt", String.class, int.class).invoke(itemTag, field, value);
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
            NBT_TAG_COMPOUND.getMethod("setString", String.class, String.class).invoke(itemTag, field, value);
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            e.printStackTrace();
        }
    }

    public static void setBooleanNBTTag(Object itemTag, String field, boolean value) {
        try {
            NBT_TAG_COMPOUND.getMethod("setBoolean", String.class, boolean.class).invoke(itemTag, field, value);
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            e.printStackTrace();
        }
    }

    public static Object getNBTBase(Object itemTag, String field) {
        try {
            return NBT_TAG_COMPOUND.getMethod("get", String.class).invoke(itemTag, field);
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw (new RuntimeException());
        }
    }

    public static Object getNMSCopy(ItemStack itemStack) {
        try {
            return CRAFT_ITEM_STACK.getMethod("asNMSCopy", ItemStack.class).invoke(CRAFT_ITEM_STACK, itemStack);

        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw (new RuntimeException());
        }
    }

    public static ItemStack fromNMS(Object NMSitemStack) {
        try {
            return (ItemStack) CRAFT_ITEM_STACK.getMethod("asCraftMirror", NMS_ITEM_STACK).invoke(CRAFT_ITEM_STACK, NMSitemStack);
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw (new RuntimeException());
        }
    }

    public static Object getNBTTagCompound(Object NMSitemStack) {
        try {
            return ((boolean) NMS_ITEM_STACK.getMethod("hasTag").invoke(NMSitemStack) ?
                    NMS_ITEM_STACK.getMethod("getTag").invoke(NMSitemStack) :
                    NBT_TAG_COMPOUND.newInstance());
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException | InstantiationException e) {
            throw (new RuntimeException());
        }
    }

    public static void setNBTTagCompound(Object NMSitemStack, Object itemTag) {
        try {
            NMS_ITEM_STACK.getMethod("setTag", NBT_TAG_COMPOUND).invoke(NMSitemStack, itemTag);
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            e.printStackTrace();
        }
    }

}
