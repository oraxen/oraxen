package io.th0rgal.oraxen.mechanics.provided.cosmetic.aura;

import io.th0rgal.oraxen.api.OraxenItems;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class AuraMechanicListener implements Listener {

    private final AuraMechanicFactory factory;
    // Use thread-safe map for Folia compatibility (concurrent region thread access)
    private final Map<Player, AuraMechanic> registeredPlayers;

    public AuraMechanicListener(AuraMechanicFactory factory) {
        this.factory = factory;
        registeredPlayers = new ConcurrentHashMap<>();
    }

    public void enable(Player player, AuraMechanic mechanic) {
        disable(player);
        mechanic.add(player);
        registeredPlayers.put(player, mechanic);
    }

    public void disable(Player player) {
        AuraMechanic aura = registeredPlayers.remove(player);
        if (aura != null) aura.remove(player);
    }

    @EventHandler(priority = EventPriority.LOW)
    public void unregister(PlayerQuitEvent event) {
        disable(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.LOW)
    public void register(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        Inventory inventory = player.getInventory();
        ItemStack item = inventory.getItem(event.getNewSlot());
        String itemID = OraxenItems.getIdByItem(item);

        if (factory.isNotImplementedIn(itemID)) disable(player);
        else enable(player, (AuraMechanic) factory.getMechanic(itemID));

    }

}
