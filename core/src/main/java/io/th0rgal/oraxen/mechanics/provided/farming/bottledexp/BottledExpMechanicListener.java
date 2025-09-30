package io.th0rgal.oraxen.mechanics.provided.farming.bottledexp;

import io.th0rgal.oraxen.api.OraxenItems;
import io.th0rgal.oraxen.config.Message;
import io.th0rgal.oraxen.utils.EventUtils;
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
        ItemStack item = event.getItem();
        String itemID = OraxenItems.getIdByItem(item);
        Player player = event.getPlayer();

        if (action != Action.LEFT_CLICK_AIR && action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK)
            return;
        if (item == null || factory.isNotImplementedIn(itemID))
            return;

        BottledExpMechanic mechanic = (BottledExpMechanic) factory.getMechanic(itemID);
        if (mechanic == null)
            return;

        int bottlesAmount = mechanic.getBottleEquivalent(player.getLevel(), player.getExp());
        if (bottlesAmount <= 0) {
            Message.NOT_ENOUGH_EXP.send(player);
            return;
        }

        ItemStack bottlesStack = new ItemStack(Material.EXPERIENCE_BOTTLE, bottlesAmount);
        player.getWorld().dropItem(player.getLocation(), bottlesStack);
        player.setLevel(0);
        player.setExp(0);

        EventUtils.callEvent(new PlayerItemDamageEvent(player, item, factory.getDurabilityCost()));
    }
}
