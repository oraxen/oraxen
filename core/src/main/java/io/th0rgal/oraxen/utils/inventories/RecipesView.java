package io.th0rgal.oraxen.utils.inventories;

import com.github.stefvanschie.inventoryframework.gui.GuiItem;
import com.github.stefvanschie.inventoryframework.gui.type.ChestGui;
import com.github.stefvanschie.inventoryframework.pane.StaticPane;
import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.api.OraxenItems;
import io.th0rgal.oraxen.config.Message;
import io.th0rgal.oraxen.font.FontManager;
import io.th0rgal.oraxen.items.ItemBuilder;
import io.th0rgal.oraxen.recipes.CustomRecipe;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class RecipesView {

    private final FontManager fontManager = OraxenPlugin.get().getFontManager();
    final String menuTexture = ChatColor.WHITE +
            fontManager.getShift(-7) +
            fontManager.getGlyphFromName("menu_recipe").getCharacter();

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
        pane.addItem(new GuiItem((OraxenItems.getItemById("exit_icon") == null
                ? new ItemBuilder(Material.BARRIER)
                : OraxenItems.getItemById("exit_icon"))
                .setDisplayName(Message.EXIT_MENU).build(),
                (event -> event.getWhoClicked().closeInventory())), 4, 5);

        // Previous Page button
        if (page > 0)
            pane.addItem(new GuiItem((OraxenItems.getItemById("arrow_previous_icon") == null
                    ? new ItemBuilder(Material.ARROW)
                    : OraxenItems.getItemById("arrow_previous_icon"))
                    .setAmount(page)
                    .setDisplayName(ChatColor.YELLOW + "Open page " + page)
                    .build(),
                    event -> create(page - 1,
                            filteredRecipes).show(event.getWhoClicked())),
                    1, 3);

        // Next page button
        if (!lastPage)
            pane.addItem(new GuiItem((OraxenItems.getItemById("arrow_next_icon") == null
                    ? new ItemBuilder(Material.ARROW)
                    : OraxenItems.getItemById("arrow_next_icon"))
                    .setAmount(page + 2)
                    .setDisplayName(ChatColor.YELLOW + "Open page " + (page + 2))
                    .build(),
                    event -> create(page + 1, filteredRecipes)
                            .show(event.getWhoClicked())),
                    7, 3);

        gui.addPane(pane);
        gui.setOnGlobalClick(event -> event.setCancelled(true));
        return gui;
    }

}
