package io.th0rgal.oraxen.commands;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import dev.jorel.commandapi.arguments.TextArgument;
import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.api.OraxenFurniture;
import io.th0rgal.oraxen.api.OraxenItems;
import io.th0rgal.oraxen.config.Message;
import io.th0rgal.oraxen.config.Settings;
import io.th0rgal.oraxen.font.FontManager;
import io.th0rgal.oraxen.items.ItemUpdater;
import io.th0rgal.oraxen.mechanics.MechanicsManager;
import io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.FurnitureFactory;
import io.th0rgal.oraxen.nms.NMSHandlers;
import io.th0rgal.oraxen.pack.PackGenerator;
import io.th0rgal.oraxen.pack.server.OraxenPackServer;
import io.th0rgal.oraxen.recipes.RecipesManager;
import io.th0rgal.oraxen.sound.SoundManager;
import io.th0rgal.oraxen.utils.AdventureUtils;
import io.th0rgal.oraxen.utils.logs.Logs;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class ReloadCommand {

    public static void reloadItems(@Nullable CommandSender sender) {
        Message.RELOAD.send(sender, AdventureUtils.tagResolver("reloaded", "items"));
        Optional.ofNullable(FurnitureFactory.get()).ifPresent(p -> p.packetManager().removeAllFurniturePackets());
        OraxenItems.loadItems();
        OraxenPlugin.get().invManager().regen();

        if (Settings.UPDATE_ITEMS.toBool() && Settings.UPDATE_ITEMS_ON_RELOAD.toBool()) {
            Logs.logInfo("Updating all items in player-inventories...");
            for (Player player : Bukkit.getServer().getOnlinePlayers()) {
                PlayerInventory inventory = player.getInventory();
                Bukkit.getScheduler().runTaskAsynchronously(OraxenPlugin.get(), () -> {
                    for (int i = 0; i < inventory.getSize(); i++) {
                        ItemStack oldItem = inventory.getItem(i);
                        ItemStack newItem = ItemUpdater.updateItem(oldItem);
                        if (oldItem == null || oldItem.equals(newItem)) continue;
                        inventory.setItem(i, newItem);
                    }
                });
            }
        }

        Logs.logInfo("Updating all placed furniture...");
        for (World world : Bukkit.getServer().getWorlds()) for (ItemDisplay baseEntity : world.getEntitiesByClass(ItemDisplay.class))
            OraxenFurniture.updateFurniture(baseEntity);

    }

    public static void reloadPack(@Nullable CommandSender sender) {
        Message.PACK_REGENERATED.send(sender);
        OraxenPlugin.get().fontManager(new FontManager(OraxenPlugin.get().configsManager()));
        OraxenPlugin.get().soundManager(new SoundManager(OraxenPlugin.get().configsManager().getSounds()));
        OraxenPlugin.get().packGenerator(new PackGenerator());
        OraxenPlugin.get().packGenerator().generatePack();
    }

    public static void reloadRecipes(@Nullable CommandSender sender) {
        Message.RELOAD.send(sender, AdventureUtils.tagResolver("reloaded", "recipes"));
        RecipesManager.reload();
    }

    CommandAPICommand getReloadCommand() {
        return new CommandAPICommand("reload")
                .withAliases("rl")
                .withPermission("oraxen.command.reload")
                .withArguments(new TextArgument("type").replaceSuggestions(
                        ArgumentSuggestions.strings("items", "pack", "recipes", "messages", "all")))
                .executes((sender, args) -> {
                    switch (((String) args.get("type")).toUpperCase()) {
                        case "ITEMS" -> reloadItems(sender);
                        case "PACK" -> reloadPack(sender);
                        case "RECIPES" -> reloadRecipes(sender);
                        case "CONFIGS" -> OraxenPlugin.get().reloadConfigs();
                        default -> {
                            Optional.ofNullable(FurnitureFactory.get()).ifPresent(f -> f.packetManager().removeAllFurniturePackets());
                            MechanicsManager.unloadListeners();
                            MechanicsManager.unregisterTasks();
                            NMSHandlers.resetHandler();
                            OraxenPlugin.get().reloadConfigs();
                            OraxenPlugin.get().packServer(OraxenPackServer.initializeServer());
                            MechanicsManager.registerNativeMechanics();
                            reloadItems(sender);
                            reloadRecipes(sender);
                            reloadPack(sender);
                        }
                    }
                    for (Player player : Bukkit.getOnlinePlayers()) {
                        OraxenPlugin.get().fontManager().sendGlyphTabCompletion(player);
                    }
                });
    }

}
