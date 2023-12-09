package io.th0rgal.oraxen.commands;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.*;
import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.api.OraxenItems;
import io.th0rgal.oraxen.config.Message;
import io.th0rgal.oraxen.items.ItemBuilder;
import io.th0rgal.oraxen.items.ItemUpdater;
import io.th0rgal.oraxen.utils.AdventureUtils;
import org.bukkit.Color;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.inventory.meta.PotionMeta;

import java.util.Collection;
import java.util.Map;

public class CommandsManager {

    public void loadCommands() {
        new CommandAPICommand("oraxen")
                .withAliases("o", "oxn")
                .withPermission("oraxen.command")
                .withSubcommands(getDyeCommand(), getInvCommand(), getSimpleGiveCommand(), getGiveCommand(),
                        (new PackCommand()).getPackCommand(),
                        (new UpdateCommand()).getUpdateCommand(),
                        (new RepairCommand()).getRepairCommand(),
                        (new RecipesCommand()).getRecipesCommand(),
                        (new ReloadCommand()).getReloadCommand(),
                        (new DebugCommand()).getDebugCommand(),
                        (new ModelDataCommand()).getHighestModelDataCommand(),
                        (new GlyphCommand()).getGlyphCommand(),
                        (new GlyphInfoCommand()).getGlyphInfoCommand(),
                        (new ItemInfoCommand()).getItemInfoCommand(),
                        (new BlockInfoCommand()).getBlockInfoCommand(),
                        (new HudCommand()).getHudCommand(),
                        (new LogDumpCommand().getLogDumpCommand()),
                        (new GestureCommand().getGestureCommand()),
                        (new VersionCommand()).getVersionCommand(),
                        (new AdminCommands()).getAdminCommand())
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
                            hexColor = hex2Rgb((String) args.get("color"));
                        } catch (StringIndexOutOfBoundsException e) {
                            Message.DYE_WRONG_COLOR.send(sender);
                            return;
                        }
                        ItemStack item = player.getInventory().getItemInMainHand();
                        ItemMeta itemMeta = item.getItemMeta();
                        if (itemMeta instanceof LeatherArmorMeta meta) meta.setColor(hexColor);
                        else if (itemMeta instanceof PotionMeta meta) meta.setColor(hexColor);
                        else if (itemMeta instanceof MapMeta meta) meta.setColor(hexColor);
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

    private CommandAPICommand getInvCommand() {
        return new CommandAPICommand("inventory")
                .withAliases("inv")
                .withPermission("oraxen.command.inventory.view")
                .executes((sender, args) -> {
                    if (sender instanceof Player player)
                        OraxenPlugin.get().getInvManager().getItemsView(player).show(player);
                    else
                        Message.NOT_PLAYER.send(sender);
                });
    }

    @SuppressWarnings("unchecked")
    private CommandAPICommand getGiveCommand() {
        return new CommandAPICommand("give")
                .withPermission("oraxen.command.give")
                .withArguments(new EntitySelectorArgument.ManyPlayers("targets"),
                        new TextArgument("item")
                                .replaceSuggestions(ArgumentSuggestions.strings(OraxenItems.getItemNames())),
                        new IntegerArgument("amount"))
                .executes((sender, args) -> {
                    final Collection<Player> targets = (Collection<Player>) args.get(0);
                    final String itemID = (String) args.get(1);
                    final ItemBuilder itemBuilder = OraxenItems.getItemById(itemID);
                    if (itemBuilder == null) {
                        Message.ITEM_NOT_FOUND.send(sender, AdventureUtils.tagResolver("item", itemID));
                        return;
                    }
                    int amount = (int) args.get(2);
                    final int max = itemBuilder.getMaxStackSize();
                    final int slots = amount / max + (max % amount > 0 ? 1 : 0);
                    ItemStack[] items = itemBuilder.buildArray(slots > 36 ? (amount = max * 36) : amount);

                    for (final Player target : targets) {
                        Map<Integer, ItemStack> output = target.getInventory().addItem(items);
                        for (ItemStack stack : output.values())
                            target.getWorld().dropItem(target.getLocation(), stack);
                    }

                    if (targets.size() == 1)
                        Message.GIVE_PLAYER
                                .send(sender, AdventureUtils.tagResolver("player", (targets.iterator().next().getName())),
                                        AdventureUtils.tagResolver("amount", (String.valueOf(amount))),
                                        AdventureUtils.tagResolver("item", itemID));
                    else
                        Message.GIVE_PLAYERS
                                .send(sender, AdventureUtils.tagResolver("count", String.valueOf(targets.size())),
                                        AdventureUtils.tagResolver("amount", String.valueOf(amount)),
                                        AdventureUtils.tagResolver("item", itemID));
                });
    }

    @SuppressWarnings("unchecked")
    private CommandAPICommand getSimpleGiveCommand() {
        return new CommandAPICommand("give")
                .withPermission("oraxen.command.give")
                .withArguments(new EntitySelectorArgument.ManyPlayers("targets"),
                        new TextArgument("item")
                                .replaceSuggestions(ArgumentSuggestions.strings(info -> OraxenItems.getItemNames())))
                .executes((sender, args) -> {
                    final Collection<Player> targets = (Collection<Player>) args.get(0);
                    final String itemID = (String) args.get(1);
                    final ItemBuilder itemBuilder = OraxenItems.getItemById(itemID);
                    if (itemBuilder == null) {
                        Message.ITEM_NOT_FOUND.send(sender, AdventureUtils.tagResolver("item", itemID));
                        return;
                    }
                    for (final Player target : targets)
                        target.getInventory().addItem(ItemUpdater.updateItem(itemBuilder.build()));

                    if (targets.size() == 1)
                        Message.GIVE_PLAYER
                                .send(sender, AdventureUtils.tagResolver("player", targets.iterator().next().getName()),
                                        AdventureUtils.tagResolver("amount", String.valueOf(1)),
                                        AdventureUtils.tagResolver("item", itemID));
                    else
                        Message.GIVE_PLAYERS
                                .send(sender, AdventureUtils.tagResolver("count", String.valueOf(targets.size())),
                                        AdventureUtils.tagResolver("amount", String.valueOf(1)),
                                        AdventureUtils.tagResolver("item", itemID));
                });
    }
}
