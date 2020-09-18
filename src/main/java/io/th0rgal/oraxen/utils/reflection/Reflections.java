package io.th0rgal.oraxen.utils.reflection;

import java.io.DataInput;
import java.io.DataInputStream;
import java.io.DataOutput;

import org.bukkit.inventory.ItemStack;

public class Reflections {

    public static void setup(ReflectionProvider provider) {

        generalSetup(provider);

        /*
         * Predefine needed classes
         */

        // NMS
        Class<?> nbtTagCompoundClass = provider.createNMSReflect("nms_nbt_compound", "NBTTagCompound").getOwner();
        Class<?> nmsItemStackClass = provider
            .createNMSReflect("nms_itemstack", "ItemStack")
            .searchMethod("save", "save", nbtTagCompoundClass)
            .searchMethod("load", "a", nbtTagCompoundClass)
            .getOwner();

        /*
         * Define reflects
         */

        // NMS
        provider
            .createNMSReflect("nms_nbt_stream_tools", "NBTCompressedStreamTools")
            .searchMethod("read", "a", DataInput.class)
            .searchMethod("read", "a", DataInputStream.class)
            .searchMethod("write", "a", nbtTagCompoundClass, DataOutput.class);

        // CB
        provider
            .createCBReflect("cb_itemstack", "inventory.CraftItemStack")
            .searchMethod("asNmsStack", "asNMSCopy", ItemStack.class)
            .searchMethod("fromNmsStack", "asBukkitCopy", nmsItemStackClass);

        provider
            .createCBReflect("cb_recipeiterator", "inventory.RecipeIterator")
            .searchMethod("hasNext", "hasNext")
            .searchMethod("next", "next");

    }

    public static void generalSetup(ReflectionProvider provider) {

        provider.createReflect("java_arraylist", "java.util.ArrayList").searchField("elements", "elementData");

    }

}
