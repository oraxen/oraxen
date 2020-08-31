package io.th0rgal.oraxen.utils.recipeshowcase;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.items.ItemBuilder;
import io.th0rgal.oraxen.recipes.CustomRecipe;
import io.th0rgal.oraxen.utils.fastinv.FastInv;
import java.util.ArrayList;
import java.util.Optional;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;

public class RecipeShowcase extends FastInv {


  public RecipeShowcase(int page, ArrayList<CustomRecipe> filteredRecipes) {
    super(6 * 9,
        filteredRecipes.get(page).getResult().getItemMeta().getDisplayName() + (filteredRecipes.get(page).isOrdered() ? "" : "  (Shapeless)"));

    //Current Recipe
    CustomRecipe currentRecipe = filteredRecipes.get(page);

    //Check if last page
    boolean lastPage = false;
    if (filteredRecipes.size() - 1 == page) {
      lastPage = true;
    }

    //Add Crafting border
    fillRect(1,3,5,7,
        new ItemBuilder(Material.WHITE_STAINED_GLASS_PANE).setDisplayName("BORDER").build());

    //Display Result
    setItem(4, new ItemBuilder(currentRecipe.getResult()).build());

    //Display Recipe
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

    //Close RecipeShowcase inventory button
    setItem(getInventory().getSize() - 2,
        new ItemBuilder(Material.BARRIER).setDisplayName(ChatColor.RED + "Close").build(),
        e -> e.getWhoClicked().closeInventory());

    //Previous Page button
    if (page > 0)
      setItem(getInventory().getSize() - 9,
          new ItemBuilder(Material.ARROW)
              .setAmount(page)
              .setDisplayName(ChatColor.YELLOW + "Open page " + page)
              .build(),
          e -> new RecipeShowcase(page - 1, filteredRecipes).open((Player) e.getWhoClicked()));

    //Next page button
    if (!lastPage)
      setItem(getInventory().getSize() - 1,
          new ItemBuilder(Material.ARROW)
              .setAmount(page + 2)
              .setDisplayName(ChatColor.YELLOW + "Open page " + (page + 2))
              .build(),
          e -> new RecipeShowcase(page + 1, filteredRecipes).open((Player) e.getWhoClicked()));
  }

  private void fillRect(int fromRow, int fromColumn, int toRow, int toColumn, ItemStack item) {
    for(int row = fromRow; row <= toRow; row++) {
      for(int column = fromColumn; column <= toColumn; column++) {
        if(row != fromRow && row != toRow && column != fromColumn && column != toColumn)
          continue;

        setItem(calcSlot(row,column), item);
      }
    }
  }

  private int calcSlot(int row, int column) {
    return ((column + 1) - 1) + ((row * 9) - 1);
  }

  private void setRecipeSlot(ItemStack itemStack, int slot,
      ArrayList<CustomRecipe> filteredRecipes) {
    //Check for recipe
    ArrayList<Recipe> recipes = (ArrayList<Recipe>) OraxenPlugin.get().getServer().getRecipesFor(itemStack);
    Optional<CustomRecipe> customRecipe = Optional.empty();
    if (recipes.size() > 0) {
      customRecipe = Optional.ofNullable(CustomRecipe.fromRecipe(recipes.get(0)));
    }

    //Get Page
    int page = -1;
    if (customRecipe.isPresent()) {
      page = filteredRecipes.indexOf(customRecipe.get());
    }

    //Set Recipe Item
    if (page != -1) {
      int finalPage = page;
      setItem(slot, new ItemBuilder(itemStack).build(),
          e -> new RecipeShowcase(finalPage, filteredRecipes).open((Player) e.getWhoClicked()));
    } else {
      setItem(slot, itemStack);
    }


  }
}
