package io.th0rgal.oraxen.command.commands;

import static io.th0rgal.oraxen.command.argument.ArgumentHelper.get;

import com.syntaxphoenix.syntaxapi.command.Arguments;
import com.syntaxphoenix.syntaxapi.command.DefaultCompletion;
import io.th0rgal.oraxen.command.CommandInfo;
import io.th0rgal.oraxen.command.MinecraftInfo;
import io.th0rgal.oraxen.command.OraxenCommand;
import io.th0rgal.oraxen.command.condition.Conditions;
import io.th0rgal.oraxen.command.permission.OraxenPermission;
import io.th0rgal.oraxen.language.Message;
import io.th0rgal.oraxen.recipes.listeners.RecipesEventsManager;
import io.th0rgal.oraxen.utils.recipeshowcase.RecipeShowcase;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class RecipeBook extends OraxenCommand {

    public static final OraxenCommand COMMAND = new RecipeBook();

    public static CommandInfo info() {
        return new CommandInfo("recipebook", COMMAND, "rb");
    }

    private RecipeBook() {
    }

    @Override
    public void execute(MinecraftInfo info, Arguments arguments) {
        CommandSender sender = info.getSender();

        if (Conditions
            .mixed(Conditions.reqPerm(OraxenPermission.COMMAND_RECIPE), Conditions.player(Message.NOT_PLAYER))
            .isFalse(sender)) {
            return;
        }

        //TODO Filter recipes based on each players permissions
        new RecipeShowcase(0, RecipesEventsManager.get().getWhitelistedCraftRecipes()).open((Player) sender);
    }

    @Override
    public DefaultCompletion complete(MinecraftInfo info, Arguments arguments) {
        return new DefaultCompletion();
    }

}
