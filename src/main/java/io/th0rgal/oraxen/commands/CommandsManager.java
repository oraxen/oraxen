package io.th0rgal.oraxen.commands;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.*;
import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.config.Message;
import io.th0rgal.oraxen.items.ItemBuilder;
import io.th0rgal.oraxen.items.ItemUpdater;
import io.th0rgal.oraxen.items.OraxenItems;
import net.kyori.adventure.text.minimessage.Template;
import org.bukkit.Color;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.inventory.meta.PotionMeta;

import java.util.Collection;
import java.util.Map;

public class CommandsManager {

    public void loadCommands() {
        ConfigurationSection commandsSection =
                OraxenPlugin.get().getConfigsManager().getSettings().getConfigurationSection("Plugin.commands");
        if (commandsSection == null) return;
        new CommandAPICommand("oraxen")
                .withAliases("o", "oxn")
                .withPermission("oraxen.command")
                .withSubcommand(getDyeCommand())
                .withSubcommand(getInvCommand())
                .withSubcommand(getSimpleGiveCommand())
                .withSubcommand(getGiveCommand())
                .withSubcommand(getPackCommand())
                .withSubcommand(getUpdateCommand())
                .withSubcommand((new RepairCommand()).getRepairCommand())
                .withSubcommand((new RecipesCommand()).getRecipesCommand())
                .withSubcommand((new ReloadCommand()).getReloadCommand())
                .withSubcommand((new DebugCommand()).getDebugCommand())
                .withSubcommand((new ModelDataCommand()).getHighestModelDataCommand())
                .withSubcommand((new GlyphCommand()).getGlyphCommand(commandsSection))
                .withSubcommand((new PrintGlyphCommand()).getPrintGlyphCommand())
                .withSubcommand((new ItemInfoCommand()).getItemInfoCommand())
                .executes((sender, args) -> {
                    Message.COMMAND_HELP.send(sender);
                })
                .register();
    }

    private Color hex2Rgb(String colorStr) {
        return Color.fromRGB(
                Integer.valueOf(colorStr.substring(1, 3), 16),
                Integer.valueOf(colorStr.substring(3, 5), 16),
                Integer.valueOf(colorStr.substring(5, 7), 16));
    }

    private CommandAPICommand getDyeCommand() {
        return new CommandAPICommand("dye")
                .withPermission("oraxen.command.dye")
                .withArguments(new GreedyStringArgument("color"))
                .executes((sender, args) -> {
                    if (sender instanceof Player player) {
                        Color hexColor;
                        try {
                            hexColor = hex2Rgb((String) args[0]);
                        } catch (StringIndexOutOfBoundsException e) {
                            Message.DYE_WRONG_COLOR.send(sender);
                            return;
                        }
                        ItemStack item = player.getInventory().getItemInMainHand();
                        ItemMeta itemMeta = item.getItemMeta();
                        if (itemMeta instanceof LeatherArmorMeta meta) meta.setColor(hexColor);
                        else if (itemMeta instanceof PotionMeta meta) meta.setColor(hexColor);
                        else {
                            Message.DYE_FAILED.send(sender);
                            return;
                        }
                        item.setItemMeta(itemMeta);
                        Message.DYE_SUCCESS.send(sender);
                    } else
                        Message.NOT_PLAYER.send(sender);
                });
    }

