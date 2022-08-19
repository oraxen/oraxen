package io.th0rgal.oraxen.mechanics.provided.gameplay.mushroomstem.logstrip;

import com.destroystokyo.paper.MaterialTags;
import io.th0rgal.oraxen.items.OraxenItems;
import io.th0rgal.oraxen.mechanics.MechanicFactory;
import io.th0rgal.oraxen.mechanics.provided.gameplay.mushroomstem.MushroomStemMechanic;
import io.th0rgal.oraxen.mechanics.provided.gameplay.mushroomstem.MushroomStemMechanicFactory;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;

import static io.th0rgal.oraxen.mechanics.provided.gameplay.mushroomstem.MushroomStemMechanicFactory.getMushroomStemMechanic;

public class LogStripListener implements Listener {

    private final MechanicFactory factory;

    public LogStripListener(MechanicFactory factory) {
        this.factory = factory;
    }

    @EventHandler
    public void onStrippingLog(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        Block block = event.getClickedBlock();
        ItemStack item = player.getInventory().getItemInMainHand();
        ItemMeta itemMeta = item.getItemMeta();

        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || block == null || block.getType() != Material.MUSHROOM_STEM)
            return;
        if (MaterialTags.AXES.isTagged(item)) {
            MushroomStemMechanic mechanic = getMushroomStemMechanic(block);
            if (mechanic == null) return;

            if (mechanic.isDirectional() && !mechanic.isLog())
                mechanic = (MushroomStemMechanic) factory.getMechanic(mechanic.getDirectional().getParentBlock());

            LogStripping log = mechanic.getLog();
            if (!mechanic.isLog() || !log.canBeStripped()) return;

            if (log.hasStrippedDrop()) {
                player.getWorld().dropItemNaturally(
                        block.getRelative(player.getFacing().getOppositeFace()).getLocation(),
                        OraxenItems.getItemById(log.getStrippedLogDrop()).build());
            }

            if (log.shouldDecreaseAxeDurability() && player.getGameMode() != GameMode.CREATIVE) {
                if (itemMeta instanceof Damageable axeDurabilityMeta) {
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
            }

            MushroomStemMechanicFactory.setBlockModel(block, log.getStrippedLogBlock());
            player.playSound(block.getLocation(), Sound.ITEM_AXE_STRIP, 1.0f, 0.8f);
        }
    }
}
