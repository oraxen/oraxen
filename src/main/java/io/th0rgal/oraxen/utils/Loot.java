package io.th0rgal.oraxen.utils;

import io.th0rgal.oraxen.items.OraxenItems;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.LinkedHashMap;
import java.util.Random;

public class Loot {

    ItemStack itemStack;
    int probability;
    int maxAmount;
    LinkedHashMap<String, Object> config;

    public Loot(LinkedHashMap<String, Object> config) {
        this.probability = (int) (1D / (double) config.get("probability"));
        this.maxAmount = config.containsValue("max_amount")
                ? (int) config.get("max_amount") : 1;
        this.config = config;
    }

    public Loot(ItemStack itemStack, double probability) {
        this.itemStack = itemStack;
        this.probability = (int) (1D / probability);
        this.maxAmount = 1;
    }

    public Loot(ItemStack itemStack, double probability, int maxAmount) {
        this.itemStack = itemStack;
        this.probability = (int) (1D / probability);
        this.maxAmount = maxAmount;
    }

    private ItemStack getItemStack() {
        if (this.itemStack != null) {
            return this.itemStack;
        } else {
            if (config.containsKey("oraxen_item")) {
                this.itemStack = OraxenItems.getItemById((String) config.get("oraxen_item")).build();
            } else if (config.containsKey("minecraft_type")) {
                this.itemStack = new ItemStack(Material.getMaterial((String) config.get("minecraft_type")));
            } else {
                this.itemStack = (ItemStack) config.get("minecraft_item");
            }
        }
        return this.itemStack;
    }

    public void dropNaturally(Location location) {
        if (new Random().nextInt(probability) == 0)
            for (int i = 0; i < maxAmount; i++)
                location.getWorld().dropItemNaturally(location, getItemStack());
    }


}