    @SuppressWarnings("unchecked")
    private CommandAPICommand getPackCommand() {
        return new CommandAPICommand("pack")
                .withPermission("oraxen.command.pack")
                .withArguments(new TextArgument("action")
                        .replaceSuggestions(ArgumentSuggestions.strings("send", "msg")))
                .withArguments(new EntitySelectorArgument("targets",
                        EntitySelector.MANY_PLAYERS))
                .executes((sender, args) -> {
                    final Collection<Player> targets = (Collection<Player>) args[1];
                    if (args[0].equals("msg"))
                        for (final Player target : targets)
                            Message.COMMAND_JOIN_MESSAGE.send(target, Template.template("pack_url",
                                    OraxenPlugin.get().getUploadManager().getHostingProvider().getPackURL()));
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
                                EntitySelector.MANY_PLAYERS),
                        new TextArgument("item")
                                .replaceSuggestions(ArgumentSuggestions.strings(OraxenItems.getItemNames())),
                        new IntegerArgument("amount"))
                .executes((sender, args) -> {
                    final Collection<Player> targets = (Collection<Player>) args[0];
                    final String itemID = (String) args[1];
                    final ItemBuilder itemBuilder = OraxenItems.getItemById(itemID);
                    int amount = (int) args[2];
                    final int max = itemBuilder.getMaxStackSize();
                    final int slots = amount / max + (max % amount > 0 ? 1 : 0);
                    final ItemStack[] items = itemBuilder.buildArray(slots > 36 ? (amount = max * 36) : amount);

                    for (final Player target : targets) {
                        Map<Integer, ItemStack> output = target.getInventory().addItem(items);
                        for (ItemStack stack : output.values())
                            target.getWorld().dropItem(target.getLocation(), stack);
                    }

                    if (targets.size() == 1)
                        Message.GIVE_PLAYER
                                .send(sender, Template.template("player", targets.iterator().next().getName()),
                                        Template.template("amount", String.valueOf(amount)),
                                        Template.template("item", itemID));
                    else
                        Message.GIVE_PLAYERS
                                .send(sender, Template.template("count", String.valueOf(targets.size())),
                                        Template.template("amount", String.valueOf(amount)),
                                        Template.template("item", itemID));
                });
    }

    @SuppressWarnings("unchecked")
    private CommandAPICommand getSimpleGiveCommand() {
        return new CommandAPICommand("give")
                .withPermission("oraxen.command.give")
                .withArguments(new EntitySelectorArgument("targets",
                                EntitySelector.MANY_PLAYERS),
                        new TextArgument("item")
                                .replaceSuggestions(ArgumentSuggestions.strings(info -> OraxenItems.getItemNames())))
                .executes((sender, args) -> {
                    final Collection<Player> targets = (Collection<Player>) args[0];
                    final String itemID = (String) args[1];
                    final ItemBuilder itemBuilder = OraxenItems.getItemById(itemID);
                    for (final Player target : targets)
                        target.getInventory().addItem(itemBuilder.build());

                    if (targets.size() == 1)
                        Message.GIVE_PLAYER
                                .send(sender, Template.template("player", targets.iterator().next().getName()),
                                        Template.template("amount", String.valueOf(1)),
                                        Template.template("item", itemID));
                    else
                        Message.GIVE_PLAYERS
                                .send(sender, Template.template("count", String.valueOf(targets.size())),
                                        Template.template("amount", String.valueOf(1)),
                                        Template.template("item", itemID));
                });
    }

    @SuppressWarnings("unchecked")
    private CommandAPICommand getUpdateCommand() {
        return new CommandAPICommand("update")
                .withPermission("oraxen.command.update")
                .withArguments(new EntitySelectorArgument("targets",
                        EntitySelector.MANY_PLAYERS))
                .withArguments(new TextArgument("type")
                        .replaceSuggestions(ArgumentSuggestions.strings("hand", "all")))
                .executes((sender, args) -> {
                    final Collection<Player> targets = (Collection<Player>) args[0];

                    if ("hand".equals(args[1])) for (final Player player : targets) {
                        player.getInventory().setItemInMainHand(
                                ItemUpdater.updateItem(player.getInventory().getItemInMainHand()));
                        Message.UPDATED_ITEMS.send(sender, Template.template("amount",
                                String.valueOf(1)), Template.template("player", player.getDisplayName()));
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
                        Message.UPDATED_ITEMS.send(sender, Template.template("amount",
                                String.valueOf(updated)), Template.template("player", player.getDisplayName()));
                    }
                    else
                        Message.NO_PERMISSION.send(sender, Template.template("permission", "oraxen.command.update.all"));
                });
    }

}
