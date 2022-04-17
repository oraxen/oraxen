package io.th0rgal.oraxen.commands;

import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketContainer;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import dev.jorel.commandapi.arguments.TextArgument;
import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.config.ConfigsManager;
import io.th0rgal.oraxen.config.Message;
import io.th0rgal.oraxen.font.FontManager;
import io.th0rgal.oraxen.items.OraxenItems;
import io.th0rgal.oraxen.mechanics.MechanicsManager;
import io.th0rgal.oraxen.recipes.RecipesManager;
import io.th0rgal.oraxen.sound.SoundManager;
import net.kyori.adventure.text.minimessage.Template;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.lang.reflect.InvocationTargetException;
import java.util.UUID;

public class ReloadCommand {

    private static void reloadItems(CommandSender sender) {
        Message.RELOAD.send(sender, Template.template("reloaded", "items"));
        OraxenItems.loadItems();
    }

    private static void reloadPack(OraxenPlugin plugin, CommandSender sender) {
        Message.PACK_REGENERATED.send(sender);
        OraxenPlugin.get().setFontManager(new FontManager(OraxenPlugin.get().getConfigsManager()));
        OraxenPlugin.get().setSoundManager(new SoundManager(OraxenPlugin.get().getConfigsManager().getSound()));
        OraxenPlugin.get().getResourcePack().generate(OraxenPlugin.get().getFontManager(),
                OraxenPlugin.get().getSoundManager());
        plugin.getUploadManager().uploadAsyncAndSendToPlayers(OraxenPlugin.get().getResourcePack(), true);
    }

    public CommandAPICommand getReloadCommand() {
        return new CommandAPICommand("reload")
                .withAliases("rl")
                .withPermission("oraxen.command.reload")
                .withArguments(new TextArgument("type").replaceSuggestions(
                        ArgumentSuggestions.strings("items", "pack", "recipes", "messages", "all")))
                .executes((sender, args) -> {
                    switch (((String) args[0]).toUpperCase()) {
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
                            RecipesManager.reload(oraxen);
                            OraxenPlugin.get().getInvManager().regen();
                        }
                    }
                    //TODO Make this actually clear tablist
                    FontManager manager = new FontManager(OraxenPlugin.get().getConfigsManager());
                    for (Player player : Bukkit.getOnlinePlayers()) {
                        manager.sendGlyphTabCompletion(player, false);
                    }
                });
    }

}
