package io.th0rgal.oraxen.commands;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.*;
import io.th0rgal.oraxen.config.Message;
import io.th0rgal.oraxen.items.ItemBuilder;
import io.th0rgal.oraxen.items.OraxenItems;
import io.th0rgal.oraxen.pack.dispatch.PackDispatcher;
import io.th0rgal.oraxen.utils.itemsvisualizer.AllItemsInventory;
import io.th0rgal.oraxen.utils.itemsvisualizer.FileInventory;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class CommandsManager {

    public void loadCommands() {
        new CommandAPICommand("oraxen")
                .withAliases("o", "oxn")
                .withPermission("oraxen.command")
                .withSubcommand(getInvCommand())
                .withSubcommand(getGiveCommand())
                .withSubcommand(getPackCommand())
                .withSubcommand((new RepairCommand()).getRepairCommand())
                .withSubcommand((new RecipesCommand()).getRecipesCommand())
                .withSubcommand((new ReloadCommand()).getReloadCommand())
                .withSubcommand((new DebugCommand()).getDebugCommand())
                .executes((sender, args) -> {
                    Message.COMMAND_HELP.send(sender);
                })
                .register();
    }

    private CommandAPICommand getPackCommand() {
        return new CommandAPICommand("pack")
                .withPermission("oraxen.command.pack")
                .withArguments(new StringArgument("action").replaceSuggestions(info -> new String[]{"send", "msg"}))
                .withArguments(new PlayerArgument("target"))
                .executes((sender, args) -> {
                    Player target = (Player) args[1];
                    if (args[0].equals("msg"))
                        PackDispatcher.sendWelcomeMessage(target, false);
                    else PackDispatcher.sendPack(target);
                });
    }

    private CommandAPICommand getInvCommand() {
        return new CommandAPICommand("inventory")
                .withAliases("inv")
                .withPermission("oraxen.command.inventory.view")
                .withArguments(new StringArgument("type").replaceSuggestions(info -> new String[]{"all", "sorted"}))
                .executes((sender, args) -> {
                    if (sender instanceof Player player) {
                        if (args[0].equals("sorted"))
                            new FileInventory(0).open(player);
                        else new AllItemsInventory(0).open(player);
                    } else
                        Message.NOT_PLAYER.send(sender);
                });
    }

    private CommandAPICommand getGiveCommand() {
        return new CommandAPICommand("give")
                .withPermission("oraxen.command.give")
                .withArguments(new PlayerArgument("target"))
                .withArguments(new StringArgument("item").replaceSuggestions(info -> OraxenItems.getItemNames()))
                .withArguments(new IntegerArgument("amount"))
                .executes((sender, args) -> {
                    Player target = (Player) args[0];
                    ItemBuilder itemBuilder = OraxenItems.getItemById((String) args[1]);
                    int amount = (int) args[2];
                    int max = itemBuilder.getMaxStackSize();
                    int slots = amount / max + (max % amount > 0 ? 1 : 0);
                    ItemStack[] items = itemBuilder.buildArray(slots > 36 ? (amount = max * 36) : amount);
                    target.getInventory().addItem(items);
                    Message.GIVE_PLAYER
                            .send(sender, "player", target.getName(), "amount", String.valueOf(amount),
                                    "item", OraxenItems.getIdByItem(itemBuilder));
                });
    }

}
