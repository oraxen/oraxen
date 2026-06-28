package io.th0rgal.oraxen.commands;

import io.th0rgal.oraxen.commands.arguments.ArgumentSuggestions;
import io.th0rgal.oraxen.commands.arguments.EntitySelectorArgument;
import io.th0rgal.oraxen.commands.arguments.GreedyStringArgument;
import io.th0rgal.oraxen.commands.arguments.IntegerArgument;
import io.th0rgal.oraxen.commands.arguments.TextArgument;
import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.api.OraxenItems;
import io.th0rgal.oraxen.configs.Message;
import io.th0rgal.oraxen.items.ItemBuilder;
import io.th0rgal.oraxen.items.ItemUpdater;
import io.th0rgal.oraxen.utils.AdventureUtils;
import io.th0rgal.oraxen.utils.ItemUtils;
import org.bukkit.Color;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;

public class CommandsManager {

    private static final String INVENTORY_VIEW_PERMISSION = "oraxen.command.inventory.view";
    private static final int MAX_GIVE_SLOTS = 36;

    public void loadCommands() {
        new OraxenCommand("oraxen")
                .withAliases("o", "oxn")
                .withPermission("oraxen.command")
                .withSubcommands(getDyeCommand(), getInvCommand(), getSimpleGiveCommand(), getGiveCommand(),
                        getTakeCommand(),
                        (new PackCommand()).getPackCommand(),
                        (new UpdateCommand()).getUpdateCommand(),
                        (new RepairCommand()).getRepairCommand(),
                        (new RecipesCommand()).getRecipesCommand(),
                        (new ReloadCommand()).getReloadCommand(),
                        (new ReportCommand()).getReportCommand(),
                        (new DebugCommand()).getDebugCommand(),
                        (new ModelDataCommand()).getHighestModelDataCommand(),
                        (new GlyphCommand()).getGlyphCommand(),
                        (new InfoCommand()).getInfoCommand(),
                        (new HudCommand()).getHudCommand(),
                        (new LogDumpCommand().getLogDumpCommand()),
                        (new VersionCommand()).getVersionCommand(),
                        (new AdminCommand()).getAdminCommand(),
                        (new SchemaCommand()).getSchemaCommand(),
                        (new RemoveBrandingCommand()).getRemoveBrandingCommand(),
                        (new RemoveDefaultsCommand()).getRemoveDefaultsCommand(),
                        (new TotemAnimationCommand()).getTotemAnimationCommand(),
                        (new TextEffectCommand()).getTextEffectCommand(),
                        (new TextEffectCommand()).getTextEffectsListCommand())
                .executes((sender, args) -> {
                    openInventoryOrHelp(sender);
                })
                .register();
    }

    private Color hex2Rgb(final String colorStr) throws NumberFormatException {
        return Color.fromRGB(
                Integer.valueOf(colorStr.substring(1, 3), 16),
                Integer.valueOf(colorStr.substring(3, 5), 16),
                Integer.valueOf(colorStr.substring(5, 7), 16));
    }

    private OraxenCommand getDyeCommand() {
        return new OraxenCommand("dye")
                .withPermission("oraxen.command.dye")
                .withArguments(new GreedyStringArgument("color"))
                .executes((sender, args) -> {
                    if (sender instanceof final Player player) {
                        final Color hexColor;
                        try {
                            hexColor = hex2Rgb((String) args.get("color"));
                        } catch (final StringIndexOutOfBoundsException | NumberFormatException e) {
                            Message.DYE_WRONG_COLOR.send(sender);
                            return;
                        }
                        ItemUtils.dyeItem(player.getInventory().getItemInMainHand(), hexColor);
                        Message.DYE_SUCCESS.send(sender);
                    } else
                        Message.NOT_PLAYER.send(sender);
                });
    }

    private OraxenCommand getInvCommand() {
        return new OraxenCommand("inventory")
                .withAliases("inv")
                .withPermission(INVENTORY_VIEW_PERMISSION)
                .executes((sender, args) -> {
                    openInventory(sender);
                });
    }

    private void openInventory(final CommandSender sender) {
        if (!(sender instanceof final Player player)) {
            Message.NOT_PLAYER.send(sender);
            return;
        }

        if (!player.hasPermission(INVENTORY_VIEW_PERMISSION)) {
            Message.NO_PERMISSION.send(sender, AdventureUtils.tagResolver("permission", INVENTORY_VIEW_PERMISSION));
            return;
        }

        OraxenPlugin.get().getInvManager().getItemsView(player).open(player);
    }

    private void openInventoryOrHelp(final CommandSender sender) {
        if (sender instanceof final Player player && player.hasPermission(INVENTORY_VIEW_PERMISSION)) {
            OraxenPlugin.get().getInvManager().getItemsView(player).open(player);
            return;
        }

        sendRootHelp(sender);
    }

