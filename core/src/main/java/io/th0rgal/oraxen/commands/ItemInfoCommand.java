package io.th0rgal.oraxen.commands;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import dev.jorel.commandapi.arguments.StringArgument;
import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.api.OraxenItems;
import io.th0rgal.oraxen.items.ItemBuilder;
import io.th0rgal.oraxen.items.OraxenMeta;
import io.th0rgal.oraxen.utils.AdventureUtils;
import io.th0rgal.oraxen.utils.logs.Logs;
import net.kyori.adventure.audience.Audience;

import java.util.Map;

import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class ItemInfoCommand {

    CommandAPICommand getItemInfoCommand() {
        return new CommandAPICommand("iteminfo")
                .withPermission("oraxen.command.iteminfo")
                .withArguments(new StringArgument("itemid")
                        .replaceSuggestions(ArgumentSuggestions.strings(OraxenItems.getItemNames())))
                .executes((commandSender, args) -> {
                    String argument = (String) args.get("itemid");
                    Audience audience = OraxenPlugin.get().getAudience().sender(commandSender);
                    if (argument.equals("all")) {
                        for (Map.Entry<String, ItemBuilder> entry : OraxenItems.getEntries()) {
                            sendItemInfo(audience, entry.getValue(), entry.getKey());
                        }
                    } else {
                        ItemBuilder ib = OraxenItems.getItemById(argument);
                        if (ib == null)
                            audience.sendMessage(AdventureUtils.MINI_MESSAGE
                                    .deserialize("<red>No item found with ID</red> <dark_red>" + argument));
                        else
                            sendItemInfo(audience, ib, argument);
                    }
                });
    }

    private void sendItemInfo(Audience sender, ItemBuilder builder, String itemId) {
        ItemStack item = builder.build();
        ItemMeta meta = item.getItemMeta();

        sender.sendMessage(AdventureUtils.MINI_MESSAGE.deserialize("<dark_aqua>ItemID: <aqua>" + itemId));
        sender.sendMessage(AdventureUtils.MINI_MESSAGE.deserialize("<dark_green>Material: <green>" + item.getType()));

        // Basic meta info
        if (meta != null) {
            sender.sendMessage(AdventureUtils.MINI_MESSAGE.deserialize("<dark_green>==== Item Meta ===="));
            if (meta.hasCustomModelData()) {
                sender.sendMessage(AdventureUtils.MINI_MESSAGE
                        .deserialize("<dark_green>CustomModelData: <green>" + meta.getCustomModelData()));
            }
            if (meta.hasItemModel()) {
                sender.sendMessage(AdventureUtils.MINI_MESSAGE
                        .deserialize("<dark_green>ItemModel: <green>" + meta.getItemModel()));
            }
            if (meta.hasDisplayName()) {
                sender.sendMessage(AdventureUtils.MINI_MESSAGE
                        .deserialize("<dark_green>DisplayName: <green>" + meta.getDisplayName()));
            }
        }

        // OraxenMeta info
        OraxenMeta oraxenMeta = builder.getOraxenMeta();
        if (oraxenMeta != null) {
            sender.sendMessage(AdventureUtils.MINI_MESSAGE.deserialize("<dark_green>==== Oraxen Meta ===="));
            sender.sendMessage(AdventureUtils.MINI_MESSAGE
                    .deserialize("<dark_green>ModelName: <green>" + oraxenMeta.getModelName()));
            if (oraxenMeta.hasPackInfos()) {
                sender.sendMessage(AdventureUtils.MINI_MESSAGE
                        .deserialize("<dark_green>ModelPath: <green>" + oraxenMeta.getGeneratedModelPath()));
            }
        }

        Logs.newline();
    }
}
