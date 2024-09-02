package io.th0rgal.oraxen.utils.inventories;

import com.github.stefvanschie.inventoryframework.gui.GuiItem;
import com.github.stefvanschie.inventoryframework.gui.type.ChestGui;
import com.github.stefvanschie.inventoryframework.pane.StaticPane;
import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.api.OraxenItems;
import io.th0rgal.oraxen.config.Message;
import io.th0rgal.oraxen.font.FontManager;
import io.th0rgal.oraxen.font.Shift;
import io.th0rgal.oraxen.items.ItemBuilder;
import io.th0rgal.oraxen.recipes.CustomRecipe;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class RecipesView {

    private final FontManager fontManager = OraxenPlugin.get().fontManager();
    final String menuTexture = String.format("%s%s%s", ChatColor.WHITE, Shift.of(-7), fontManager.getGlyphFromName("menu_recipe").character());

    public ChestGui create(final int page, final List<CustomRecipe> filteredRecipes) {
        final ChestGui gui = new ChestGui(6, menuTexture);

        final CustomRecipe currentRecipe = filteredRecipes.get(page);

        // Check if last page
        final boolean lastPage = filteredRecipes.size() - 1 == page;
        final StaticPane pane = new StaticPane(9, 6);
        pane.addItem(new GuiItem(currentRecipe.getResult()), 4, 0);

        for (int i = 0; i < currentRecipe.getIngredients().size(); i++) {
            final ItemStack itemStack = currentRecipe.getIngredients().get(i);
            if (itemStack != null && itemStack.getType() != Material.AIR)
                pane.addItem(new GuiItem(itemStack), 3 + i % 3, 2 + i / 3);
        }

        // Close RecipeShowcase inventory button
        ItemBuilder exitBuilder = OraxenItems.getOptionalItemById("exit_icon").orElse(new ItemBuilder(Material.BARRIER));
        ItemStack exitItem = exitBuilder.displayName(Message.EXIT_MENU.toComponent()).build();
        pane.addItem(new GuiItem(exitItem, event -> event.getWhoClicked().closeInventory()), 4, 5);

        // Previous Page button
        if (page > 0) {
            ItemBuilder builder = OraxenItems.getOptionalItemById("arrow_previous_icon").orElse(new ItemBuilder(Material.ARROW));
            ItemStack guiItem = builder.setAmount(page).displayName(Component.text("Open page " + page, NamedTextColor.YELLOW)).build();
            pane.addItem(new GuiItem(guiItem, e -> create(page - 1, filteredRecipes).show(e.getWhoClicked())), 1, 3);
        }

        // Next page button
        if (!lastPage) {
            ItemBuilder builder = OraxenItems.getOptionalItemById("arrow_next_icon").orElse(new ItemBuilder(Material.ARROW));
            ItemStack guiItem = builder.setAmount(page + 2).displayName(Component.text("Open page " + (page + 2), NamedTextColor.YELLOW)).build();
            pane.addItem(new GuiItem(guiItem, e -> create(page + 1, filteredRecipes).show(e.getWhoClicked())), 7, 3);
        }

        gui.addPane(pane);
        gui.setOnGlobalClick(event -> event.setCancelled(true));
        return gui;
    }

}