    private void sendRootHelp(final CommandSender sender) {
        sender.sendMessage("Oraxen commands");
        sender.sendMessage("/oraxen inventory - Open the item browser");
        sender.sendMessage("/oraxen give <player> <item> [amount] - Give an Oraxen item");
        sender.sendMessage("/oraxen pack <send|msg|extract_default> - Manage the resource pack");
        sender.sendMessage("/oraxen reload - Reload Oraxen");
        sender.sendMessage("/oraxen info <item|glyph|block> <id|all> - Show Oraxen info");
        sender.sendMessage("/oraxen version - Show version information");
    }

    private void sendInvalidGiveAmount(final CommandSender sender) {
        Message.GIVE_INVALID_AMOUNT.send(sender);
    }

    static boolean isValidGiveAmount(final int amount) {
        return amount > 0;
    }

    static int capGiveAmountToInventory(final int amount, final int maxStackSize) {
        final int slots = amount / maxStackSize + (amount % maxStackSize > 0 ? 1 : 0);
        return slots > MAX_GIVE_SLOTS ? maxStackSize * MAX_GIVE_SLOTS : amount;
    }

    @SuppressWarnings("unchecked")
    private OraxenCommand getGiveCommand() {
        return new OraxenCommand("give")
                .withPermission("oraxen.command.give")
                .withArguments(new EntitySelectorArgument.ManyPlayers("targets"),
                        new TextArgument("item")
                                .replaceSuggestions(ArgumentSuggestions.strings(OraxenItems.getItemNames())),
                        new IntegerArgument("amount"))
                .executes((sender, args) -> {
                    final Collection<Player> targets = (Collection<Player>) args.get(0);
                    final String itemID = (String) args.get(1);
                    int amount = (int) args.get(2);
                    if (!isValidGiveAmount(amount)) {
                        sendInvalidGiveAmount(sender);
                        return;
                    }

                    final ItemBuilder itemBuilder = OraxenItems.getItemById(itemID);
                    if (itemBuilder == null) {
                        Message.ITEM_NOT_FOUND.send(sender, AdventureUtils.tagResolver("item", itemID));
                        return;
                    }
                    final int max = itemBuilder.hasMaxStackSize() ? itemBuilder.getMaxStackSize()
                            : itemBuilder.getType().getMaxStackSize();
                    amount = capGiveAmountToInventory(amount, max);
                    final ItemStack[] items = itemBuilder.buildArray(amount);

                    for (final Player target : targets) {
                        final Map<Integer, ItemStack> output = target.getInventory().addItem(items);
                        if (!output.isEmpty()) {
                            for (final ItemStack stack : output.values())
                                target.getWorld().dropItem(target.getLocation(), stack);
                        }
                    }

                    if (targets.size() == 1)
                        Message.GIVE_PLAYER
                                .send(sender,
                                        AdventureUtils.tagResolver("player", (targets.iterator().next().getName())),
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
    private OraxenCommand getSimpleGiveCommand() {
        return new OraxenCommand("give")
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

                    for (final Player target : targets) {
                        final Map<Integer, ItemStack> output = target.getInventory()
                                .addItem(ItemUpdater.updateItem(itemBuilder.build()));
                        if (!output.isEmpty()) {
                            for (final ItemStack stack : output.values()) {
                                target.getWorld().dropItem(target.getLocation(), stack);
                            }
                        }
                    }

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

    private OraxenCommand getTakeCommand() {
        return new OraxenCommand("take")
                .withPermission("oraxen.command.take")
                .withArguments(
                        new EntitySelectorArgument.ManyPlayers("targets"),
                        new TextArgument("item")
                                .replaceSuggestions(ArgumentSuggestions.strings(OraxenItems.getItemNames())),
                        new IntegerArgument("amount").setOptional(true))
                .executes((sender, args) -> {
                    final Collection<Player> targets = (Collection<Player>) args.get("targets");
                    final String itemID = (String) args.getOrDefault("item", "");
                    final Optional<Integer> amount = args.getOptionalByClass("amount", Integer.class);
                    if (!OraxenItems.exists(itemID)) {
                        Message.ITEM_NOT_FOUND.send(sender, AdventureUtils.tagResolver("item", itemID));
                    } else
                        for (final Player target : targets) {
                            if (amount.isEmpty()) {
                                for (final ItemStack itemStack : target.getInventory().getContents())
                                    if (!ItemUtils.isEmpty(itemStack)
                                            && itemID.equals(OraxenItems.getIdByItem(itemStack)))
                                        target.getInventory().remove(itemStack);
                            } else {
                                int toRemove = amount.get();
                                while (toRemove > 0) {
                                    final ItemStack[] items = target.getInventory().getStorageContents();
                                    for (int i = 0; i < items.length; i++) {
                                        final ItemStack itemStack = items[i];
                                        if (!ItemUtils.isEmpty(itemStack)
                                                && itemID.equals(OraxenItems.getIdByItem(itemStack))) {
                                            if (itemStack.getAmount() <= toRemove) {
                                                toRemove -= itemStack.getAmount();
                                                target.getInventory().clear(i);
                                            } else {
                                                itemStack.setAmount(itemStack.getAmount() - toRemove);
                                                toRemove = 0;
                                            }

                                            if (toRemove == 0)
                                                break;
                                        }
                                    }

                                    if (toRemove > 0)
                                        break;
                                }
                            }
                        }
                });
    }
}
