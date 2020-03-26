package io.th0rgal.oraxen.mechanics.provided.thor;

import io.th0rgal.oraxen.items.OraxenItems;
import io.th0rgal.oraxen.mechanics.MechanicFactory;
import io.th0rgal.oraxen.settings.Message;
import io.th0rgal.oraxen.utils.timers.Timer;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

public class ThorMechanicListener implements Listener {

    private final MechanicFactory factory;

    public ThorMechanicListener(MechanicFactory factory) {
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
        Timer playerTimer = mechanic.getTimer(player);

        if (!playerTimer.isFinished()) {
            Message.DELAY.send(player, Timer.DECIMAL_FORMAT.format(playerTimer.getRemainingTimeMillis() / 1000D));
            return;
        }

        playerTimer.reset();
        for (int i = 0; i < mechanic.getLightningBoltsAmount(); i++) {
            Location target = event.getPlayer().getTargetBlock(null, 50).getLocation();
            player.getWorld().strikeLightning(mechanic.getRandomizedLocation(target));
        }

    }

}
