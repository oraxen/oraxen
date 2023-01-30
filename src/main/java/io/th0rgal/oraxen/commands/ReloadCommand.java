package io.th0rgal.oraxen.commands;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import dev.jorel.commandapi.arguments.TextArgument;
import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.api.OraxenItems;
import io.th0rgal.oraxen.config.Message;
import io.th0rgal.oraxen.font.FontManager;
import io.th0rgal.oraxen.hud.HudManager;
import io.th0rgal.oraxen.mechanics.MechanicsManager;
import io.th0rgal.oraxen.recipes.RecipesManager;
import io.th0rgal.oraxen.sound.SoundManager;
import io.th0rgal.oraxen.utils.AdventureUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ReloadCommand {

    private static void reloadItems(CommandSender sender) {
        Message.RELOAD.send(sender, AdventureUtils.tagResolver("reloaded", "items"));
        OraxenItems.loadItems();
    }

    private static void reloadPack(OraxenPlugin plugin, CommandSender sender) {
        Message.PACK_REGENERATED.send(sender);
        plugin.setFontManager(new FontManager(plugin.getConfigsManager()));
        plugin.setSoundManager(new SoundManager(plugin.getConfigsManager().getSound()));
        plugin.getResourcePack().generate(plugin.getFontManager(),
                plugin.getSoundManager());
        plugin.getUploadManager().uploadAsyncAndSendToPlayers(plugin.getResourcePack(), true, true);
    }

    private static void reloadHud(CommandSender sender) {
        Message.RELOAD.send(sender, AdventureUtils.tagResolver("reloaded", "hud"));
        OraxenPlugin.get().reloadConfigs();
        HudManager hudManager = new HudManager(OraxenPlugin.get().getConfigsManager());
        OraxenPlugin.get().setHudManager(hudManager);
        hudManager.loadHuds(hudManager.getHudConfigSection());
        hudManager.parsedHudDisplays = hudManager.generateHudDisplays();
        hudManager.reregisterEvents();
        hudManager.restartTask();
    }

    private static void reloadGestures(CommandSender sender) {
        Message.RELOAD.send(sender, AdventureUtils.tagResolver("reloaded", "gestures"));
        OraxenPlugin.get().getGesturesManager().reload();
    }

    public CommandAPICommand getReloadCommand() {
        return new CommandAPICommand("reload")
                .withAliases("rl")
                .withPermission("oraxen.command.reload")
                .withArguments(new TextArgument("type").replaceSuggestions(
                        ArgumentSuggestions.strings("items", "pack", "hud", "recipes", "messages", "all")))
                .executes((sender, args) -> {
                    switch (((String) args[0]).toUpperCase()) {
                        case "HUD" -> reloadHud(sender);
                        case "ITEMS" -> {
                            reloadItems(sender);
                            OraxenPlugin.get().getInvManager().regen();
                        }
                        case "PACK" -> reloadPack(OraxenPlugin.get(), sender);
                        case "RECIPES" -> RecipesManager.reload(OraxenPlugin.get());
                        case "CONFIGS" -> OraxenPlugin.get().reloadConfigs();
                        default -> {
                            OraxenPlugin oraxen = OraxenPlugin.get();
                            MechanicsManager.unloadListeners();
                            MechanicsManager.registerNativeMechanics();
                            OraxenPlugin.get().reloadConfigs();
                            reloadItems(sender);
                            reloadPack(oraxen, sender);
                            reloadHud(sender);
                            RecipesManager.reload(oraxen);
                            OraxenPlugin.get().getInvManager().regen();
                        }
                    }
                    // This does not clear the tablist, and I am not sure how to do it otherwise
                    FontManager manager = new FontManager(OraxenPlugin.get().getConfigsManager());
                    for (Player player : Bukkit.getOnlinePlayers()) {
                        manager.sendGlyphTabCompletion(player, false);
                    }
                });
    }

}
