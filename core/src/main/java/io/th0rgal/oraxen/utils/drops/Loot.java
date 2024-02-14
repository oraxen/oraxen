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

    private final String sourceID;
    private ItemStack itemStack;
    private final int probability;
    private final int minAmount;
    private final int maxAmount;
    private LinkedHashMap<String, Object> config;

    public Loot(LinkedHashMap<String, Object> config, String sourceID) {
        this.probability = Integer.getInteger(config.getOrDefault("probability", 1).toString(), 1);
        this.minAmount = Integer.getInteger(config.getOrDefault("min_amount", 1).toString(), 1);
        this.maxAmount = Math.max(Integer.getInteger(config.getOrDefault("max_amount", this.minAmount).toString(), this.minAmount), this.minAmount);
        this.config = config;
        this.sourceID = sourceID;
    }

    public Loot(ItemStack itemStack, double probability) {
        this.itemStack = itemStack;
        this.probability = (int) (1D / probability);
        this.minAmount = 1;
        this.maxAmount = 1;
        this.sourceID = null;
    }

    public Loot(String sourceID, ItemStack itemStack, double probability, int minAmount, int maxAmount) {
        this.sourceID = sourceID;
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
        } else if (config.containsKey("minecraft_item")) {
            itemStack = (ItemStack) config.get("minecraft_item");
        }

        if (itemStack == null) itemStack = OraxenItems.getItemById(sourceID).build();

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
