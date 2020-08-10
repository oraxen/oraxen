package io.th0rgal.oraxen.command.commands;

import static io.th0rgal.oraxen.language.Translations.translate;

import org.bukkit.command.CommandSender;

import com.oraxen.chimerate.commons.command.tree.nodes.Argument;
import com.oraxen.chimerate.commons.command.tree.nodes.Literal;
import com.oraxen.chimerate.commons.command.tree.nodes.Literal.Builder;

import io.th0rgal.oraxen.command.CommandInfo;
import io.th0rgal.oraxen.command.CommandProvider;
import io.th0rgal.oraxen.command.condition.Conditions;
import io.th0rgal.oraxen.command.permission.OraxenPermission;
import io.th0rgal.oraxen.command.types.RecipeTypeType;
import io.th0rgal.oraxen.command.types.SpecificWordType;
import io.th0rgal.oraxen.language.DescriptionType;
import io.th0rgal.oraxen.language.Language;
import io.th0rgal.oraxen.language.LanguageProvider;
import io.th0rgal.oraxen.language.Message;
import io.th0rgal.oraxen.utils.general.Placeholder;

public class Recipes {

    public static CommandInfo build() {
        return new CommandInfo("recipe", info -> {
            Builder<CommandSender> builder = Literal.of(info.getName()).alias(info.getAliases());
            builder
                .requires(Conditions
                    .mixed(Conditions.reqPerm(OraxenPermission.COMMAND_RECIPE), Conditions.player(Message.NOT_PLAYER)))
                .then(Literal
                    .of("builder")
                    .then(Argument
                        .of("type", RecipeTypeType.TYPE)
                        .optionally(Argument.of("furnace", SpecificWordType.of("cookingtime", "experience")))
                        .executes((sender, context) -> {
                            // Builder
                        })))
                .then(Literal.of("save").executes((sender, context) -> {
                    // Save
                }))
                .then(Literal
                    .of("show")
                    .then(Argument
                        .of("location", SpecificWordType.of("hand", "all"))
                        .optionally(Argument.of("type", RecipeTypeType.TYPE))
                        .executes((sender, context) -> {
                            // Show
                        })))
                .executes((sender, context) -> {
                    Language language = LanguageProvider.getLanguageOf(sender);
                    Message.COMMAND_HELP_INFO_DETAILED.send(sender, language, new Placeholder("name", info.getName()),
                        new Placeholder("decription", translate(language, info, DescriptionType.DETAILED)));
                });
            return builder;
        });
    }

}
