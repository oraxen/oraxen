package io.th0rgal.oraxen.mechanics.provided.misc.misc;

import io.th0rgal.oraxen.api.OraxenItems;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByBlockEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;

import static org.bukkit.event.entity.EntityDamageEvent.DamageCause.FIRE;
import static org.bukkit.event.entity.EntityDamageEvent.DamageCause.FIRE_TICK;

public class MiscListener implements Listener {

    private final MiscMechanicFactory factory;

    public MiscListener(MiscMechanicFactory factory) {
        this.factory = factory;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onStripLog(PlayerInteractEvent event) {
        Block block = event.getClickedBlock();
        ItemStack item = event.getItem();
        Player player = event.getPlayer();

        if (block == null || !Tag.LOGS.isTagged(block.getType())) return;
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || item == null) return;
        if (!(factory.getMechanic(OraxenItems.getIdByItem(item)) instanceof MiscMechanic mechanic)) return;
        if (!mechanic.canStripLogs()) return;

        if (item.getItemMeta() instanceof Damageable axeDurabilityMeta) {
            int durability = axeDurabilityMeta.getDamage();
            int maxDurability = item.getType().getMaxDurability();

            if (durability + 1 <= maxDurability) {
                axeDurabilityMeta.setDamage(durability + 1);
                item.setItemMeta(axeDurabilityMeta);
            } else {
                player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 1, 1);
                item.setType(Material.AIR);
            }
        }
        block.setType(getStrippedLog(block.getType()));
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
        MiscMechanic mechanic = (MiscMechanic) factory.getMechanic(OraxenItems.getIdByItem(event.getItem()));
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

    private Material getStrippedLog(Material log) {
        return switch (log) {
            case OAK_LOG -> Material.STRIPPED_OAK_LOG;
            case SPRUCE_LOG -> Material.STRIPPED_SPRUCE_LOG;
            case BIRCH_LOG -> Material.STRIPPED_BIRCH_LOG;
            case JUNGLE_LOG -> Material.STRIPPED_JUNGLE_LOG;
            case ACACIA_LOG -> Material.STRIPPED_ACACIA_LOG;
            case DARK_OAK_LOG -> Material.STRIPPED_DARK_OAK_LOG;
            case CRIMSON_STEM -> Material.STRIPPED_CRIMSON_STEM;
            case WARPED_STEM -> Material.STRIPPED_WARPED_STEM;
            case MANGROVE_LOG -> Material.STRIPPED_MANGROVE_LOG;

            case OAK_WOOD -> Material.STRIPPED_OAK_WOOD;
            case SPRUCE_WOOD -> Material.STRIPPED_SPRUCE_WOOD;
            case BIRCH_WOOD -> Material.STRIPPED_BIRCH_WOOD;
            case JUNGLE_WOOD -> Material.STRIPPED_JUNGLE_WOOD;
            case ACACIA_WOOD -> Material.STRIPPED_ACACIA_WOOD;
            case DARK_OAK_WOOD -> Material.STRIPPED_DARK_OAK_WOOD;
            case CRIMSON_HYPHAE -> Material.STRIPPED_CRIMSON_HYPHAE;
            case WARPED_HYPHAE -> Material.STRIPPED_WARPED_HYPHAE;
            case MANGROVE_WOOD -> Material.STRIPPED_MANGROVE_WOOD;

            default -> null;
        };
    }
}
