package io.th0rgal.oraxen.command.commands;

import static io.th0rgal.oraxen.language.Translations.translate;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.oraxen.chimerate.commons.command.tree.nodes.Argument;
import com.oraxen.chimerate.commons.command.tree.nodes.Literal;
import com.oraxen.chimerate.commons.command.tree.nodes.Literal.Builder;
import com.oraxen.chimerate.commons.command.types.WordType;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.command.CommandInfo;
import io.th0rgal.oraxen.command.argument.RecipeType;
import io.th0rgal.oraxen.command.condition.Conditions;
import io.th0rgal.oraxen.command.permission.OraxenPermission;
import io.th0rgal.oraxen.command.types.RecipeTypeType;
import io.th0rgal.oraxen.command.types.SpecificWordType;
import io.th0rgal.oraxen.language.DescriptionType;
import io.th0rgal.oraxen.language.Language;
import io.th0rgal.oraxen.language.LanguageProvider;
import io.th0rgal.oraxen.language.Message;
import io.th0rgal.oraxen.recipes.builders.RecipeBuilder;
import io.th0rgal.oraxen.recipes.builders.FurnaceBuilder;
import io.th0rgal.oraxen.recipes.builders.ShapelessBuilder;
import io.th0rgal.oraxen.recipes.builders.ShapedBuilder;
import io.th0rgal.oraxen.utils.general.Placeholder;
import io.th0rgal.oraxen.utils.input.InputProvider;

public class Recipe {

    public static CommandInfo build() {
        return new CommandInfo("recipe", info -> {
            Builder<CommandSender> builder = Literal.of(info.getName()).alias(info.getAliases());
            builder
                .requires(Conditions
                    .mixed(Conditions.reqPerm(OraxenPermission.COMMAND_RECIPE), Conditions.player(Message.NOT_PLAYER)))
                .then(Literal
                    .of("builder")
                    .requires(Conditions.reqPerm(OraxenPermission.COMMAND_RECIPE_EDIT))
                    .then(Argument
                        .of("type", RecipeTypeType.TYPE)
                        .optionally(Argument.of("furnace", SpecificWordType.of("cookingtime", "experience")))
                        .executes((sender, context) -> {
                            Player player = (Player) sender;
                            RecipeBuilder recipe = RecipeBuilder.get(player.getUniqueId());
                            switch (context.getArgument("type", RecipeType.class)) {
                            case SHAPED:
                                (recipe = recipe != null ? recipe : new ShapedBuilder(player)).open();
                                break;
                            case SHAPELESS:
                                (recipe = recipe != null ? recipe : new ShapelessBuilder(player)).open();
                                break;
                            case FURNACE:
                                recipe = recipe != null ? recipe : new FurnaceBuilder(player);
                                String type;
                                if ((type = context.getOptionalArgument("furnace", String.class)) != null) {
                                    if (recipe instanceof FurnaceBuilder) {
                                        FurnaceBuilder furnace = (FurnaceBuilder) recipe;
                                        InputProvider input = OraxenPlugin.get().getInputProvider();
                                        if (type.equals("cookingtime")) {
                                            if (input.hasMultipleLines()) {
                                                input
                                                    .setMessage(String
                                                        .join(InputProvider.LINE, "200", "Please enter the",
                                                            "Cooking time", "(Default is: 200)"));
                                            } else {
                                                input.setMessage("Please enter the Cooking time (Default is: 200)");
                                            }
                                            furnace.setCookingTimeProvider(input);
                                        } else {
                                            if (input.hasMultipleLines()) {
                                                input
                                                    .setMessage(String
                                                        .join(InputProvider.LINE, "200", "Please enter the",
                                                            "Experience amount", "(Default is: 200)"));
                                            } else {
                                                input
                                                    .setMessage("Please enter the Experience amount (Default is: 200)");
                                            }
                                            furnace.setExperienceProvider(input);
                                        }
                                        input.open(player);
                                    } else {
                                        Message.COMMAND_RECIPE_NO_FURNACE.send(sender);
                                    }
                                    return;
                                }
                                recipe.open();
                                break;
                            }
                        })))
                .then(Literal
                    .of("save")
                    .requires(Conditions.reqPerm(OraxenPermission.COMMAND_RECIPE_EDIT))
                    .then(Argument
                        .of("name", WordType.WORD)
                        .optionally(Argument.of("permission", WordType.WORD))
                        .executes((sender, context) -> {
                            Player player = (Player) sender;
                            RecipeBuilder recipe = RecipeBuilder.get(player.getUniqueId());
                            if (recipe == null) {
                                Message.COMMAND_RECIPE_NO_BUILDER.send(sender);
                                return;
                            }
                            String name;
                            if ((name = context.getArgument("name", String.class)) == null) {
                                Message.COMMAND_RECIPE_NO_NAME.send(sender);
                                return;
                            }
                            String permission = context.getOptionalArgument("permission", String.class);
                            if (permission == null)
                                recipe.saveRecipe(name);
                            else
                                recipe.saveRecipe(name, permission);
                            Message.COMMAND_RECIPE_SAVE.send(sender, Placeholder.of("name", name));
                        })))
                .then(Literal
                    .of("show")
                    .then(Argument
                        .of("location", SpecificWordType.of("hand", "all"))
                        .optionally(Argument.of("type", RecipeTypeType.TYPE))
                        .executes((sender, context) -> {
                            Message.WORK_IN_PROGRESS.send(sender);
                        })))
                .executes((sender, context) -> {
                    Language language = LanguageProvider.getLanguageOf(sender);
                    Message.COMMAND_HELP_INFO_DETAILED
                        .send(sender, language, new Placeholder("name", info.getName()),
                            new Placeholder("description", translate(language, info, DescriptionType.DETAILED)));
                });
            return builder;
        });
    }
}