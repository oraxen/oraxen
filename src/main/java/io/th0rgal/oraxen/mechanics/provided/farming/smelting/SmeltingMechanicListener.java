package io.th0rgal.oraxen.mechanics.provided.farming.smelting;

import io.th0rgal.oraxen.items.OraxenItems;
import io.th0rgal.oraxen.mechanics.MechanicFactory;
import org.bukkit.*;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.CookingRecipe;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Collection;
import java.util.concurrent.ThreadLocalRandom;

public class SmeltingMechanicListener implements Listener {

    private final MechanicFactory factory;

    public SmeltingMechanicListener(MechanicFactory factory) {
        this.factory = factory;
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onBlockBreak(BlockBreakEvent event) {
        if (event.isCancelled())
            return;
        ItemStack item = event.getPlayer().getInventory().getItemInMainHand();

        String itemID = OraxenItems.getIdByItem(item);
        if (factory.isNotImplementedIn(itemID))
            return;

        if (event.getPlayer().getGameMode() == GameMode.CREATIVE)
            return;

        Collection<ItemStack> itemStacks = event.getBlock().getDrops();
        if (itemStacks.isEmpty())
            return;
        ItemStack loot = furnace(itemStacks.stream().findAny().orElse(null));
        if (loot == null)
            return; // not recipe
        ItemMeta itemMeta = item.getItemMeta();
        if (itemMeta == null) return;

        if (event.getBlock().getType().toString().contains("ORE")
                && itemMeta.hasEnchant(Enchantment.LOOT_BONUS_BLOCKS)) {
            loot.setAmount(1 + ThreadLocalRandom.current().nextInt(itemMeta.getEnchantLevel(Enchantment.LOOT_BONUS_BLOCKS)));
        }
        event.setDropItems(false);
        Location location = event.getBlock().getLocation().add(0, 0.5, 0);
        if (!location.isWorldLoaded()) return;
        location.getWorld().dropItemNaturally(location, loot);
        SmeltingMechanic mechanic = (SmeltingMechanic) factory.getMechanic(itemID);
        if (mechanic.playSound()) {
            location.getWorld().playSound(location, Sound.ENTITY_GUARDIAN_ATTACK, 0.10f, 0.8f);
        }
    }

    private ItemStack furnace(ItemStack item) {
        if (item == null)
            return null; // Because item can be null
        String type = item.getType().toString();
        if (type.startsWith("RAW_") && !type.endsWith("_BLOCK")) {
            item.setType(Material.valueOf(item.getType().toString().substring(4) + "_INGOT"));
            return item;
        }
        for (Recipe recipe : Bukkit.getRecipesFor(item)) {
            if (!(recipe instanceof CookingRecipe<?> cookingRecipe))
                continue;
            if (cookingRecipe.getInputChoice().test(item))
                return new ItemStack(recipe.getResult().getType(), item.getAmount());
        }
        return null; // return result furnace :)
    }

}
