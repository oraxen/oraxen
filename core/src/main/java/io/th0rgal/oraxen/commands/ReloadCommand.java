package io.th0rgal.oraxen.commands;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import dev.jorel.commandapi.arguments.TextArgument;
import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.api.OraxenFurniture;
import io.th0rgal.oraxen.api.OraxenItems;
import io.th0rgal.oraxen.api.OraxenPack;
import io.th0rgal.oraxen.api.events.OraxenItemsLoadedEvent;
import io.th0rgal.oraxen.config.Message;
import io.th0rgal.oraxen.config.Settings;
import io.th0rgal.oraxen.hud.HudManager;
import io.th0rgal.oraxen.items.ItemUpdater;
import io.th0rgal.oraxen.mechanics.MechanicsManager;
import io.th0rgal.oraxen.recipes.RecipesManager;
import io.th0rgal.oraxen.utils.AdventureUtils;
import io.th0rgal.oraxen.utils.SchedulerUtil;
import io.th0rgal.oraxen.utils.logs.Logs;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.jetbrains.annotations.Nullable;

public class ReloadCommand {

    private static final String PACK_RELOAD = "pack";
    private static final String ITEMS_RELOAD = "items";
    private static final String RECIPES_RELOAD = "recipes";
    private static final String MESSAGES_RELOAD = "messages";
    private static final String HUD_RELOAD = "huds";

    public static void reloadItems(@Nullable CommandSender sender) {
        sendReloadMessage(sender, ITEMS_RELOAD);
        OraxenItems.loadItems();
        OraxenPlugin.get().getInvManager().regen();
        Bukkit.getPluginManager().callEvent(new OraxenItemsLoadedEvent());

        if (Settings.UPDATE_ITEMS.toBool() && Settings.UPDATE_ITEMS_ON_RELOAD.toBool()) {
            Message.UPDATING_USER_ITEMS.log();
            for (Player player : Bukkit.getServer().getOnlinePlayers()) {
                // Use runForEntity for Folia compatibility - inventory must be accessed on player's region thread
                SchedulerUtil.runForEntity(player, () -> {
                    PlayerInventory inventory = player.getInventory();
                    for (int i = 0; i < inventory.getSize(); i++) {
                        ItemStack oldItem = inventory.getItem(i);
                        ItemStack newItem = ItemUpdater.updateItem(oldItem);
                        if (oldItem == null || oldItem.equals(newItem))
                            continue;
                        inventory.setItem(i, newItem);
                    }
                });
            }
        }

        if (Settings.UPDATE_FURNITURE.toBool() && Settings.UPDATE_FURNITURE_ON_RELOAD.toBool()) {
            Message.UPDATING_PLACED_FURNITURES.log();
            for (World world : Bukkit.getServer().getWorlds())
                world.getEntities().stream().filter(OraxenFurniture::isBaseEntity)
                        .forEach(entity -> SchedulerUtil.runForEntity(entity, () -> OraxenFurniture.updateFurniture(entity)));
        }

    }

    public static void reloadPack(@Nullable CommandSender sender) {
        sendReloadMessage(sender, PACK_RELOAD);
        OraxenPack.reloadPack();
    }

    public static void reloadHud(@Nullable CommandSender sender) {
        sendReloadMessage(sender, HUD_RELOAD);
        OraxenPlugin.get().reloadConfigs();
        HudManager hudManager = new HudManager(OraxenPlugin.get().getConfigsManager());
        OraxenPlugin.get().setHudManager(hudManager);
        hudManager.loadHuds(hudManager.getHudConfigSection());
        hudManager.parsedHudDisplays = hudManager.generateHudDisplays();
        hudManager.reregisterEvents();
        hudManager.restartTask();
    }

    public static void reloadRecipes(@Nullable CommandSender sender) {
        sendReloadMessage(sender, RECIPES_RELOAD);
        RecipesManager.reload();
    }

    public static void reloadMessages(@Nullable CommandSender sender) {
        sendReloadMessage(sender, MESSAGES_RELOAD);
        OraxenPlugin.get().reloadConfigs();
    }

    private static void sendReloadMessage(@Nullable CommandSender sender, String reloaded) {
        Message.RELOAD.send(sender, AdventureUtils.tagResolver("reloaded", reloaded));
    }

    CommandAPICommand getReloadCommand() {
        return new CommandAPICommand("reload")
                .withAliases("rl")
                .withPermission("oraxen.command.reload")
                .withArguments(new TextArgument("type").replaceSuggestions(
                        ArgumentSuggestions.strings("items", "pack", "hud", "recipes", "messages", "all")))
                .executes((sender, args) -> {
                    switch (((String) args.get("type")).toUpperCase()) {
                        case "HUD" -> reloadHud(sender);
                        case "ITEMS" -> reloadItems(sender);
                        case "PACK" -> reloadPack(sender);
                        case "RECIPES" -> reloadRecipes(sender);
                        case "MESSAGES" -> reloadMessages(sender);
                        default -> {
                            MechanicsManager.unloadListeners();
                            MechanicsManager.unregisterTasks();
                            reloadMessages(sender);
                            MechanicsManager.registerNativeMechanics();
                            reloadItems(sender);
                            reloadPack(sender);
                            reloadHud(sender);
                            reloadRecipes(sender);
                        }
                    }
                    for (Player player : Bukkit.getOnlinePlayers()) {
                        OraxenPlugin.get().getFontManager().sendGlyphTabCompletion(player);
                    }
                });
    }

}
