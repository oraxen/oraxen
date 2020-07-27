package io.th0rgal.oraxen.utils.reflection;

import static io.th0rgal.oraxen.utils.reflection.ReflectionProvider.ORAXEN;

import java.util.Optional;

import org.bukkit.inventory.ItemStack;

import com.syntaxphoenix.syntaxapi.nbt.NbtCompound;
import com.syntaxphoenix.syntaxapi.reflection.Reflect;

public class ItemTools {

    public static Object toMinecraftCompound(ItemStack itemStack) {
        Optional<Reflect> option0 = ORAXEN.getOptionalReflect("cb_itemstack");
        Optional<Reflect> option1 = ORAXEN.getOptionalReflect("nms_itemstack");
        Optional<Reflect> option2 = ORAXEN.getOptionalReflect("nms_nbt_compound");

        if (!(option0.isPresent() || option1.isPresent() || option2.isPresent()))
            throw new IllegalStateException("Oraxen Reflections aren't setup properly?");

        Object minecraftCompound = option2.get().init();

        Object craftStack = option0.get().run("asNmsStack", itemStack);
        minecraftCompound = option1.get().run(craftStack, "save", minecraftCompound);

        return minecraftCompound;
    }

    public static ItemStack fromMinecraftCompound(Object minecraftCompound) {
        Optional<Reflect> option0 = ORAXEN.getOptionalReflect("cb_itemstack");
        Optional<Reflect> option1 = ORAXEN.getOptionalReflect("nms_itemstack");

        if (!(option0.isPresent() || option1.isPresent()))
            throw new IllegalStateException("Oraxen Reflections aren't setup properly?");

        Object minecraftStack = option1.get().run("load", minecraftCompound);
        Object itemStack = option0.get().run("fromNmsStack", minecraftStack);

        return (ItemStack) itemStack;
    }

    public static NbtCompound toNbtCompound(ItemStack itemStack) {
        return NbtTools.fromMinecraft(toMinecraftCompound(itemStack));
    }

    public static ItemStack fromNbtCompound(NbtCompound compound) {
        return fromMinecraftCompound(NbtTools.toMinecraft(compound));
    }

}
