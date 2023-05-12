package io.th0rgal.oraxen.mechanics.provided.misc.misc;

import io.th0rgal.oraxen.api.OraxenItems;
import io.th0rgal.oraxen.utils.Utils;
import io.th0rgal.protectionlib.ProtectionLib;
import org.bukkit.Effect;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.Levelled;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByBlockEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import static org.bukkit.event.entity.EntityDamageEvent.DamageCause.FIRE;
import static org.bukkit.event.entity.EntityDamageEvent.DamageCause.FIRE_TICK;

public class MiscListener implements Listener {

    private final MiscMechanicFactory factory;

    public MiscListener(MiscMechanicFactory factory) {
        this.factory = factory;
    }

    // Since EntityDamageByBlockEvent apparently does not trigger for fire, use this aswell
    @EventHandler
    public void onItemBurnFire(EntityDamageEvent event) {
        if (event.getCause() != FIRE && event.getCause() != FIRE_TICK) return;
        MiscMechanic mechanic = getMiscMechanic(event.getEntity());
        if (mechanic == null || mechanic.burnsInFire()) return;
        event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onItemBurn(EntityDamageByBlockEvent event) {
        MiscMechanic mechanic = getMiscMechanic(event.getEntity());
        if (mechanic == null) return;

        switch (event.getCause()) {
            case CONTACT: if (!mechanic.breaksFromCactus()) event.setCancelled(true);
            case LAVA: if (!mechanic.burnsInLava()) event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDisableVanillaInteraction(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        MiscMechanic mechanic = (MiscMechanic) factory.getMechanic(event.getItem());
        if (mechanic == null || !mechanic.isVanillaInteractionDisabled()) return;
        event.setCancelled(true);
    }

    private MiscMechanic getMiscMechanic(Entity entity) {
        if (!(entity instanceof Item item)) return null;
        ItemStack itemStack = item.getItemStack();
        String itemID = OraxenItems.getIdByItem(itemStack);
        if (itemID == null) return null;

        return (MiscMechanic) factory.getMechanic(itemID);
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onCompost(PlayerInteractEvent event) {
        Block block = event.getClickedBlock();
        ItemStack item = event.getItem();
        Player player = event.getPlayer();

        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getHand() == null) return;
        if (item == null || block == null || block.getType() != Material.COMPOSTER) return;
        if (!(block.getBlockData() instanceof Levelled levelled)) return;
        if (!ProtectionLib.canInteract(player, block.getLocation())) return;
        MiscMechanic mechanic = (MiscMechanic) factory.getMechanic(item);
        if (mechanic == null || !mechanic.isCompostable()) return;
        if (event.useInteractedBlock() == Event.Result.DENY) return;
        if (event.useItemInHand() == Event.Result.ALLOW) return;
        event.setUseInteractedBlock(Event.Result.ALLOW);

        if (levelled.getLevel() < levelled.getMaximumLevel()) {
            if (Math.random() <= 0.65) levelled.setLevel(levelled.getLevel() + 1); // Same as wheat
            block.setBlockData(levelled);
            block.getWorld().playEffect(block.getLocation(), Effect.COMPOSTER_FILL_ATTEMPT, 0, 1);
            if (player.getGameMode() != GameMode.CREATIVE) item.setAmount(item.getAmount() - 1);
            Utils.swingHand(player, event.getHand());
        }

    }
}
