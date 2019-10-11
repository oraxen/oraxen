package io.th0rgal.oraxen.utils;

import io.th0rgal.oraxen.items.OraxenItems;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.Map;

public class ConfigUtils {

    //todo: use it for recipes
    public static ItemStack parseToItemStack(Map<String, Object> section) {

        if (section.get("oraxen_item") != null)
            return OraxenItems.getItemById((String)section.get("oraxen_item")).build();

        if (section.get("minecraft_type") != null)
            return new ItemStack(Material.getMaterial((String) section.get("minecraft_type")));

        return (ItemStack)section.get("minecraft_item");
    }

}
