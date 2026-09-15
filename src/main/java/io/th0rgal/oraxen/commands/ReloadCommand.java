package io.th0rgal.oraxen.commands;

import io.th0rgal.oraxen.commands.arguments.ArgumentSuggestions;
import io.th0rgal.oraxen.commands.arguments.TextArgument;
import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.api.OraxenFurniture;
import io.th0rgal.oraxen.api.OraxenItems;
import io.th0rgal.oraxen.api.OraxenPack;
import io.th0rgal.oraxen.api.events.OraxenItemsLoadedEvent;
import io.th0rgal.oraxen.configs.Message;
import io.th0rgal.oraxen.configs.Settings;
import io.th0rgal.oraxen.hud.HudManager;
import io.th0rgal.oraxen.items.ItemUpdater;
import io.th0rgal.oraxen.mechanics.MechanicsManager;
import io.th0rgal.oraxen.recipes.RecipesManager;
import io.th0rgal.oraxen.utils.AdventureUtils;
import io.th0rgal.oraxen.utils.SchedulerUtil;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.PlayerInventory;
import org.jetbrains.annotations.Nullable;

public class ReloadCommand {

    private static final String PACK_RELOAD = "pack";
    private static final String ITEMS_RELOAD = "items";
    private static final String RECIPES_RELOAD = "recipes";
    private static final String CONFIGS_RELOAD = "configs";
    private static final String MESSAGES_RELOAD = "messages";
    private static final String HUD_RELOAD = "huds";
    private static final String PAINTINGS_RELOAD = "paintings";
    private static final String DISCS_RELOAD = "discs";

    public static void reloadItems(@Nullable CommandSender sender) {
        sendReloadMessage(sender, ITEMS_RELOAD);
        OraxenItems.loadItems();
        OraxenPlugin.get().getInvManager().regen();
        new OraxenItemsLoadedEvent().callEvent();

        if (Settings.UPDATE_ITEMS.toBool() && Settings.UPDATE_ITEMS_ON_RELOAD.toBool()) {
            Message.UPDATING_USER_ITEMS.log();
            for (Player player : Bukkit.getServer().getOnlinePlayers()) {
                // Use runForEntity for Folia compatibility - inventory must be accessed on player's region thread
                SchedulerUtil.runForEntity(player, () -> {
                    PlayerInventory inventory = player.getInventory();
                    ItemUpdater.updateInventory(inventory);
                    ItemUpdater.updateInventory(player.getEnderChest());
                });
            }
            ItemUpdater.updateLoadedEntityContents();
            ItemUpdater.updateLoadedTileEntityContents();
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
        hudManager.registerTask();
    }

    public static void reloadRecipes(@Nullable CommandSender sender) {
        sendReloadMessage(sender, RECIPES_RELOAD);
        RecipesManager.reload();
    }

    public static void reloadConfigs(@Nullable CommandSender sender) {
        sendReloadMessage(sender, CONFIGS_RELOAD);
        OraxenPlugin.get().reloadConfigs();
        OraxenPlugin.get().reloadCustomPaintings();
        OraxenPlugin.get().reloadCustomJukeboxSongs();
    }

    public static void reloadMessages(@Nullable CommandSender sender) {
        sendReloadMessage(sender, MESSAGES_RELOAD);
        OraxenPlugin.get().reloadConfigs();
    }

    public static void reloadPaintings(@Nullable CommandSender sender) {
        sendReloadMessage(sender, PAINTINGS_RELOAD);
        OraxenPlugin.get().reloadConfigs();
        OraxenPlugin.get().reloadCustomPaintings();
    }

    public static void reloadDiscs(@Nullable CommandSender sender) {
        sendReloadMessage(sender, DISCS_RELOAD);
        OraxenPlugin.get().reloadConfigs();
        OraxenPlugin.get().reloadCustomJukeboxSongs();
    }

    private static void sendReloadMessage(@Nullable CommandSender sender, String reloaded) {
        Message.RELOAD.send(sender, AdventureUtils.tagResolver("reloaded", reloaded));
    }

    OraxenCommand getReloadCommand() {
        return new OraxenCommand("reload")
                .withAliases("rl")
                .withPermission("oraxen.command.reload")
                .withArguments(new TextArgument("type").replaceSuggestions(
                        ArgumentSuggestions.strings("items", "pack", "hud", "recipes", "configs", "messages", "paintings", "discs", "all")))
                .executes((sender, args) -> {
                    switch (((String) args.get("type")).toUpperCase()) {
                        case "HUD" -> reloadHud(sender);
                        case "ITEMS" -> reloadItems(sender);
                        case "PACK" -> reloadPack(sender);
                        case "RECIPES" -> reloadRecipes(sender);
                        case "CONFIGS" -> reloadConfigs(sender);
                        case "MESSAGES" -> reloadMessages(sender);
                        case "PAINTINGS" -> reloadPaintings(sender);
                        case "DISCS" -> reloadDiscs(sender);
                        default -> {
                            MechanicsManager.unloadListeners();
                            MechanicsManager.unregisterTasks();
                            reloadMessages(sender);
                            reloadPaintings(sender);
                            reloadDiscs(sender);
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
