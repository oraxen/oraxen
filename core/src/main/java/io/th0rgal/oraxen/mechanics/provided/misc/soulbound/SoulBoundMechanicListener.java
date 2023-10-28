package io.th0rgal.oraxen.mechanics.provided.misc.soulbound;

import com.jeff_media.morepersistentdatatypes.DataType;
import io.th0rgal.oraxen.api.OraxenItems;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

public class SoulBoundMechanicListener implements Listener {
    private final SoulBoundMechanicFactory factory;

    public SoulBoundMechanicListener(SoulBoundMechanicFactory factory) {
        this.factory = factory;
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        if (event.getKeepInventory())
            return;

        Random random = ThreadLocalRandom.current();
        List<ItemStack> items = new ArrayList<>();
        for (ItemStack drop : event.getDrops()) {
            String itemID = OraxenItems.getIdByItem(drop);
            if (itemID == null || factory.isNotImplementedIn(itemID))
                continue;

            SoulBoundMechanic mechanic = (SoulBoundMechanic) factory.getMechanic(itemID);
            if (random.nextInt(100) >= mechanic.getLoseChance() * 100)
                items.add(drop);
        }
        if (!items.isEmpty()) {
            Player player = event.getEntity();
            PersistentDataContainer pdc = player.getPersistentDataContainer();
            pdc.set(SoulBoundMechanic.NAMESPACED_KEY, DataType.ITEM_STACK_ARRAY, items.toArray(ItemStack[]::new));
            event.getDrops().removeAll(items);
        }
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        PersistentDataContainer pdc = player.getPersistentDataContainer();
        if(!pdc.has(SoulBoundMechanic.NAMESPACED_KEY, DataType.ITEM_STACK_ARRAY))
            return;

        ItemStack[] items = pdc.getOrDefault(SoulBoundMechanic.NAMESPACED_KEY, DataType.ITEM_STACK_ARRAY, new ItemStack[0]);
        Collection<ItemStack> remainingItems = player.getInventory().addItem(items).values();
        for(final ItemStack item : remainingItems) {
            player.getWorld().dropItem(player.getLocation(), item);
        }

        pdc.remove(SoulBoundMechanic.NAMESPACED_KEY);
    }
}
