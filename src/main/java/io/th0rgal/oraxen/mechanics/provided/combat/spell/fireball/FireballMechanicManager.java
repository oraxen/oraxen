package io.th0rgal.oraxen.mechanics.provided.combat.spell.fireball;

import io.th0rgal.oraxen.items.OraxenItems;
import io.th0rgal.oraxen.mechanics.MechanicFactory;
import io.th0rgal.oraxen.utils.timers.Timer;
import org.bukkit.entity.Fireball;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

public class FireballMechanicManager implements Listener {

    private final MechanicFactory factory;

    public FireballMechanicManager(MechanicFactory factory) {
        this.factory = factory;
    }

    @EventHandler
    public void onPlayerUse(PlayerInteractEvent event) {

        if (!(event.getAction().equals(Action.RIGHT_CLICK_AIR) || event.getAction().equals(Action.RIGHT_CLICK_BLOCK)))
            return;

        ItemStack item = event.getPlayer().getInventory().getItemInMainHand();

        String itemID = OraxenItems.getIdByItem(item);

        if (factory.isNotImplementedIn(itemID))
            return;

        FireballMechanic mechanic = (FireballMechanic) factory.getMechanic(itemID);

        Player player = event.getPlayer();

        Timer playerTimer = mechanic.getTimer(player);

        if (!playerTimer.isFinished()) {
            mechanic.getTimer(player).sendToPlayer(player);
            return;
        }

        playerTimer.reset();

        Fireball fireball = player.launchProjectile(Fireball.class);
        fireball.setYield((float) mechanic.getYield());
        fireball.setDirection(fireball.getDirection().multiply(mechanic.getSpeed()));

        mechanic.removeCharge(item);
    }
}
