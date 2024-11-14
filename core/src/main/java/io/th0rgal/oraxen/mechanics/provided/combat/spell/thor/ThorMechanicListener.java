package io.th0rgal.oraxen.mechanics.provided.combat.spell.thor;

import io.th0rgal.oraxen.api.OraxenItems;
import io.th0rgal.oraxen.mechanics.MechanicFactory;
import io.th0rgal.oraxen.utils.BlockHelpers;
import io.th0rgal.oraxen.utils.timers.Timer;
import io.th0rgal.protectionlib.ProtectionLib;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
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
        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        String itemID = OraxenItems.getIdByItem(item);
        ThorMechanic mechanic = (ThorMechanic) factory.getMechanic(item);
        Block block = event.getClickedBlock();
        Location targetBlock;
        try {
            targetBlock = player.getTargetBlock(null, 50).getLocation();
        } catch (Exception e) {
            return;
        }

        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (event.useItemInHand() == Event.Result.DENY) return;
        if (BlockHelpers.isInteractable(block) && event.useInteractedBlock() == Event.Result.ALLOW) return;
        if (!ProtectionLib.canUse(player, targetBlock)) return;
        if (factory.isNotImplementedIn(itemID)) return;
        if (mechanic == null) return;

        Timer playerTimer = mechanic.getTimer(player);
        if (!playerTimer.isFinished()) {
            mechanic.getTimer(player).sendToPlayer(player);
            return;
        }

        playerTimer.reset();
        mechanic.removeCharge(item);
        for (int i = 0; i < mechanic.getLightningBoltsAmount(); i++) {
            player.getWorld().strikeLightning(mechanic.getRandomizedLocation(targetBlock));
        }

    }

}
