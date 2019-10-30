package io.th0rgal.oraxen.commands.subcommands;

import io.th0rgal.oraxen.commands.CommandInterface;
import io.th0rgal.oraxen.recipes.builders.RecipeBuilder;
import io.th0rgal.oraxen.recipes.builders.ShapedBuilder;
import io.th0rgal.oraxen.recipes.builders.ShapelessBuilder;
import io.th0rgal.oraxen.settings.Message;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class Recipes implements CommandInterface {

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {

        if (!sender.hasPermission("oraxen.command.recipes")) {
            Message.DONT_HAVE_PERMISSION.send(sender, "oraxen.command.recipes");
            return false;
        }

        if (args.length == 1) {
            sender.sendMessage("$help");

        } else if (sender instanceof Player) {
            Player player = (Player) sender;
            RecipeBuilder currentBuilder = RecipeBuilder.get(player.getUniqueId());

            switch (args[1]) {

                case "open":
                    if (args.length > 2)
                        buildAndOpen(player, args[2]);
                    else if (currentBuilder != null)
                        currentBuilder.open();
                    else
                        sender.sendMessage("$recipeslist");
                    break;

                case "save":
                    if (currentBuilder == null)
                        sender.sendMessage("Builder is null! You need to create a new one");
                    else if (args.length == 3)
                        currentBuilder.saveRecipe(args[2]);
                    else if (args.length == 4)
                        currentBuilder.saveRecipe(args[2], args[3]);
                    else
                        sender.sendMessage("o recipes save <recipe_name>");
                    break;

                default:
                    break;
            }

        } else {
            Message.NOT_A_PLAYER_ERROR.send(sender);
        }

        return true;
    }


    private void buildAndOpen(Player player, String recipeType) {
        switch (recipeType.toLowerCase()) {
            case "shaped":
                new ShapedBuilder(player);
                break;

            case "shapeless":
                new ShapelessBuilder(player);
                break;

            default:
                player.sendMessage("error");
                break;
        }
    }

}
