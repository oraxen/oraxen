package io.th0rgal.oraxen.commands;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import dev.jorel.commandapi.arguments.IntegerArgument;
import dev.jorel.commandapi.arguments.TextArgument;
import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.config.Message;
import io.th0rgal.oraxen.recipes.CustomRecipe;
import io.th0rgal.oraxen.recipes.builders.FurnaceBuilder;
import io.th0rgal.oraxen.recipes.builders.RecipeBuilder;
import io.th0rgal.oraxen.recipes.builders.ShapedBuilder;
import io.th0rgal.oraxen.recipes.builders.ShapelessBuilder;
import io.th0rgal.oraxen.recipes.listeners.RecipesEventsManager;
import io.th0rgal.oraxen.utils.AdventureUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.bukkit.entity.Player;

import java.util.List;

public class RecipesCommand {
    public CommandAPICommand getRecipesCommand() {
        return new CommandAPICommand("recipes")
                .withPermission("oraxen.command.recipes")
                .withSubcommand(getShowCommand())
                .withSubcommand(getBuilderCommand())
                .withSubcommand(getSaveCommand());
    }

    private CommandAPICommand getShowCommand() {
        return new CommandAPICommand("show")
                .withPermission("oraxen.command.recipes.show")
                .withArguments(new TextArgument("type").replaceSuggestions(ArgumentSuggestions.strings(info ->
                        ArrayUtils.addAll(new String[]{"all"},
                                RecipesEventsManager.get().getPermittedRecipesName(info.sender()))))
                )
                .executes((sender, args) -> {
                    if (sender instanceof Player player) {
                        List<CustomRecipe> recipes = RecipesEventsManager.get().getPermittedRecipes(player);
                        final String param = (String) args[0];
                        if (!"all".equals(param))
                            recipes = recipes.stream().filter(customRecipe ->
                                    customRecipe.getName().equals(param)).toList();
                        if (recipes.isEmpty()) {
                            Message.RECIPE_NO_RECIPE.send(sender);
                            return;
                        }
                        OraxenPlugin.get().getInvManager().getRecipesShowcase(0, recipes).show(player);
                    } else
                        Message.NOT_PLAYER.send(sender);
                });
    }

    private CommandAPICommand getBuilderCommand() {
        return new CommandAPICommand("builder")
                .withPermission("oraxen.command.recipes.builder")
                .withSubcommand(getShapedBuilderCommand())
                .withSubcommand(getShapelessBuilderCommand())
                .withSubcommand(getFurnaceBuilderCommand())
                .executes((sender, args) -> {
                    if (sender instanceof Player player) {
                        final RecipeBuilder recipe = RecipeBuilder.get(player.getUniqueId());
                        if (recipe != null)
                            recipe.open();
                        else
                            Message.RECIPE_NO_BUILDER.send(sender);
                    } else
                        Message.NOT_PLAYER.send(sender);
                });
    }

    private CommandAPICommand getShapedBuilderCommand() {
        return new CommandAPICommand("shaped")
                .withPermission("oraxen.command.recipes.builder")
                .executes((sender, args) -> {
                    if (sender instanceof Player player) {
                        final RecipeBuilder recipe = RecipeBuilder.get(player.getUniqueId());
                        (recipe != null ? recipe : new ShapedBuilder(player)).open();
                    } else
                        Message.NOT_PLAYER.send(sender);
                });
    }

    private CommandAPICommand getShapelessBuilderCommand() {
        return new CommandAPICommand("shapeless")
                .withPermission("oraxen.command.recipes.builder")
                .executes((sender, args) -> {
                    if (sender instanceof Player player) {
                        final RecipeBuilder recipe = RecipeBuilder.get(player.getUniqueId());
                        (recipe != null ? recipe : new ShapelessBuilder(player)).open();
                    } else
                        Message.NOT_PLAYER.send(sender);
                });
    }

    private CommandAPICommand getFurnaceBuilderCommand() {
        return new CommandAPICommand("furnace")
                .withPermission("oraxen.command.recipes.builder")
                .withArguments(new IntegerArgument("cookingtime"))
                .withArguments(new IntegerArgument("experience"))
                .executes((sender, args) -> {
                    if (sender instanceof Player player) {
                        RecipeBuilder recipe = RecipeBuilder.get(player.getUniqueId());
                        recipe = recipe != null ? recipe : new FurnaceBuilder(player);
                        if (recipe instanceof FurnaceBuilder furnace) {
                            furnace.setCookingTime((Integer) args[0]);
                            furnace.setExperience((Integer) args[1]);
                        }
                        recipe.open();
                    } else
                        Message.NOT_PLAYER.send(sender);
                });
    }

    private CommandAPICommand getSaveCommand() {
        return new CommandAPICommand("save")
                .withPermission("oraxen.command.recipes.builder")
                .withArguments(new TextArgument("name"))
                .executes((sender, args) -> {
                    if (sender instanceof Player player) {
                        final RecipeBuilder recipe = RecipeBuilder.get(player.getUniqueId());
                        if (recipe == null) {
                            Message.RECIPE_NO_BUILDER.send(sender);
                            return;
                        }
                        final String name = (String) args[0];
                        recipe.saveRecipe(name);
                        Message.RECIPE_SAVE.send(sender, AdventureUtils.tagResolver("name", name));
                    } else
                        Message.NOT_PLAYER.send(sender);
                });
    }
}
