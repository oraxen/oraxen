package io.th0rgal.oraxen.mechanics.provided.smelting;

import io.th0rgal.oraxen.items.OraxenItems;
import io.th0rgal.oraxen.mechanics.MechanicFactory;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Random;

public class SmeltingMechanicListener implements Listener {

    private final MechanicFactory factory;

    public SmeltingMechanicListener(MechanicFactory factory) {
        this.factory = factory;
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onBlockbreak(BlockBreakEvent event) {

        ItemStack item = event.getPlayer().getInventory().getItemInMainHand();
        if (item == null)
            return;

        String itemID = OraxenItems.getIdByItem(item);
        if (factory.isNotImplementedIn(itemID))
            return;

        Material type = event.getBlock().getType();
        ItemStack loot;
        if (type == Material.IRON_ORE) {
            loot = new ItemStack(Material.IRON_INGOT);
        } else if (type == Material.GOLD_ORE) {
            loot = new ItemStack(Material.GOLD_INGOT);
        } else {
            return;
        }

        ItemMeta itemMeta = item.getItemMeta();
        if (item.getItemMeta().hasEnchant(Enchantment.LOOT_BONUS_BLOCKS)) {
            loot.setAmount(1 + new Random().nextInt(item.getItemMeta().getEnchantLevel(Enchantment.LOOT_BONUS_BLOCKS)));
        }
        event.setDropItems(false);
        Location location = event.getBlock().getLocation();
        location.getWorld().dropItemNaturally(location, loot);
        SmeltingMechanic mechanic = (SmeltingMechanic) factory.getMechanic(itemID);
        if (mechanic.playSound()) {
            location.getWorld().playSound(location, Sound.ENTITY_GUARDIAN_ATTACK, 0.10f, 0.8f);
        }
    }

}
