package io.th0rgal.oraxen.mechanics.provided.farming.bottledexp;

import io.th0rgal.oraxen.config.Message;
import io.th0rgal.oraxen.items.OraxenItems;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.bukkit.inventory.ItemStack;

public class BottledExpMechanicListener implements Listener {

    private final BottledExpMechanicFactory factory;

    public BottledExpMechanicListener(BottledExpMechanicFactory factory) {
        this.factory = factory;
    }

    @EventHandler
    public void onRightClick(PlayerInteractEvent event) {
        Action action = event.getAction();
        if (action != Action.LEFT_CLICK_AIR && action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK)
            return;

        ItemStack item = event.getItem();
        if (item == null)
            return;

        String itemID = OraxenItems.getIdByItem(item);
        if (factory.isNotImplementedIn(itemID))
            return;

        BottledExpMechanic mechanic = (BottledExpMechanic) factory.getMechanic(itemID);
        Player player = event.getPlayer();
        ItemStack bottlesStack = new ItemStack(Material.EXPERIENCE_BOTTLE,
                mechanic.getBottleEquivalent(player.getLevel(), player.getExp()));
        if (bottlesStack.getAmount() <= 0) {
            Message.NOT_ENOUGH_EXP.send(player);
            return;
        }

        player.getWorld().dropItem(player.getLocation(), bottlesStack);
        player.setLevel(0);
        player.setExp(0);

        PlayerItemDamageEvent playerItemDamageEvent = new PlayerItemDamageEvent(player, item,
                factory.getDurabilityCost());
        Bukkit.getPluginManager().callEvent(playerItemDamageEvent);
    }

}
