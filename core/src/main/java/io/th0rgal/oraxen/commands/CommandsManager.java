package io.th0rgal.oraxen.commands;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.*;
import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.api.OraxenItems;
import io.th0rgal.oraxen.config.Message;
import io.th0rgal.oraxen.items.ItemBuilder;
import io.th0rgal.oraxen.items.ItemUpdater;
import io.th0rgal.oraxen.utils.AdventureUtils;
import io.th0rgal.oraxen.utils.ItemUtils;
import org.bukkit.Color;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;

public class CommandsManager {

    public void loadCommands() {
        new CommandAPICommand("oraxen")
                .withAliases("o", "oxn")
                .withPermission("oraxen.command")
                .withSubcommands(getDyeCommand(), getInvCommand(), getSimpleGiveCommand(), getGiveCommand(), getTakeCommand(),
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
                        (new VersionCommand()).getVersionCommand(),
                        (new AdminCommand()).getAdminCommand())
                .executes((sender, args) -> {
                    Message.COMMAND_HELP.send(sender);
                })
                .register();
    }

    private Color hex2Rgb(String colorStr) throws NumberFormatException {
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
                        } catch (StringIndexOutOfBoundsException | NumberFormatException e) {
                            Message.DYE_WRONG_COLOR.send(sender);
                            return;
                        }
                        ItemUtils.dyeItem(player.getInventory().getItemInMainHand(), hexColor);
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
                        OraxenPlugin.get().getInvManager().getItemsView(player).open(player);
                    else Message.NOT_PLAYER.send(sender);
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
                    final int max = itemBuilder.hasMaxStackSize() ? itemBuilder.getMaxStackSize() : itemBuilder.getType().getMaxStackSize();
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

    private CommandAPICommand getTakeCommand() {
        return new CommandAPICommand("take")
                .withPermission("oraxen.command.take")
                .withArguments(
                        new EntitySelectorArgument.ManyPlayers("targets"),
                        new TextArgument("item").replaceSuggestions(ArgumentSuggestions.strings(OraxenItems.getItemNames())),
                        new IntegerArgument("amount").setOptional(true)
                )
                .executes((sender, args) -> {
                    final Collection<Player> targets = (Collection<Player>) args.get("targets");
                    final String itemID = (String) args.getOrDefault("item", "");
                    final Optional<Integer> amount = args.getOptionalByClass("amount", Integer.class);
                    if (!OraxenItems.exists(itemID)) {
                        Message.ITEM_NOT_FOUND.send(sender, AdventureUtils.tagResolver("item", itemID));
                    } else for (final Player target : targets) {
                        if (amount.isEmpty()) {
                            for (ItemStack itemStack : target.getInventory().getContents())
                                if (!ItemUtils.isEmpty(itemStack) && itemID.equals(OraxenItems.getIdByItem(itemStack)))
                                    target.getInventory().remove(itemStack);
                        } else {
                            int toRemove = amount.get();
                            while (toRemove > 0) {
                                ItemStack[] items = target.getInventory().getStorageContents();
                                for (int i = 0; i < items.length; i++) {
                                    ItemStack itemStack = items[i];
                                    if (!ItemUtils.isEmpty(itemStack) && itemID.equals(OraxenItems.getIdByItem(itemStack))) {
                                        if (itemStack.getAmount() <= toRemove) {
                                            toRemove -= itemStack.getAmount();
                                            target.getInventory().clear(i);
                                        } else {
                                            itemStack.setAmount(itemStack.getAmount() - toRemove);
                                            toRemove = 0;
                                        }

                                        if (toRemove == 0) break;
                                    }
                                }

                                if (toRemove > 0) break;
                            }
                        }
                    }
                });
    }
}
