package io.th0rgal.oraxen.utils.drops;

import io.th0rgal.oraxen.api.OraxenItems;
import io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.FurnitureMechanic;
import io.th0rgal.oraxen.mechanics.provided.misc.itemtype.ItemTypeMechanic;
import io.th0rgal.oraxen.mechanics.provided.misc.itemtype.ItemTypeMechanicFactory;
import io.th0rgal.oraxen.utils.BlockHelpers;
import io.th0rgal.oraxen.utils.ItemUtils;
import io.th0rgal.oraxen.utils.wrappers.EnchantmentWrapper;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class Drop {

    private List<String> hierarchy;
    private final List<Loot> loots;
    final boolean silktouch;
    final boolean fortune;
    String minimalType;
    private final List<String> bestTools;
    final String sourceID;

    @SuppressWarnings("unchecked")
    public static Drop createDrop(List<String> toolTypes, @NotNull ConfigurationSection dropSection, String sourceID) {
        List<Loot> loots = ((List<LinkedHashMap<String, Object>>) dropSection.getList("loots", new ArrayList<>())).stream().map(c -> new Loot(c, sourceID)).toList();
        return new Drop(toolTypes, loots, dropSection.getBoolean("silktouch"),
                dropSection.getBoolean("fortune"), sourceID,
                dropSection.getString("minimal_type", ""), dropSection.getStringList("best_tools"));
    }

    public Drop(List<String> hierarchy, List<Loot> loots, boolean silktouch, boolean fortune, String sourceID,
                String minimalType, List<String> bestTools) {
        this.hierarchy = hierarchy;
        this.loots = loots;
        this.silktouch = silktouch;
        this.fortune = fortune;
        this.sourceID = sourceID;
        this.minimalType = minimalType;
        this.bestTools = bestTools;
    }

    public Drop(List<Loot> loots, boolean silktouch, boolean fortune, String sourceID) {
        this.loots = loots;
        this.silktouch = silktouch;
        this.fortune = fortune;
        this.sourceID = sourceID;
        this.bestTools = new ArrayList<>();
    }

    public static Drop emptyDrop() {
        return new Drop(new ArrayList<>(), false, false, "");
    }
    public static Drop emptyDrop(List<Loot> loots) {
        return new Drop(loots, false, false, "");
    }
    public static Drop clone(Drop drop, List<Loot> newLoots) {
        return new Drop(drop.hierarchy, newLoots, drop.silktouch, drop.fortune, drop.sourceID, drop.minimalType, drop.bestTools);
    }

    public String getItemType(ItemStack itemInHand) {
        String itemID = OraxenItems.getIdByItem(itemInHand);
        ItemTypeMechanicFactory factory = ItemTypeMechanicFactory.get();
        if (factory == null || factory.isNotImplementedIn(itemID)) {
            String[] content = itemInHand.getType().toString().split("_");
            return content.length >= 2 ? content[0] : "";
        } else {
            ItemTypeMechanic mechanic = (ItemTypeMechanic) factory.getMechanic(itemID);
            return mechanic.itemType;
        }
    }

    public boolean canDrop(ItemStack itemInHand) {
        return minimalType == null || minimalType.isEmpty() || isToolEnough(itemInHand) && isTypeEnough(itemInHand);
    }

    public boolean isTypeEnough(ItemStack itemInHand) {
        if (minimalType != null && !minimalType.isEmpty()) {
            String itemType = itemInHand == null ? "" : getItemType(itemInHand);
            return !itemType.isEmpty() && hierarchy.contains(itemType)
                    && (hierarchy.indexOf(itemType) >= hierarchy.indexOf(minimalType));
        } else return true;
    }

    public boolean isToolEnough(ItemStack itemInHand) {
        if (!bestTools.isEmpty()) {
            String itemID = OraxenItems.getIdByItem(itemInHand);
            String type = (itemInHand == null ? Material.AIR : itemInHand.getType()).toString().toUpperCase();
            if (itemID != null && bestTools.stream().anyMatch(itemID::equalsIgnoreCase)) return true;
            else if (bestTools.contains(type)) return true;
            else return bestTools.stream().anyMatch(toolName -> type.endsWith(toolName.toUpperCase()));
        } else return true;
    }

    public int getDiff(ItemStack item) {
        return (minimalType == null) ? 0 : hierarchy.indexOf(getItemType(item)) - hierarchy.indexOf(minimalType);
    }

    public boolean isSilktouch() {
        return silktouch;
    }

    public boolean isFortune() {
        return fortune;
    }

    public String getSourceID() {
        return sourceID;
    }

    public String getMinimalType() {
        return minimalType;
    }

    public List<String> getBestTools() {
        return bestTools;
    }

    public List<String> getHierarchy() {
        return hierarchy;
    }

    public List<Loot> getLoots() {
        return loots;
    }

    public Drop setLoots(List<Loot> loots) {
        this.loots.clear();
        this.loots.addAll(loots);
        return this;
    }

    public void spawns(Location location, ItemStack itemInHand) {
        if (!canDrop(itemInHand) || !BlockHelpers.isLoaded(location)) return;
        ItemStack baseItem = OraxenItems.getItemById(sourceID).build();

        if (silktouch && itemInHand.hasItemMeta() && itemInHand.getItemMeta().hasEnchant(EnchantmentWrapper.SILK_TOUCH))
            location.getWorld().dropItemNaturally(BlockHelpers.toCenterBlockLocation(location), baseItem);
        else dropLoot(loots, location, getFortuneMultiplier(itemInHand));
    }

    public void furnitureSpawns(Entity baseEntity, ItemStack itemInHand) {
        ItemStack baseItem = OraxenItems.getItemById(sourceID).build();
        Location location = BlockHelpers.toBlockLocation(baseEntity.getLocation());
        ItemStack furnitureItem = FurnitureMechanic.getFurnitureItem(baseEntity);
        ItemUtils.editItemMeta(furnitureItem, (itemMeta) -> {
            ItemMeta baseMeta = baseItem.getItemMeta();
            if (baseMeta != null && baseMeta.hasDisplayName())
                itemMeta.setDisplayName(baseMeta.getDisplayName());
        });

        if (!canDrop(itemInHand) || !location.isWorldLoaded()) return;
        assert location.getWorld() != null;

        if (silktouch && itemInHand.hasItemMeta() && itemInHand.getItemMeta().hasEnchant(EnchantmentWrapper.SILK_TOUCH)) {
            location.getWorld().dropItemNaturally(BlockHelpers.toCenterBlockLocation(location), baseItem);
        } else {
            // Drop all the items that aren't the furniture item
            dropLoot(loots.stream().filter(loot ->
                    !loot.getItemStack().isSimilar(baseItem) && !OraxenItems.getIdByItem(loot.getItemStack()).equals(sourceID)).toList(), location, getFortuneMultiplier(itemInHand));
            // Filter loots down to only the furniture item and drop the item in the actual Furniture to preseve color etc.
            dropLoot(loots.stream()
                    .filter(loot -> loot.getItemStack().isSimilar(baseItem) || OraxenItems.getIdByItem(loot.getItemStack()).equals(sourceID))
                    .map(loot -> new Loot(sourceID, furnitureItem, loot.getProbability(), 1, loot.getMaxAmount()))
                    .toList(), location, getFortuneMultiplier(itemInHand));
        }
    }

    private int getFortuneMultiplier(ItemStack itemInHand) {
        int fortuneMultiplier = 1;
        if (itemInHand != null) {
            ItemMeta itemMeta = itemInHand.getItemMeta();
            if (itemMeta != null) {
                if (fortune && itemMeta.hasEnchant(EnchantmentWrapper.FORTUNE))
                    fortuneMultiplier += ThreadLocalRandom.current().nextInt(itemMeta.getEnchantLevel(EnchantmentWrapper.FORTUNE));
            }
        }
        return fortuneMultiplier;
    }

    private void dropLoot(List<Loot> loots, Location location, int fortuneMultiplier) {
        for (Loot loot : loots) loot.dropNaturally(location, fortuneMultiplier);
    }

    /**
     * Get the loots that will drop based on a given Player
     * @param player the player that triggered this drop
     * @return the loots that will drop
     */
    public List<Loot> getLootToDrop(Player player) {
        ItemStack itemInHand = player.getInventory().getItemInMainHand();
        int fortuneMultiplier = getFortuneMultiplier(itemInHand);
        List<Loot> droppedLoots = new ArrayList<>();
        for (Loot loot : loots) {
            ItemStack item = loot.getItem(fortuneMultiplier);

            if (!canDrop(itemInHand) || item == null) continue;
            if (Math.random() > loot.getProbability()) continue;

            droppedLoots.add(loot);
        }
        return droppedLoots;
    }
}
