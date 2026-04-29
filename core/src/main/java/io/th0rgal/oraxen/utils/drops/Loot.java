package io.th0rgal.oraxen.utils.drops;

import io.th0rgal.oraxen.utils.IntegerRange;
import io.th0rgal.oraxen.api.OraxenItems;
import io.th0rgal.oraxen.compatibilities.provided.ecoitems.WrappedEcoItem;
import io.th0rgal.oraxen.compatibilities.provided.executableitems.WrappedExecutableItem;
import io.th0rgal.oraxen.compatibilities.provided.mythiccrucible.WrappedCrucibleItem;
import io.th0rgal.oraxen.items.ItemUpdater;
import io.th0rgal.oraxen.utils.OraxenYaml;
import io.th0rgal.oraxen.utils.Utils;
import io.th0rgal.oraxen.utils.logs.Logs;
import net.Indyuce.mmoitems.MMOItems;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.LinkedHashMap;
import java.util.concurrent.ThreadLocalRandom;

public class Loot {

    private final String sourceID;
    private ItemStack itemStack;
    private final double probability;
    private final IntegerRange amount;
    private LinkedHashMap<String, Object> config;

    public Loot(LinkedHashMap<String, Object> config, String sourceID) {
        this.probability = Double.parseDouble(config.getOrDefault("probability", 1).toString());
        if (config.getOrDefault("amount", "") instanceof String amount && amount.contains("..")) {
            this.amount = Utils.parseToRange(amount);
        } else this.amount = new IntegerRange(1,1);
        this.config = config;
        this.sourceID = sourceID;
    }

    public Loot(ItemStack itemStack, double probability) {
        this.itemStack = itemStack;
        this.probability = Math.min(1.0, probability);
        this.amount = new IntegerRange(1,1);
        this.sourceID = null;
    }

    public Loot(String sourceID, ItemStack itemStack, double probability, int minAmount, int maxAmount) {
        this.sourceID = sourceID;
        this.itemStack = itemStack;
        this.probability = Math.min(1.0, probability);
        this.amount = new IntegerRange(minAmount, maxAmount);
    }

    public Loot(String sourceID, ItemStack itemStack, double probability, IntegerRange amount) {
        this.sourceID = sourceID;
        this.itemStack = itemStack;
        this.probability = Math.min(1.0, probability);
        this.amount = amount;
    }

    public ItemStack getItemStack() {
        if (itemStack != null) return ItemUpdater.updateItem(itemStack);

        String oraxenItemId = getConfigString("oraxen_item");
        String crucibleItemId = getConfigString("crucible_item");
        String mmoItemsId = getConfigString("mmoitems_id");
        String mmoItemsType = getConfigString("mmoitems_type");
        String ecoItemId = getConfigString("ecoitem");
        String executableItemId = getConfigString("executableitem");
        String minecraftType = getConfigString("minecraft_type");

        if (oraxenItemId != null) {
            if (OraxenItems.getItemById(oraxenItemId) != null)
                itemStack = OraxenItems.getItemById(oraxenItemId).build();
        } else if (crucibleItemId != null) {
            itemStack = new WrappedCrucibleItem(crucibleItemId).build();
        } else if (mmoItemsId != null && mmoItemsType != null) {
            String type = mmoItemsType;
            String id = mmoItemsId;
            itemStack = MMOItems.plugin.getItem(type, id);
        } else if (ecoItemId != null) {
            itemStack = new WrappedEcoItem(ecoItemId).build();
        } else if (executableItemId != null) {
            itemStack = new WrappedExecutableItem(executableItemId).build();
        } else if (minecraftType != null) {
            Material material = OraxenYaml.getMaterial(minecraftType);
            itemStack = material != null ? new ItemStack(material) : null;
        } else if (config.containsKey("minecraft_item")) {
            itemStack = (ItemStack) config.get("minecraft_item");
        }

        if (itemStack == null && sourceID != null && OraxenItems.getItemById(sourceID) != null)
            itemStack = OraxenItems.getItemById(sourceID).build();

        if (itemStack == null)
            Logs.logWarning("Failed to resolve loot item for source " + sourceID + " with config " + config);

        return itemStack != null ? ItemUpdater.updateItem(itemStack) : null;
    }

    public void setItemStack(ItemStack itemStack) {
        this.itemStack = itemStack;
    }

    public double getProbability() {
        return probability;
    }

    public IntegerRange amount() {
        return this.amount;
    }

    public int getMaxAmount() {
        return amount.getUpperBound();
    }

    public void dropNaturally(Location location, int amountMultiplier) {
        if (Math.random() <= probability)
            dropItems(location, amountMultiplier);
    }

    public ItemStack getItem(int amountMultiplier) {
        ItemStack baseStack = getItemStack();
        if (baseStack == null) return null;

        ItemStack stack = baseStack.clone();
        int dropAmount = ThreadLocalRandom.current().nextInt(amount.getLowerBound(), amount.getUpperBound() + 1);
        stack.setAmount(stack.getAmount() * amountMultiplier * dropAmount);
        return ItemUpdater.updateItem(stack);
    }

    private void dropItems(Location location, int amountMultiplier) {
        ItemStack item = getItem(amountMultiplier);
        if (location.getWorld() != null && item != null) location.getWorld().dropItemNaturally(location, item);
    }

    private String getConfigString(String key) {
        Object value = config.get(key);
        if (value == null) return null;

        String stringValue = value.toString().trim();
        return stringValue.isEmpty() ? null : stringValue;
    }
}
