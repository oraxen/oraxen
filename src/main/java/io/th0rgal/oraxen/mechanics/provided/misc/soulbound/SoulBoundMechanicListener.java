package io.th0rgal.oraxen.mechanics.provided.misc.soulbound;

import com.jeff_media.morepersistentdatatypes.DataType;
import io.th0rgal.oraxen.api.OraxenItems;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class SoulBoundMechanicListener implements Listener {
    private final SoulBoundMechanicFactory factory;

    public SoulBoundMechanicListener(SoulBoundMechanicFactory factory) {
        this.factory = factory;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerDeath(PlayerDeathEvent event) {
        if (event.getKeepInventory())
            return;

        List<ItemStack> items = new ArrayList<>();
        for (Iterator<ItemStack> iterator = event.getDrops().iterator(); iterator.hasNext();) {
            ItemStack drop = iterator.next();
            String itemID = OraxenItems.getIdByItem(drop);
            if (itemID == null || factory.isNotImplementedIn(itemID))
                continue;

            SoulBoundMechanic mechanic = (SoulBoundMechanic) factory.getMechanic(itemID);
            // Keep the drop with probability (1 - loseChance).
            if (ThreadLocalRandom.current().nextDouble() >= mechanic.getLoseChance()) {
                items.add(drop);
                iterator.remove();
            }
        }
        if (!items.isEmpty()) {
            event.getItemsToKeep().addAll(items);
        }
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        PersistentDataContainer pdc = player.getPersistentDataContainer();
        if(!pdc.has(SoulBoundMechanic.NAMESPACED_KEY, DataType.ITEM_STACK_ARRAY))
            return;

        ItemStack[] items = pdc.getOrDefault(SoulBoundMechanic.NAMESPACED_KEY, DataType.ITEM_STACK_ARRAY, new ItemStack[0]);
        List<ItemStack> itemsToRestore = getMissingItems(items, player.getInventory().getContents());

        Collection<ItemStack> remainingItems = player.getInventory().addItem(itemsToRestore.toArray(ItemStack[]::new)).values();
        for(final ItemStack item : remainingItems) {
            player.getWorld().dropItem(player.getLocation(), item);
        }

        pdc.remove(SoulBoundMechanic.NAMESPACED_KEY);
    }

    private static List<ItemStack> getMissingItems(ItemStack[] expectedItems, ItemStack[] currentItems) {
        List<ItemStack> missingItems = new ArrayList<>();
        List<ItemStack> availableItems = new ArrayList<>();
        for (ItemStack currentItem : currentItems) {
            if (currentItem != null)
                availableItems.add(currentItem.clone());
        }

        for (ItemStack expectedItem : expectedItems) {
            int missingAmount = consumeAvailableAmount(expectedItem, availableItems);
            if (missingAmount <= 0)
                continue;

            ItemStack missingItem = expectedItem.clone();
            missingItem.setAmount(missingAmount);
            missingItems.add(missingItem);
        }

        return missingItems;
    }

    private static int consumeAvailableAmount(ItemStack expectedItem, List<ItemStack> availableItems) {
        int missingAmount = expectedItem.getAmount();
        for (ItemStack availableItem : availableItems) {
            if (missingAmount <= 0)
                break;

            if (!expectedItem.isSimilar(availableItem))
                continue;

            int consumedAmount = Math.min(missingAmount, availableItem.getAmount());
            availableItem.setAmount(availableItem.getAmount() - consumedAmount);
            missingAmount -= consumedAmount;
        }

        return missingAmount;
    }
}
