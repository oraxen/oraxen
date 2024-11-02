package io.th0rgal.oraxen.utils;

import io.th0rgal.oraxen.items.ItemBuilder;
import net.kyori.adventure.text.Component;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

public class ItemUtils {

    public static boolean isEmpty(ItemStack itemStack) {
        return itemStack == null || itemStack.getType() == Material.AIR || itemStack.getAmount() == 0;
    }

    public static void subtract(ItemStack itemStack, int amount) {
        itemStack.setAmount(Math.max(0, itemStack.getAmount() - amount));
    }

    public static void itemName(ItemMeta itemMeta, ItemMeta otherMeta) {
        if (itemMeta == null) return;
        if (VersionUtil.isPaperServer()) itemName(itemMeta, otherMeta.itemName());
        else itemMeta.setItemName(otherMeta.getItemName());
    }

    public static void itemName(ItemMeta itemMeta, @Nullable Component component) {
        component = component != Component.empty() ? component : null;
        if (VersionUtil.isPaperServer()) itemMeta.itemName(component);
        else itemMeta.setItemName(AdventureUtils.LEGACY_SERIALIZER.serialize(component));
    }

    public static void itemName(ItemBuilder itemBuilder, @Nullable Component component) {
        component = component != Component.empty() ? component : null;
        if (VersionUtil.isPaperServer()) itemBuilder.itemName(component);
        else itemBuilder.setItemName(AdventureUtils.LEGACY_SERIALIZER.serialize(component));
    }

    public static void displayName(ItemMeta itemMeta, ItemMeta otherMeta) {
        if (itemMeta == null) return;
        if (VersionUtil.isPaperServer()) displayName(itemMeta, otherMeta.displayName());
        else itemMeta.setDisplayName(otherMeta.getDisplayName());
    }

    public static void displayName(ItemStack itemStack, @Nullable Component component) {
        editItemMeta(itemStack, itemMeta -> {
            if (VersionUtil.isPaperServer()) itemMeta.displayName(component);
            else itemMeta.setDisplayName(Optional.ofNullable(component).map(AdventureUtils.LEGACY_SERIALIZER::serialize).orElse(null));
        });
    }

    public static void displayName(ItemBuilder itemBuilder, @Nullable Component component) {
        if (VersionUtil.isPaperServer()) itemBuilder.displayName(component);
        else itemBuilder.setDisplayName(Optional.ofNullable(component).map(AdventureUtils.LEGACY_SERIALIZER::serialize).orElse(null));
    }

    public static void displayName(ItemMeta itemMeta, @Nullable Component component) {
        component = component != Component.empty() ? component : null;
        if (VersionUtil.isPaperServer()) itemMeta.displayName(component);
        else itemMeta.setDisplayName(AdventureUtils.LEGACY_SERIALIZER.serialize(component));
    }

    public static void lore(ItemStack itemStack, List<Component> components) {
        if (VersionUtil.isPaperServer()) itemStack.lore(components);
        else itemStack.setLore(components.stream().map(AdventureUtils.LEGACY_SERIALIZER::serialize).toList());
    }

    public static void lore(ItemStack itemStack, Collection<String> strings) {
        if (VersionUtil.isPaperServer()) itemStack.lore(strings.stream().map(AdventureUtils.MINI_MESSAGE::deserialize).toList());
        else itemStack.setLore(strings.stream().toList());
    }

    public static void lore(ItemMeta itemMeta, List<Component> components) {
        if (VersionUtil.isPaperServer()) itemMeta.lore(components);
        else itemMeta.setLore(components.stream().map(AdventureUtils.LEGACY_SERIALIZER::serialize).toList());
    }

    public static void lore(ItemMeta itemMeta, Collection<String> strings) {
        if (VersionUtil.isPaperServer()) itemMeta.lore(strings.stream().map(AdventureUtils.MINI_MESSAGE::deserialize).toList());
        else itemMeta.setLore(strings.stream().toList());
    }

    public static void lore(ItemBuilder itemBuilder, List<Component> components) {
        if (VersionUtil.isPaperServer()) itemBuilder.lore(components);
        else itemBuilder.setLore(components.stream().map(AdventureUtils.LEGACY_SERIALIZER::serialize).toList());
    }

    public static void lore(ItemBuilder itemBuilder, Collection<String> strings) {
        if (VersionUtil.isPaperServer()) itemBuilder.lore(strings.stream().map(AdventureUtils.MINI_MESSAGE::deserialize).toList());
        else itemBuilder.setLore(strings.stream().toList());
    }

    public static void lore(ItemMeta itemMeta, ItemMeta otherMeta) {
        if (VersionUtil.isPaperServer()) itemMeta.lore(otherMeta.lore());
        else itemMeta.setLore(otherMeta.getLore());
    }

    public static Optional<Color> dyeColor(@NotNull ItemStack itemStack) {
        return Optional.ofNullable(
                switch (itemStack.getItemMeta()) {
                    case LeatherArmorMeta leatherArmorMeta -> leatherArmorMeta.getColor();
                    case PotionMeta potionMeta -> potionMeta.getColor();
                    case MapMeta mapMeta -> mapMeta.getColor();
                    case null, default -> null;
                }
        );
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
        if (meta == null) return;
        function.accept(meta);
        itemStack.setItemMeta(meta);
    }

    /**
     * Used to correctly damage the item in the player's hand based on broken block
     * Only handles it if the block is a OraxenBlock or OraxenFurniture
     *
     * @param player    the player that broke the OraxenBlock or OraxenFurniture
     * @param itemStack the item in the player's hand
     */
    public static void damageItem(Player player, ItemStack itemStack) {
        // If the item is not a tool, it will not be damaged, example flint&steel should not be damaged
        int damage = isTool(itemStack) ? 1 : 0;

        if (damage == 0) return;
        if (VersionUtil.isPaperServer()) player.damageItemStack(itemStack, damage);
        else {
            int finalDamage = damage;
            editItemMeta(itemStack, meta -> {
                if (meta instanceof Damageable damageable && EventUtils.callEvent(new PlayerItemDamageEvent(player, itemStack, finalDamage))) {
                    damageable.setDamage(damageable.getDamage() + 1);
                }
            });
        }
    }

    public static boolean isTool(@NotNull ItemStack itemStack) {
        return isTool(itemStack.getType());
    }

    public static boolean isTool(@NotNull Material material) {
        return material.toString().endsWith("_AXE")
                || material.toString().endsWith("_PICKAXE")
                || material.toString().endsWith("_SHOVEL")
                || material.toString().endsWith("_HOE")
                || material.toString().endsWith("_SWORD")
                || material == Material.TRIDENT;
    }

    public static boolean isMusicDisc(@NotNull ItemStack itemStack) {
        if (VersionUtil.atOrAbove("1.21"))
            return itemStack.hasItemMeta() && itemStack.getItemMeta().hasJukeboxPlayable();
        return itemStack.getType().name().startsWith("MUSIC_DISC");
    }
}
