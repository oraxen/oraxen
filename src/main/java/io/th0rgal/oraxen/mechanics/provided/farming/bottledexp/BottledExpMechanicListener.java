package io.th0rgal.oraxen.mechanics.provided.farming.bottledexp;

import io.th0rgal.oraxen.api.OraxenItems;
import io.th0rgal.oraxen.configs.Message;
import io.th0rgal.oraxen.mechanics.provided.gameplay.block.BlockDurability;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

public class BottledExpMechanicListener implements Listener {

    private static final int MAX_DROPPED_STACKS = 64;

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

        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK)
            return;
        if (event.useItemInHand() == Event.Result.DENY)
            return;
        if (item == null || factory.isNotImplementedIn(itemID))
            return;

        BottledExpMechanic mechanic = (BottledExpMechanic) factory.getMechanic(itemID);
        if (mechanic == null)
            return;

        EquipmentSlot hand = event.getHand();
        if (hand == null) return;

        int bottlesAmount = mechanic.getBottleEquivalent(player.getLevel(), player.getExp());
        if (bottlesAmount <= 0) {
            Message.NOT_ENOUGH_EXP.send(player);
            return;
        }

        int maxStackSize = Material.EXPERIENCE_BOTTLE.getMaxStackSize();
        int droppedBottles = Math.min(bottlesAmount, maxStackSize * MAX_DROPPED_STACKS);
        int remainingBottles = droppedBottles;
        while (remainingBottles > 0) {
            int stackAmount = Math.min(remainingBottles, maxStackSize);
            player.getWorld().dropItem(player.getLocation(), new ItemStack(Material.EXPERIENCE_BOTTLE, stackAmount));
            remainingBottles -= stackAmount;
        }
        player.setLevel(0);
        player.setExp(0);
        int undroppedBottles = bottlesAmount - droppedBottles;
        if (undroppedBottles > 0)
            player.giveExp((int) Math.floor(undroppedBottles * 10.0f / mechanic.ratio));

        BlockDurability.damageItemStack(player, hand, factory.getDurabilityCost());
    }
}
