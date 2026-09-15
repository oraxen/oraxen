package io.th0rgal.oraxen.utils;

import io.th0rgal.oraxen.utils.drops.Drop;
import net.kyori.adventure.text.Component;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.*;
import org.bukkit.inventory.meta.components.FoodComponent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.function.Consumer;

public class ItemUtils {

    private static final class CustomNameMethods {
        private static final Method HAS_CUSTOM_NAME = findItemMetaMethod("hasCustomName");
        private static final Method CUSTOM_NAME = findItemMetaMethod("customName");
        private static final Method SET_CUSTOM_NAME = findItemMetaMethod("customName", Component.class);

        private static Method findItemMetaMethod(String name, Class<?>... parameterTypes) {
            try {
                return ItemMeta.class.getMethod(name, parameterTypes);
            } catch (NoSuchMethodException exception) {
                return null;
            }
        }
    }

    public static boolean isEmpty(ItemStack itemStack) {
        return itemStack == null || itemStack.getType() == Material.AIR || itemStack.getAmount() == 0;
    }

    /**
     * Returns whether the player holds an item in either hand. Vanilla uses this to decide
     * whether sneaking bypasses block interactions (containers still open when sneaking with
     * empty hands).
     */
    public static boolean hasItemInAnyHand(Player player) {
        return !isEmpty(player.getInventory().getItemInMainHand())
                || !isEmpty(player.getInventory().getItemInOffHand());
    }

    public static void subtract(ItemStack itemStack, int amount) {
        itemStack.setAmount(Math.max(0, itemStack.getAmount() - amount));
    }

    public static void dyeItem(ItemStack itemStack, Color color) {
        editItemMeta(itemStack, meta -> {
            if (meta instanceof LeatherArmorMeta leatherArmorMeta) {
                leatherArmorMeta.setColor(color);
            } else if (meta instanceof PotionMeta potionMeta) {
                potionMeta.setColor(color);
            } else if (meta instanceof MapMeta mapMeta) {
                mapMeta.setColor(color);
            }
        });
    }

    /**
     * @param itemStack The ItemStack to edit the ItemMeta of
     * @param function  The function-block to edit the ItemMeta in
     * @return The original ItemStack with the new ItemMeta
     */
    public static void editItemMeta(ItemStack itemStack, Consumer<ItemMeta> function) {
        ItemMeta meta = itemStack.getItemMeta();
        if (meta == null)
            return;
        function.accept(meta);
        itemStack.setItemMeta(meta);
    }

    /**
     * Uses Paper's custom-name component on 1.21.4+ and the legacy display-name
     * component API on older supported runtimes.
     */
    public static boolean hasDisplayName(ItemMeta itemMeta) {
        if (VersionUtil.atOrAbove("1.21.4"))
            return (boolean) invoke(CustomNameMethods.HAS_CUSTOM_NAME, itemMeta);
        return itemMeta.hasDisplayName();
    }

    public static @Nullable Component getDisplayName(ItemMeta itemMeta) {
        if (VersionUtil.atOrAbove("1.21.4"))
            return (Component) invoke(CustomNameMethods.CUSTOM_NAME, itemMeta);
        return itemMeta.displayName();
    }

    public static void setDisplayName(ItemMeta itemMeta, @Nullable Component displayName) {
        if (VersionUtil.atOrAbove("1.21.4")) {
            invoke(CustomNameMethods.SET_CUSTOM_NAME, itemMeta, displayName);
            return;
        }
        itemMeta.displayName(displayName);
    }

    private static Object invoke(Method method, ItemMeta itemMeta, Object... arguments) {
        if (method == null)
            throw new IllegalStateException("The ItemMeta custom-name API is unavailable on this runtime");

        try {
            return method.invoke(itemMeta, arguments);
        } catch (IllegalAccessException exception) {
            throw new IllegalStateException("Unable to access the ItemMeta custom-name API", exception);
        } catch (InvocationTargetException exception) {
            Throwable cause = exception.getCause();
            if (cause instanceof RuntimeException runtimeException)
                throw runtimeException;
            if (cause instanceof Error error)
                throw error;
            throw new IllegalStateException("The ItemMeta custom-name API failed", cause);
        }
    }

