package io.th0rgal.oraxen.commands.recipeshowcase;

import io.th0rgal.oraxen.items.ItemBuilder;
import io.th0rgal.oraxen.items.OraxenItems;
import io.th0rgal.oraxen.recipes.CustomRecipe;
import io.th0rgal.oraxen.utils.fastinv.FastInv;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class RecipeShowcase extends FastInv {

    public RecipeShowcase(int page, List<CustomRecipe> filteredRecipes) {
        super(6 * 9, filteredRecipes.get(page).getResult().getItemMeta().getDisplayName()
                + (filteredRecipes.get(page).isOrdered() ? "" : "  (Shapeless)"));

        // Current Recipe
        CustomRecipe currentRecipe = filteredRecipes.get(page);

        // Check if last page
        boolean lastPage = filteredRecipes.size() - 1 == page;

        // Display Result
        setItem(4, new ItemBuilder(currentRecipe.getResult()).build());

        Optional<ItemBuilder> background = OraxenItems.getOptionalItemById("recipe_showcase");
        if (background.isPresent()) {
            // Add Background
            setItem(45, background.get().build());
        } else {
            // Add Crafting border
            fillRect(1, 3, 5, 7, new ItemBuilder(Material.WHITE_STAINED_GLASS_PANE).setDisplayName("BORDER").build());
        }

        // Display Recipe
        int slot = 21;
        int i = 0;
        for (ItemStack itemStack : currentRecipe.getIngredients()) {
            if (itemStack != null && itemStack.getType() != Material.AIR)
                setRecipeSlot(itemStack, slot, filteredRecipes);
            i++;
            if (i > 2) {
                i = 0;
                slot += 6;
            }
            slot++;
        }

        // Close RecipeShowcase inventory button
        setItem(49, new ItemBuilder(Material.BARRIER).setDisplayName(ChatColor.RED + "Close").build(),
                e -> e.getWhoClicked().closeInventory());

        // Previous Page button
        if (page > 0)
            setItem(28,
                    (OraxenItems.getItemById("arrow_previous_icon") == null
                            ? new ItemBuilder(Material.ARROW)
                            : OraxenItems.getItemById("arrow_previous_icon"))
                            .setAmount(page)
                            .setDisplayName(ChatColor.YELLOW + "Open page " + page)
                            .build(),
                    e -> new RecipeShowcase(page - 1, filteredRecipes).open((Player) e.getWhoClicked()));

        // Next page button
        if (!lastPage)
            setItem(34,
                    (OraxenItems.getItemById("arrow_next_icon") == null
                            ? new ItemBuilder(Material.ARROW)
                            : OraxenItems.getItemById("arrow_next_icon"))
                            .setAmount(page + 2)
                            .setDisplayName(ChatColor.YELLOW + "Open page " + (page + 2))
                            .build(),
                    e -> new RecipeShowcase(page + 1, filteredRecipes).open((Player) e.getWhoClicked()));
    }

    private void fillRect(int fromRow, int fromColumn, int toRow, int toColumn, ItemStack item) {
        for (int row = fromRow; row <= toRow; row++) {
            for (int column = fromColumn; column <= toColumn; column++) {
                if (row != fromRow && row != toRow && column != fromColumn && column != toColumn)
                    continue;

                setItem(calcSlot(row, column), item);
            }
        }
    }

    private int calcSlot(int row, int column) {
        return ((column + 1) - 1) + ((row * 9) - 1);
    }

    private void setRecipeSlot(ItemStack itemStack, int slot, List<CustomRecipe> filteredRecipes) {
        int page = -1;

        // Check for recipe
        for (int i = 0; i < filteredRecipes.size(); i++) {
            String currentItemId = OraxenItems.getIdByItem(itemStack);
            String recipeItemId = OraxenItems.getIdByItem(filteredRecipes.get(i).getResult());
            if (Objects.nonNull(currentItemId) && Objects.nonNull(recipeItemId) && currentItemId.equals(recipeItemId)) {
                page = i;
                break;
            }
        }

        // Set Recipe Item
        if (page != -1) {
            int finalPage = page;
            setItem(slot, new ItemBuilder(itemStack).build(),
                    e -> new RecipeShowcase(finalPage, filteredRecipes).open((Player) e.getWhoClicked()));
        } else {
            setItem(slot, itemStack);
        }

    }
}
