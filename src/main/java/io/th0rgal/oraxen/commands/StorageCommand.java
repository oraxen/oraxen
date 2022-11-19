package io.th0rgal.oraxen.commands;

import com.jeff_media.morepersistentdatatypes.DataType;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import dev.jorel.commandapi.arguments.PlayerArgument;
import dev.jorel.commandapi.arguments.TextArgument;
import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.utils.AdventureUtils;
import io.th0rgal.oraxen.utils.storage.StorageMechanic;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class StorageCommand {
    public CommandAPICommand getStorageCommand() {
        return new CommandAPICommand("storage")
                .withPermission("oraxen.command.storage")
                .withArguments(new TextArgument("type").replaceSuggestions(ArgumentSuggestions.strings("open_personal")))
                .withSubcommand(openPersonalStorageCommand());
    }

    private CommandAPICommand openPersonalStorageCommand() {
        return new CommandAPICommand("open_personal")
                .withPermission("oraxen.command.storage.open_personal")
                .withArguments(new PlayerArgument("player").replaceSuggestions(ArgumentSuggestions.strings(Bukkit.getOnlinePlayers().stream().map(Player::getName).toArray(String[]::new))))
                .executes((sender, args) -> {
                    if (sender instanceof Player player) {
                        Player target = (Player) args[0];
                        if (target == null) {
                            OraxenPlugin.get().getAudience().player(player).sendMessage(AdventureUtils.MINI_MESSAGE.deserialize("<red>The player <dark_red>" + args[0] + "</dark_red> is not online."));
                            return;
                        }

                        ItemStack[] items = target.getPersistentDataContainer().getOrDefault(StorageMechanic.PERSONAL_STORAGE_KEY, DataType.ITEM_STACK_ARRAY, new ItemStack[0]);
                        if (!target.getPersistentDataContainer().has(StorageMechanic.PERSONAL_STORAGE_KEY, DataType.ITEM_STACK_ARRAY) || items.length == 0) {
                            OraxenPlugin.get().getAudience().player(player).sendMessage(AdventureUtils.MINI_MESSAGE.deserialize("<red>The player <dark_red>" + args[0] + "</dark_red> has no items in their personal storage."));
                            return;
                        }
                        ConfigurationSection emptySection = OraxenPlugin.get().getConfig().createSection("null");
                        new StorageMechanic(emptySection).openPersonalStorage(target, player);
                    }
                });
    }
}