    /**
     * Used to correctly damage the item in the player's hand based on broken block
     * Only handles it if the block is a OraxenBlock or OraxenFurniture
     *
     * @param player    the player that broke the OraxenBlock or OraxenFurniture
     * @param drop      the Drop that will be dropped
     * @param itemStack the item in the player's hand
     * @return the itemStack with the correct damage applied
     */
    public static void damageItem(Player player, Drop drop, ItemStack itemStack) {

        // If all are null this is not something Oraxen should handle
        // If the block/furniture has no drop, it returns Drop.emptyDrop() which is
        // handled by the caller
        if (drop == null)
            return;

        int damage;
        boolean isToolEnough = drop.isToolEnough(itemStack);
        damage = isToolEnough ? 1 : 2;
        // If the item is not a tool, it will not be damaged, example flint&steel should
        // not be damaged
        damage = isTool(itemStack) ? damage : 0;

        if (damage == 0)
            return;
        player.damageItemStack(itemStack, damage);
    }

    public static boolean isTool(@NotNull ItemStack itemStack) {
        return isTool(itemStack.getType());
    }

    public static boolean isTool(@NotNull Material material) {
        if (!VersionUtil.atOrAbove("1.20.5"))
            return Tag.ITEMS_TOOLS.isTagged(material);
        else
            return material.toString().endsWith("_AXE")
                    || material.toString().endsWith("_PICKAXE")
                    || material.toString().endsWith("_SHOVEL")
                    || material.toString().endsWith("_HOE")
                    || material.toString().endsWith("_SWORD")
                    || material == Material.TRIDENT;
    }

    public static boolean isSkull(Material material) {
        return switch (material) {
            case PLAYER_HEAD, PLAYER_WALL_HEAD, SKELETON_SKULL, SKELETON_WALL_SKULL, WITHER_SKELETON_SKULL,
                    WITHER_SKELETON_WALL_SKULL, ZOMBIE_HEAD, ZOMBIE_WALL_HEAD, CREEPER_HEAD, CREEPER_WALL_HEAD,
                    DRAGON_HEAD, DRAGON_WALL_HEAD, PIGLIN_HEAD, PIGLIN_WALL_HEAD ->
                true;
            default -> false;
        };
    }

    public static boolean hasInventoryParent(Material material) {
        return Tag.WALLS.isTagged(material) || Tag.FENCES.isTagged(material) || Tag.BUTTONS.isTagged(material)
                || material == Material.PISTON || material == Material.STICKY_PISTON
                || material == Material.CHISELED_BOOKSHELF
                || material == Material.BROWN_MUSHROOM_BLOCK || material == Material.RED_MUSHROOM_BLOCK
                || material == Material.MUSHROOM_STEM;
    }

    public static boolean isMusicDisc(ItemStack itemStack) {
        if (isInvalidItem(itemStack)) return false;
        // native disks don't seem to have jukebox playable set to true
        if (VersionUtil.atOrAbove("1.21") && itemStack.hasItemMeta() && itemStack.getItemMeta().hasJukeboxPlayable()) {
            return true;
        } else {
            return itemStack.getType().isRecord();
        }
    }

    public static boolean isInvalidItem(ItemStack itemStack) {
        return itemStack == null || itemStack.getType().isAir() || itemStack.getAmount() == 0;
    }

    @Nullable
    public static ItemStack getUsingConvertsTo(ItemMeta itemMeta) {
        if (!VersionUtil.atOrAbove("1.21") || itemMeta == null)
            return null;

        if (VersionUtil.atOrAbove("1.21.2"))
            return itemMeta.hasUseRemainder() ? itemMeta.getUseRemainder() : null;
        try {
            return (ItemStack) FoodComponent.class.getMethod("getUsingConvertsTo").invoke(itemMeta.getFood());
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
