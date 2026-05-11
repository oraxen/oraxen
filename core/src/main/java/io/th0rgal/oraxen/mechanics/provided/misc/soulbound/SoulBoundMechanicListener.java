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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

public class SoulBoundMechanicListener implements Listener {
    private static final Method GET_ITEMS_TO_KEEP_METHOD = getItemsToKeepMethod();

    private final SoulBoundMechanicFactory factory;

    public SoulBoundMechanicListener(SoulBoundMechanicFactory factory) {
        this.factory = factory;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerDeath(PlayerDeathEvent event) {
        if (event.getKeepInventory())
            return;

        Random random = ThreadLocalRandom.current();
        List<ItemStack> items = new ArrayList<>();
        for (Iterator<ItemStack> iterator = event.getDrops().iterator(); iterator.hasNext();) {
            ItemStack drop = iterator.next();
            String itemID = OraxenItems.getIdByItem(drop);
            if (itemID == null || factory.isNotImplementedIn(itemID))
                continue;

            SoulBoundMechanic mechanic = (SoulBoundMechanic) factory.getMechanic(itemID);
            if (random.nextInt(100) >= mechanic.getLoseChance() * 100) {
                items.add(drop);
                iterator.remove();
            }
        }
        if (!items.isEmpty()) {
            List<ItemStack> itemsToKeep = getItemsToKeep(event);
            if (itemsToKeep != null) {
                itemsToKeep.addAll(items);
                return;
            }

            Player player = event.getEntity();
            PersistentDataContainer pdc = player.getPersistentDataContainer();
            pdc.set(SoulBoundMechanic.NAMESPACED_KEY, DataType.ITEM_STACK_ARRAY, items.toArray(ItemStack[]::new));
        }
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        PersistentDataContainer pdc = player.getPersistentDataContainer();
        if(!pdc.has(SoulBoundMechanic.NAMESPACED_KEY, DataType.ITEM_STACK_ARRAY))
            return;

        ItemStack[] items = pdc.getOrDefault(SoulBoundMechanic.NAMESPACED_KEY, DataType.ITEM_STACK_ARRAY, new ItemStack[0]);
        List<ItemStack> itemsToRestore = new ArrayList<>();
        for (ItemStack item : items) {
            if (!player.getInventory().containsAtLeast(item, item.getAmount()))
                itemsToRestore.add(item);
        }

        Collection<ItemStack> remainingItems = player.getInventory().addItem(itemsToRestore.toArray(ItemStack[]::new)).values();
        for(final ItemStack item : remainingItems) {
            player.getWorld().dropItem(player.getLocation(), item);
        }

        pdc.remove(SoulBoundMechanic.NAMESPACED_KEY);
    }

    @SuppressWarnings("unchecked")
    private static List<ItemStack> getItemsToKeep(PlayerDeathEvent event) {
        if (GET_ITEMS_TO_KEEP_METHOD == null)
            return null;

        try {
            return (List<ItemStack>) GET_ITEMS_TO_KEEP_METHOD.invoke(event);
        } catch (IllegalAccessException | InvocationTargetException ignored) {
            return null;
        }
    }

    private static Method getItemsToKeepMethod() {
        try {
            return PlayerDeathEvent.class.getMethod("getItemsToKeep");
        } catch (NoSuchMethodException ignored) {
            return null;
        }
    }
}
