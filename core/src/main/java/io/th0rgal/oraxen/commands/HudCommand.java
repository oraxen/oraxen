package io.th0rgal.oraxen.commands;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import dev.jorel.commandapi.arguments.TextArgument;
import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.config.Message;
import io.th0rgal.oraxen.hud.Hud;
import io.th0rgal.oraxen.hud.HudManager;
import io.th0rgal.oraxen.utils.AdventureUtils;
import org.bukkit.entity.Player;

public class HudCommand {

    private final HudManager manager = OraxenPlugin.get().getHudManager();

    CommandAPICommand getHudCommand() {
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
                        String hudId = (String) args.get("type");
                        Hud hud = manager.getHudFromID(hudId);
                        if (hud == null) {
                            Message.HUD_NO_HUD.send(player, AdventureUtils.tagResolver("hud_id", hudId));
                            return;
                        }

                        boolean toggle = !manager.getHudState(player);
                        manager.setHudState(player, toggle);
                        manager.setActiveHud(player, hud);
                        if (toggle) {
                            Message.HUD_TOGGLE_ON.send(player, AdventureUtils.tagResolver("hud_id", hudId));
                            manager.enableHud(player, hud);
                        }
                        else {
                            Message.HUD_TOGGLE_OFF.send(player, AdventureUtils.tagResolver("hud_id", hudId));
                            manager.disableHud(player);
                        }
                    }
                });
    }
}
