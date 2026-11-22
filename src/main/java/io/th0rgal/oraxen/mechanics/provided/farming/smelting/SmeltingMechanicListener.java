package io.th0rgal.oraxen.mechanics.provided.farming.smelting;

import io.papermc.paper.event.server.ServerResourcesReloadedEvent;
import io.th0rgal.oraxen.api.OraxenItems;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.FurnaceRecipe;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class SmeltingMechanicListener implements Listener {

    // Plugins can add recipes at runtime without an event, so the index is also rebuilt once it gets old.
    private static final long FURNACE_RECIPES_MAX_AGE = TimeUnit.MINUTES.toNanos(1);

    private final SmeltingMechanicFactory factory;
    private volatile FurnaceRecipes furnaceRecipes;

    public SmeltingMechanicListener(SmeltingMechanicFactory factory) {
        this.factory = factory;
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();
        String itemID = OraxenItems.getIdByItem(item);

        if (factory.isNotImplementedIn(itemID)) return;
        if (player.getGameMode() == GameMode.CREATIVE || !event.isDropItems()) return;

        Block block = event.getBlock();
        List<ItemStack> drops = new ArrayList<>();
        boolean smelted = false;
        // The tool and player decide tool requirements, fortune and silk touch, like vanilla drops.
        for (ItemStack drop : block.getDrops(item, player)) {
            ItemStack loot = factory.isCookingBlacklisted(drop.getType()) ? null : furnace(drop);
            drops.add(loot == null ? drop : loot);
            smelted |= loot != null;
        }
        if (!smelted) return;

        Location location = block.getLocation().toCenterLocation();
        event.setDropItems(false);
        if (!location.isWorldLoaded()) return;
        assert location.getWorld() != null;
        for (ItemStack drop : drops) {
            // Recipe results that make several items per input can exceed one stack.
            int maxStackSize = Math.max(1, drop.getMaxStackSize());
            for (int amount = drop.getAmount(); amount > 0; amount -= maxStackSize)
                location.getWorld().dropItemNaturally(location, drop.asQuantity(Math.min(amount, maxStackSize)));
        }

        SmeltingMechanic mechanic = (SmeltingMechanic) factory.getMechanic(itemID);
        if (mechanic != null && mechanic.playSound())
            location.getWorld().playSound(location, Sound.ENTITY_GUARDIAN_ATTACK, 0.10f, 0.8f);
    }

    @EventHandler
    public void onServerResourcesReloaded(ServerResourcesReloadedEvent event) {
        furnaceRecipes = null;
    }

    private ItemStack furnace(ItemStack item) {
        for (FurnaceRecipe recipe : furnaceRecipes()) {
            if (!recipe.getInputChoice().test(item)) continue;
            ItemStack result = recipe.getResult().clone();
            result.setAmount(result.getAmount() * item.getAmount());
            return result;
        }
        return null;
    }

    // Bukkit only looks recipes up by their result, so furnace recipes are indexed to find them by input.
    private List<FurnaceRecipe> furnaceRecipes() {
        FurnaceRecipes current = furnaceRecipes;
        long now = System.nanoTime();
        if (current == null || now - current.createdAt() > FURNACE_RECIPES_MAX_AGE) {
            List<FurnaceRecipe> recipes = new ArrayList<>();
            Bukkit.recipeIterator().forEachRemaining(recipe -> {
                if (recipe instanceof FurnaceRecipe furnaceRecipe) recipes.add(furnaceRecipe);
            });
            current = new FurnaceRecipes(List.copyOf(recipes), now);
            furnaceRecipes = current;
        }
        return current.recipes();
    }

    private record FurnaceRecipes(List<FurnaceRecipe> recipes, long createdAt) {
    }

}
