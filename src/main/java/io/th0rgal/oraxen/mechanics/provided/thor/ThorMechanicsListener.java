package io.th0rgal.oraxen.mechanics.provided.thor;

import io.th0rgal.oraxen.items.OraxenItems;
import io.th0rgal.oraxen.mechanics.MechanicFactory;
import io.th0rgal.oraxen.settings.Message;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import java.text.DecimalFormat;

public class ThorMechanicsListener implements Listener {

    private final MechanicFactory factory;
    private static final DecimalFormat decimalFormat = new DecimalFormat("##.##");
    public ThorMechanicsListener(MechanicFactory factory) {
        this.factory = factory;
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onCall(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK)
            return;

        ItemStack item = event.getItem();
        String itemID = OraxenItems.getIdByItem(item);

        if (factory.isNotImplementedIn(itemID))
            return;

        ThorMechanic mechanic = (ThorMechanic) factory.getMechanic(itemID);
        Player player = event.getPlayer();

        long remainingTime = mechanic.getRemainingTime();
        if (remainingTime > 0) {
            Message.DELAY.send(player, decimalFormat.format(remainingTime/1000D));
            return;
        }

        for (int i = 0; i < mechanic.getLightningBoltsAmount(); i++) {
            Location target = event.getPlayer().getTargetBlock(null, 50).getLocation();
            player.getWorld().strikeLightning(mechanic.getRandomizedLocation(target));
        }

    }

}
