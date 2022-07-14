package io.th0rgal.oraxen.commands;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import dev.jorel.commandapi.arguments.TextArgument;
import io.th0rgal.oraxen.config.Message;
import io.th0rgal.oraxen.hud.Hud;
import io.th0rgal.oraxen.hud.HudManager;
import net.kyori.adventure.text.minimessage.Template;
import org.bukkit.entity.Player;

public class HudCommand {

    private final HudManager manager = HudManager.getInstance();

    public CommandAPICommand getHudCommand() {
        return new CommandAPICommand("hud")
                .withPermission("oraxen.command.hud.toggle")
                .withArguments(new TextArgument("type").replaceSuggestions(ArgumentSuggestions.strings("toggle")))
                .withSubcommand(disableHudsCommand());
    }

    private CommandAPICommand disableHudsCommand() {
        String[] huds = manager.getHuds().keySet().toArray(new String[0]);
        return new CommandAPICommand("toggle")
                .withPermission("oraxen.command.hud.toggle")
                .withArguments(new TextArgument("type").replaceSuggestions(ArgumentSuggestions.strings(huds)))
                .executes((sender, args) -> {
                    if (sender instanceof Player player) {
                        String hudId = (String) args[0];
                        Hud hud = manager.getHudFromID(hudId);
                        if (hud == null) {
                            Message.HUD_NO_HUD.send(player, Template.template("hud_id", hudId));
                            return;
                        }
                        if (!hud.getGameModes().contains(player.getGameMode())) return;

                        boolean toggle = !manager.getHudStateForPlayer(player);
                        manager.toggleHudForPlayer(player, toggle);
                        if (toggle) {
                            Message.HUD_TOGGLE_ON.send(player, Template.template("hud_id", hudId));
                            manager.enableHud(player, hud);
                        }
                        else {
                            Message.HUD_TOGGLE_OFF.send(player, Template.template("hud_id", hudId));
                            manager.disableHud(player);
                        }
                    }
                });
    }
}
