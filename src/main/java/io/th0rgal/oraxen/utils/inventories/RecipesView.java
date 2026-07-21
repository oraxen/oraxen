package io.th0rgal.oraxen.utils.inventories;

import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.api.OraxenItems;
import io.th0rgal.oraxen.configs.Message;
import io.th0rgal.oraxen.fonts.FontManager;
import io.th0rgal.oraxen.items.ItemBuilder;
import io.th0rgal.oraxen.recipes.CustomRecipe;
import io.th0rgal.oraxen.utils.AdventureUtils;
import io.th0rgal.oraxen.utils.logs.Logs;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class RecipesView {

    private final FontManager fontManager = OraxenPlugin.get().getFontManager();
    final String menuTexture = ChatColor.WHITE +
            fontManager.getShift(-7) +
            fontManager.getGlyphFromName("menu_recipe").getCharacter();

    public Gui create(final int page, final List<CustomRecipe> filteredRecipes) {
        final Gui gui = Gui.gui().rows(6).title(AdventureUtils.LEGACY_SERIALIZER.deserialize(menuTexture)).create();
        gui.setDefaultClickAction(event -> event.setCancelled(true));
        gui.setPlayerInventoryAction(event -> event.setCancelled(true));
        gui.setOutsideClickAction(event -> event.setCancelled(true));
        gui.setDragAction(event -> event.setCancelled(true));
        if (filteredRecipes.isEmpty()) return gui;

        final int currentPage = Math.max(0, Math.min(page, filteredRecipes.size() - 1));
        final CustomRecipe currentRecipe = filteredRecipes.get(currentPage);

        // Check if last page
        final boolean lastPage = filteredRecipes.size() - 1 == currentPage;
        final ItemStack result = currentRecipe.getResult();
        if (result != null) {
            gui.setItem(1, 5, new GuiItem(result));
        } else {
            Logs.logWarning("Recipe " + currentRecipe.getName() + " has no result and cannot be displayed in the recipe viewer.");
        }

        for (int i = 0; i < currentRecipe.getIngredients().size(); i++) {
            final ItemStack itemStack = currentRecipe.getIngredients().get(i);
            if (itemStack != null && itemStack.getType() != Material.AIR)
                gui.setItem(3 + i / 3, 4 + i % 3, new GuiItem(itemStack));
        }

        // Close RecipeShowcase inventory button
        gui.setItem(6, 5, new GuiItem(iconOrDefault("exit_icon", Material.BARRIER)
                .setDisplayName(Message.EXIT_MENU).build(),
                (event -> event.getWhoClicked().closeInventory())));

        // Previous Page button
        if (currentPage > 0)
            gui.setItem(4, 2, new GuiItem(iconOrDefault("arrow_previous_icon", Material.ARROW)
                    .setDisplayName(pageName(currentPage))
                    .build(),
                    event -> create(currentPage - 1,
                            filteredRecipes).open(event.getWhoClicked())));

        // Next page button
        if (!lastPage)
            gui.setItem(4, 8, new GuiItem(iconOrDefault("arrow_next_icon", Material.ARROW)
                    .setDisplayName(pageName(currentPage + 2))
                    .build(),
                    event -> create(currentPage + 1, filteredRecipes)
                            .open(event.getWhoClicked())));

        return gui;
    }

    private ItemBuilder iconOrDefault(String itemId, Material fallback) {
        ItemBuilder icon = OraxenItems.getItemById(itemId);
        return icon != null ? icon : new ItemBuilder(fallback);
    }

    private String pageName(int page) {
        return ChatColor.YELLOW + "Page " + page;
    }

}
