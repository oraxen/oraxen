package io.th0rgal.oraxen.utils.inventories;

import com.github.stefvanschie.inventoryframework.gui.GuiItem;
import com.github.stefvanschie.inventoryframework.gui.type.ChestGui;
import com.github.stefvanschie.inventoryframework.pane.StaticPane;
import io.th0rgal.oraxen.items.ItemBuilder;
import io.th0rgal.oraxen.items.OraxenItems;
import io.th0rgal.oraxen.recipes.CustomRecipe;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Optional;

public class RecipesView {

    public ChestGui create(final int page, final List<CustomRecipe> filteredRecipes) {
        final ChestGui gui = new ChestGui(6, filteredRecipes.get(page).getResult().getItemMeta().getDisplayName()
                + (filteredRecipes.get(page).isOrdered() ? "" : "  (Shapeless)"));

        final CustomRecipe currentRecipe = filteredRecipes.get(page);

        // Check if last page
        final boolean lastPage = filteredRecipes.size() - 1 == page;
        final StaticPane pane = new StaticPane(9, 6);
        pane.addItem(new GuiItem(new ItemBuilder(currentRecipe.getResult()).build()), 4, 0);

        final Optional<ItemBuilder> background = OraxenItems.getOptionalItemById("recipe_showcase");
        background.ifPresent(itemBuilder -> pane.addItem(new GuiItem(itemBuilder.build()), 0, 5));

        final StaticPane ingredientsPane = new StaticPane(3, 3, 3, 3);
        for (int i = 0; i < currentRecipe.getIngredients().size(); i++) {
            final ItemStack itemStack = currentRecipe.getIngredients().get(i);
            if (itemStack != null && itemStack.getType() != Material.AIR)
                ingredientsPane.addItem(new GuiItem(itemStack), i % 3, i / 3 - 1);
        }

        // Close RecipeShowcase inventory button
        pane.addItem(new GuiItem(new ItemBuilder(Material.BARRIER).setDisplayName(ChatColor.RED + "Close").build(),
                (event -> event.getWhoClicked().closeInventory())), 4, 5);

        // Previous Page button
        if (page > 0)
            pane.addItem(new GuiItem((OraxenItems.getItemById("arrow_previous_icon") == null
                    ? new ItemBuilder(Material.ARROW)
                    : OraxenItems.getItemById("arrow_previous_icon"))
                    .setAmount(page)
                    .setDisplayName(ChatColor.YELLOW + "Open page " + page)
                    .build(), event -> create(page - 1,
                    filteredRecipes).show((Player) event.getWhoClicked())), 1, 3);


        // Next page button
        if (!lastPage)
            pane.addItem(new GuiItem((OraxenItems.getItemById("arrow_next_icon") == null
                    ? new ItemBuilder(Material.ARROW)
                    : OraxenItems.getItemById("arrow_next_icon"))
                    .setAmount(page + 2)
                    .setDisplayName(ChatColor.YELLOW + "Open page " + (page + 2))
                    .build(), event ->
                    create(page + 1, filteredRecipes)
                            .show((Player) event.getWhoClicked())), 7, 3);

        gui.addPane(pane);
        gui.addPane(ingredientsPane);
        gui.setOnGlobalClick(event -> event.setCancelled(true));
        return gui;
    }

}
