package io.th0rgal.oraxen.commands;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.EntitySelectorArgument;
import dev.jorel.commandapi.arguments.IntegerArgument;
import dev.jorel.commandapi.arguments.TextArgument;
import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.config.Message;
import io.th0rgal.oraxen.items.ItemBuilder;
import io.th0rgal.oraxen.items.ItemUpdater;
import io.th0rgal.oraxen.items.OraxenItems;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Collection;

public class CommandsManager {

    public void loadCommands() {
        new CommandAPICommand("oraxen")
                .withAliases("o", "oxn")
                .withPermission("oraxen.command")
                .withSubcommand(getInvCommand())
                .withSubcommand(getSimpleGiveCommand())
                .withSubcommand(getGiveCommand())
                .withSubcommand(getPackCommand())
                .withSubcommand(getUpdateCommand())
                .withSubcommand((new RepairCommand()).getRepairCommand())
                .withSubcommand((new RecipesCommand()).getRecipesCommand())
                .withSubcommand((new ReloadCommand()).getReloadCommand())
                .withSubcommand((new DebugCommand()).getDebugCommand())
                .executes((sender, args) -> {
                    Message.COMMAND_HELP.send(sender);
                })
                .register();
    }

    @SuppressWarnings("unchecked")
    private CommandAPICommand getPackCommand() {
        return new CommandAPICommand("pack")
                .withPermission("oraxen.command.pack")
                .withArguments(new TextArgument("action")
                        .replaceSuggestions(info -> new String[]{"send", "msg"}))
                .withArguments(new EntitySelectorArgument("targets",
                        EntitySelectorArgument.EntitySelector.MANY_PLAYERS))
                .executes((sender, args) -> {
                    final Collection<Player> targets = (Collection<Player>) args[1];
                    if (args[0].equals("msg"))
                        for (final Player target : targets)
                            Message.COMMAND_JOIN_MESSAGE.send(target, "pack_url",
                                    OraxenPlugin.get().getUploadManager().getHostingProvider().getPackURL());
                    else for (final Player target : targets)
                        OraxenPlugin.get().getUploadManager().getSender().sendPack(target);
                });
    }

    private CommandAPICommand getInvCommand() {
        return new CommandAPICommand("inventory")
                .withAliases("inv")
                .withPermission("oraxen.command.inventory.view")
                .executes((sender, args) -> {
                    if (sender instanceof Player player)
                        OraxenPlugin.get().getInvManager().getItemsView().show(player);
                    else
                        Message.NOT_PLAYER.send(sender);
                });
    }

    @SuppressWarnings("unchecked")
    private CommandAPICommand getGiveCommand() {
        return new CommandAPICommand("give")
                .withPermission("oraxen.command.give")
                .withArguments(new EntitySelectorArgument("targets",
                                EntitySelectorArgument.EntitySelector.MANY_PLAYERS),
                        new TextArgument("item")
                                .replaceSuggestions(info -> OraxenItems.getItemNames()),
                        new IntegerArgument("amount"))
                .executes((sender, args) -> {
                    final Collection<Player> targets = (Collection<Player>) args[0];
                    final String itemID = (String) args[1];
                    final ItemBuilder itemBuilder = OraxenItems.getItemById(itemID);
                    int amount = (int) args[2];
                    final int max = itemBuilder.getMaxStackSize();
                    final int slots = amount / max + (max % amount > 0 ? 1 : 0);
                    final ItemStack[] items = itemBuilder.buildArray(slots > 36 ? (amount = max * 36) : amount);

                    for (final Player target : targets)
                        target.getInventory().addItem(items);

                    if (targets.size() == 1)
                        Message.GIVE_PLAYER
                                .send(sender, "player", targets.iterator().next().getName(),
                                        "amount", String.valueOf(amount),
                                        "item", itemID);
                    else
                        Message.GIVE_PLAYERS
                                .send(sender, "count", String.valueOf(targets.size()),
                                        "amount", String.valueOf(amount),
                                        "item", itemID);


                });
    }

    @SuppressWarnings("unchecked")
    private CommandAPICommand getSimpleGiveCommand() {
        return new CommandAPICommand("give")
                .withPermission("oraxen.command.give")
                .withArguments(new EntitySelectorArgument("targets",
                                EntitySelectorArgument.EntitySelector.MANY_PLAYERS),
                        new TextArgument("item")
                                .replaceSuggestions(info -> OraxenItems.getItemNames()))
                .executes((sender, args) -> {
                    final Collection<Player> targets = (Collection<Player>) args[0];
                    final String itemID = (String) args[1];
                    final ItemBuilder itemBuilder = OraxenItems.getItemById(itemID);
                    for (final Player target : targets)
                        target.getInventory().addItem(itemBuilder.build());

                    if (targets.size() == 1)
                        Message.GIVE_PLAYER
                                .send(sender, "player", targets.iterator().next().getName(),
                                        "amount", String.valueOf(1),
                                        "item", itemID);
                    else
                        Message.GIVE_PLAYERS
                                .send(sender, "count", String.valueOf(targets.size()),
                                        "amount", String.valueOf(1),
                                        "item", itemID);
                });
    }

    @SuppressWarnings("unchecked")
    private CommandAPICommand getUpdateCommand() {
        return new CommandAPICommand("update")
                .withPermission("oraxen.command.update")
                .withArguments(new EntitySelectorArgument("targets",
                        EntitySelectorArgument.EntitySelector.MANY_PLAYERS))
                .withArguments(new TextArgument("type")
                        .replaceSuggestions(info -> new String[]{"hand", "all"}))
                .executes((sender, args) -> {
                    final Collection<Player> targets = (Collection<Player>) args[0];

                    if ("hand".equals(args[1])) for (final Player player : targets) {
                        player.getInventory().setItemInMainHand(
                                ItemUpdater.updateItem(player.getInventory().getItemInMainHand()));
                        Message.UPDATED_ITEMS.send(sender, "amount",
                                String.valueOf(1), "player", player.getDisplayName());
                    }

                    if (sender.hasPermission("oraxen.command.update.all")) for (final Player player : targets) {
                        int updated = 0;
                        for (int i = 0; i < player.getInventory().getSize(); i++) {
                            final ItemStack oldItem = player.getInventory().getItem(i);
                            final ItemStack newItem = ItemUpdater.updateItem(oldItem);
                            if (oldItem == null || oldItem.equals(newItem))
                                continue;
                            player.getInventory().setItem(i, newItem);
                            updated++;
                        }
                        Message.UPDATED_ITEMS.send(sender, "amount",
                                String.valueOf(updated), "player", player.getDisplayName());
                    }
                    else
                        Message.NO_PERMISSION.send(sender, "permission", "oraxen.command.update.all");
                });
    }

}
