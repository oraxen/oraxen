package io.th0rgal.oraxen.commands;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import dev.jorel.commandapi.arguments.EntitySelectorArgument;
import dev.jorel.commandapi.arguments.TextArgument;
import io.th0rgal.oraxen.api.OraxenFurniture;
import io.th0rgal.oraxen.config.Message;
import io.th0rgal.oraxen.items.ItemUpdater;
import io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.FurnitureUpdater;
import io.th0rgal.oraxen.utils.AdventureUtils;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Collection;
import java.util.Objects;

public class UpdateCommand {

    CommandAPICommand getUpdateCommand() {
        return new CommandAPICommand("update")
                .withPermission("oraxen.command.update")
                .withArguments(new TextArgument("type").replaceSuggestions(ArgumentSuggestions.strings("item", "furniture")))
                .withArguments(new EntitySelectorArgument.ManyEntities("targets"))
                .executes((sender, args) -> {

                    String type = (String) args.get("type");


                    if (Objects.equals(type, "item")) {
                        final Collection<Player> targets = ((Collection<Entity>) args.get("targets")).stream().filter(entity -> entity instanceof Player).map(e -> (Player) e).toList();
                        for (Player player : targets) {
                            int updated = 0;
                            for (int i = 0; i < player.getInventory().getSize(); i++) {
                                final ItemStack oldItem = player.getInventory().getItem(i);
                                final ItemStack newItem = ItemUpdater.updateItem(oldItem);
                                if (oldItem == null || oldItem.equals(newItem))
                                    continue;
                                player.getInventory().setItem(i, newItem);
                                updated++;
                            }
                            player.updateInventory();
                            Message.UPDATED_ITEMS.send(sender, AdventureUtils.tagResolver("amount", String.valueOf(updated)),
                                    AdventureUtils.tagResolver("player", player.getDisplayName()));
                        }
                    } else if (Objects.equals(type, "furniture")) {
                        final Collection<Entity> targets = ((Collection<Entity>) args.get("targets")).stream().filter(OraxenFurniture::isBaseEntity).toList();
                        FurnitureUpdater.furnitureToUpdate.addAll(targets);
                    }
                });
    }
}
