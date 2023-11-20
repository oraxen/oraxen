package io.th0rgal.oraxen.utils.inventories;

import dev.triumphteam.gui.components.InteractionModifier;
import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
import dev.triumphteam.gui.guis.PaginatedGui;
import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.api.OraxenItems;
import io.th0rgal.oraxen.config.Message;
import io.th0rgal.oraxen.font.FontManager;
import io.th0rgal.oraxen.items.ItemBuilder;
import io.th0rgal.oraxen.recipes.CustomRecipe;
import io.th0rgal.oraxen.utils.AdventureUtils;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class RecipesView {

    private final FontManager fontManager = OraxenPlugin.get().fontManager();
    final String menuTexture = fontManager.getShift(-7) + fontManager.getGlyphFromName("menu_recipe").character();

    public PaginatedGui create(final int page, final List<CustomRecipe> filteredRecipes) {

        final PaginatedGui gui = Gui.paginated().rows(6).title(AdventureUtils.MINI_MESSAGE.deserialize(menuTexture)).create();
        final CustomRecipe currentRecipe = filteredRecipes.get(page);

        // Check if last page
        final boolean lastPage = filteredRecipes.size() - 1 == page;
        PaginatedGui paginatedGui = new PaginatedGui(6, 54, "", Arrays.stream(InteractionModifier.values()).collect(Collectors.toSet()));
        paginatedGui.setItem(4, 0, new GuiItem(currentRecipe.getResult()));

        for (int i = 0; i < currentRecipe.getIngredients().size(); i++) {
            final ItemStack itemStack = currentRecipe.getIngredients().get(i);
            if (itemStack != null && !itemStack.getType().isAir())
                paginatedGui.setItem(3 + i % 3, 2 + i / 3, new GuiItem(itemStack));
        }

        // Close RecipeShowcase inventory button
        paginatedGui.setItem(4, 5, new GuiItem((OraxenItems.getItemById("exit_icon") == null
                ? new ItemBuilder(Material.BARRIER)
                : OraxenItems.getItemById("exit_icon"))
                .setDisplayName(Message.EXIT_MENU.toSerializedString()).build(),
                (event -> event.getWhoClicked().closeInventory())));

        // Previous Page button
        if (page > 0)
            paginatedGui.setItem(1, 3, new GuiItem((OraxenItems.getItemById("arrow_previous_icon") == null
                    ? new ItemBuilder(Material.ARROW) : OraxenItems.getItemById("arrow_previous_icon"))
                    .setAmount(page).setDisplayName(ChatColor.YELLOW + "Open page " + page)
                    .build(), event -> create(page - 1, filteredRecipes).open(event.getWhoClicked())));


        // Next page button
        if (!lastPage)
            paginatedGui.setItem(7, 3, new GuiItem((OraxenItems.getItemById("arrow_next_icon") == null
                    ? new ItemBuilder(Material.ARROW) : OraxenItems.getItemById("arrow_next_icon"))
                    .setAmount(page + 2).setDisplayName(ChatColor.YELLOW + "Open page " + (page + 2))
                    .build(), event -> create(page + 1, filteredRecipes).open(event.getWhoClicked())));

        return gui;
    }

}
