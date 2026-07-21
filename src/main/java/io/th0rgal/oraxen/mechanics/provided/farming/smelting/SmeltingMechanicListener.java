package io.th0rgal.oraxen.mechanics.provided.farming.smelting;

import io.th0rgal.oraxen.api.OraxenItems;
import io.th0rgal.oraxen.mechanics.MechanicFactory;
import io.th0rgal.oraxen.utils.BlockHelpers;
import io.th0rgal.oraxen.utils.wrappers.EnchantmentWrapper;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
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

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();
        Block block = event.getBlock();
        Location location = BlockHelpers.toCenterLocation(block.getLocation());
        Collection<ItemStack> itemStacks = event.getBlock().getDrops();
        String itemID = OraxenItems.getIdByItem(item);

        if (itemStacks.isEmpty()) return;
        if (factory.isNotImplementedIn(itemID)) return;
        if (player.getGameMode() == GameMode.CREATIVE) return;

        ItemStack loot = furnace(itemStacks.stream().findAny().orElse(null));
        if (loot == null) return; // not recipe

        ItemMeta itemMeta = item.getItemMeta();
        if (!item.hasItemMeta()) return;
        assert itemMeta != null;
        if (block.getType().toString().contains("ORE") && itemMeta.hasEnchant(EnchantmentWrapper.FORTUNE)) {
            loot.setAmount(1 + ThreadLocalRandom.current().nextInt(itemMeta.getEnchantLevel(EnchantmentWrapper.FORTUNE)));
        }

        event.setDropItems(false);
        if (!location.isWorldLoaded()) return;
        assert location.getWorld() != null;
        location.getWorld().dropItemNaturally(location, loot);

        SmeltingMechanic mechanic = (SmeltingMechanic) factory.getMechanic(itemID);
        if (mechanic != null && mechanic.playSound())
            location.getWorld().playSound(location, Sound.ENTITY_GUARDIAN_ATTACK, 0.10f, 0.8f);
    }

    private ItemStack furnace(ItemStack item) {
        if (item == null)
            return null; // Because item can be null
        String type = item.getType().toString();
        if (type.startsWith("RAW_") && !type.endsWith("_BLOCK")) {
            Material smeltedMaterial = Material.matchMaterial(item.getType().toString().substring(4) + "_INGOT");
            if (smeltedMaterial == null) return null;
            item.setType(smeltedMaterial);
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
