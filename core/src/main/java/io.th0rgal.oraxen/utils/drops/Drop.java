package io.th0rgal.oraxen.utils.drops;

import io.th0rgal.oraxen.api.OraxenItems;
import io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.FurnitureMechanic;
import io.th0rgal.oraxen.mechanics.provided.gameplay.stringblock.StringBlockMechanicFactory;
import io.th0rgal.oraxen.mechanics.provided.misc.itemtype.ItemTypeMechanic;
import io.th0rgal.oraxen.mechanics.provided.misc.itemtype.ItemTypeMechanicFactory;
import io.th0rgal.oraxen.utils.BlockHelpers;
import io.th0rgal.oraxen.utils.Utils;
import io.th0rgal.oraxen.utils.logs.Logs;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
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
    public static Drop createDrop(@NotNull ConfigurationSection dropSection, String itemID) {
        List<Loot> loots = ((List<LinkedHashMap<String, Object>>) dropSection.getList("loots", new ArrayList<>())).stream().map(Loot::new).toList();
        return new Drop(StringBlockMechanicFactory.getInstance().toolTypes, loots, dropSection.getBoolean("silktouch"),
                dropSection.getBoolean("fortune"), itemID,
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

    public String getItemType(ItemStack itemInHand) {
        String itemID = OraxenItems.getIdByItem(itemInHand);
        ItemTypeMechanicFactory factory = ItemTypeMechanicFactory.get();
        if (factory.isNotImplementedIn(itemID)) {
            String[] content = itemInHand.getType().toString().split("_");
            return content.length >= 2 ? content[0] : "";
        } else {
            ItemTypeMechanic mechanic = (ItemTypeMechanic) factory.getMechanic(itemID);
            return mechanic.itemType;
        }
    }

    public boolean canDrop(ItemStack itemInHand) {
        return isToolEnough(itemInHand) && isTypeEnough(itemInHand);
    }

    public boolean isTypeEnough(ItemStack itemInHand) {
        if (!minimalType.isEmpty()) {
            String itemType = itemInHand == null ? "" : getItemType(itemInHand);
            return !itemType.isEmpty() && hierarchy.contains(itemType)
                    && (hierarchy.indexOf(itemType) >= hierarchy.indexOf(minimalType));
        } else return true;
    }

    public boolean isToolEnough(ItemStack itemInHand) {
        if (!bestTools.isEmpty()) {
            String itemID = OraxenItems.getIdByItem(itemInHand);
            String type = itemInHand == null ? "AIR" : itemInHand.getType().toString().toUpperCase();
            if (itemID != null && bestTools.stream().anyMatch(itemID::equalsIgnoreCase)) return true;
            else if (bestTools.contains(type)) return true;
            else return bestTools.stream().anyMatch(toolName -> type.endsWith(toolName.toUpperCase()));
        } else return true;
    }

    public int getDiff(ItemStack item) {
        return (minimalType == null) ? 0 : hierarchy.indexOf(getItemType(item)) - hierarchy.indexOf(minimalType);
    }

    public List<Loot> getLoots() {
        return loots;
    }

    public void spawns(Location location, ItemStack itemInHand) {
        if (!canDrop(itemInHand) || !BlockHelpers.isLoaded(location)) return;
        ItemStack baseItem = OraxenItems.getItemById(sourceID).build();

        if (silktouch && itemInHand.hasItemMeta() && itemInHand.getItemMeta().hasEnchant(Enchantment.SILK_TOUCH))
            location.getWorld().dropItemNaturally(BlockHelpers.toCenterBlockLocation(location), baseItem);
        else dropLoot(loots, location, getFortuneMultiplier(itemInHand));
    }

    public void furnitureSpawns(Entity baseEntity, ItemStack itemInHand) {
        ItemStack baseItem = OraxenItems.getItemById(sourceID).build();
        Location location = baseEntity.getLocation();
        ItemStack furnitureItem = FurnitureMechanic.getFurnitureItem(baseEntity);
        Utils.editItemMeta(furnitureItem, (itemMeta) -> {
            ItemMeta baseMeta = baseItem.getItemMeta();
            if (baseMeta != null && baseMeta.hasDisplayName())
                itemMeta.setDisplayName(baseMeta.getDisplayName());
        });

        if (!canDrop(itemInHand) || !location.isWorldLoaded()) return;
        assert location.getWorld() != null;

        if (silktouch && itemInHand.hasItemMeta() && itemInHand.getItemMeta().hasEnchant(Enchantment.SILK_TOUCH)) {
            location.getWorld().dropItemNaturally(BlockHelpers.toCenterBlockLocation(location), baseItem);
        } else {
            // Drop all the items that aren't the furniture item
            dropLoot(loots.stream().filter(loot ->
                    !loot.getItemStack().equals(baseItem)).toList(), location, getFortuneMultiplier(itemInHand));
            // Filter loots down to only the furniture item and drop the item in the actual Furniture to preseve color etc.
            dropLoot(loots.stream()
                    .filter(loot -> loot.getItemStack().equals(baseItem))
                    .map(loot -> new Loot(furnitureItem, loot.getProbability(), 1, loot.getMaxAmount()))
                    .toList(), location, getFortuneMultiplier(itemInHand));
        }
    }

    private int getFortuneMultiplier(ItemStack itemInHand) {
        int fortuneMultiplier = 1;
        if (itemInHand != null) {
            ItemMeta itemMeta = itemInHand.getItemMeta();
            if (itemMeta != null) {
                if (fortune && itemMeta.hasEnchant(Enchantment.LOOT_BONUS_BLOCKS))
                    fortuneMultiplier += ThreadLocalRandom.current().nextInt(itemMeta.getEnchantLevel(Enchantment.LOOT_BONUS_BLOCKS));
            }
        }
        return fortuneMultiplier;
    }

    private void dropLoot(List<Loot> loots, Location location, int fortuneMultiplier) {
        for (Loot loot : loots) loot.dropNaturally(location, fortuneMultiplier);
    }
}
