package io.th0rgal.oraxen.utils.drops;

import io.th0rgal.oraxen.api.OraxenItems;
import io.th0rgal.oraxen.compatibilities.provided.ecoitems.WrappedEcoItem;
import io.th0rgal.oraxen.compatibilities.provided.mythiccrucible.WrappedCrucibleItem;
import io.th0rgal.oraxen.items.ItemUpdater;
import net.Indyuce.mmoitems.MMOItems;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.LinkedHashMap;
import java.util.concurrent.ThreadLocalRandom;

public class Loot {

    private ItemStack itemStack;
    private final int probability;
    private final int minAmount;
    private final int maxAmount;
    private LinkedHashMap<String, Object> config;

    public Loot(LinkedHashMap<String, Object> config) {
        this.probability = config.containsKey("probability") ? (int) (1D / (double) config.get("probability")) : 1;
        this.minAmount = config.containsKey("min_amount") ? (int) config.get("min_amount") : 1;
        this.maxAmount = config.containsKey("max_amount") ? Math.max((int) config.get("max_amount"), this.minAmount) : this.minAmount;
        this.config = config;
    }

    public Loot(ItemStack itemStack, double probability) {
        this.itemStack = itemStack;
        this.probability = (int) (1D / probability);
        this.minAmount = 1;
        this.maxAmount = 1;
    }

    public Loot(ItemStack itemStack, double probability, int minAmount, int maxAmount) {
        this.itemStack = itemStack;
        this.probability = (int) (1D / probability);
        this.minAmount = minAmount;
        this.maxAmount = maxAmount;
    }

    public ItemStack getItemStack() {
        if (itemStack != null) return ItemUpdater.updateItem(itemStack);

        if (config.containsKey("oraxen_item")) {
            String itemId = config.get("oraxen_item").toString();
            itemStack = OraxenItems.getItemById(itemId).build();
        } else if (config.containsKey("crucible_item")) {
            itemStack = new WrappedCrucibleItem(config.get("crucible_item").toString()).build();
        } else if (config.containsKey("mmoitems_id") && config.containsKey("mmoitems_type")) {
            String type = config.get("mmoitems_type").toString();
            String id = config.get("mmoitems_id").toString();
            itemStack = MMOItems.plugin.getItem(type, id);
        } else if (config.containsKey("ecoitem")) {
            itemStack = new WrappedEcoItem(config.get("ecoitem").toString()).build();
        } else if (config.containsKey("minecraft_type")) {
            String itemType = config.get("minecraft_type").toString();
            Material material = Material.getMaterial(itemType);
            itemStack = material != null ? new ItemStack(material) : null;
        } else itemStack = (ItemStack) config.get("minecraft_item");
        return ItemUpdater.updateItem(itemStack);
    }

    public void setItemStack(ItemStack itemStack) {
        this.itemStack = itemStack;
    }

    public int getProbability() {
        return probability;
    }

    public int getMaxAmount() {
        return maxAmount;
    }

    public void dropNaturally(Location location, int amountMultiplier) {
        if (ThreadLocalRandom.current().nextInt(probability) == 0)
            dropItems(location, amountMultiplier);
    }

    public ItemStack getItem(int amountMultiplier) {
        ItemStack stack = getItemStack().clone();
        int dropAmount = ThreadLocalRandom.current().nextInt(minAmount, maxAmount + 1);
        stack.setAmount(stack.getAmount() * amountMultiplier * dropAmount);
        return ItemUpdater.updateItem(stack);
    }

    private void dropItems(Location location, int amountMultiplier) {
        if (location.getWorld() != null) location.getWorld().dropItemNaturally(location, getItem(amountMultiplier));
    }
}
