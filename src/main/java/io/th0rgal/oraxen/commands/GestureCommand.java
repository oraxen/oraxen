package io.th0rgal.oraxen.commands;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import dev.jorel.commandapi.arguments.PlayerArgument;
import dev.jorel.commandapi.arguments.TextArgument;
import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.config.Message;
import io.th0rgal.oraxen.gestures.GestureManager;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;

public class GestureCommand {

    public CommandAPICommand getGestureCommand() {
        GestureManager gestureManager = OraxenPlugin.get().getGesturesManager();
        return new CommandAPICommand("gesture")
                .withAliases("gestures", "g")
                .withPermission("oraxen.command.gesture")
                .withArguments(
                        new TextArgument("gesture").replaceSuggestions(ArgumentSuggestions.strings(GestureManager.gestures)),
                        new PlayerArgument("player")
                )
                .executes((sender, args) -> {
                    String gesture = (String) args[0];
                    if (!GestureManager.gestures.contains(gesture)) {
                        Message.GESTURE_NO_GESTURE.send(sender);
                    }

                    if (args.length == 1) {
                        if (sender instanceof Player player) {
                            gestureManager.playGesture(player, gesture);
                        } else if (sender instanceof ConsoleCommandSender console) {
                            Message.GESTURE_CONSOLE.send(console);
                        }
                    } else if (args.length == 2) {
                        Player secondPlayer = (Player) args[1];
                        if (secondPlayer != null) {
                            if (sender.hasPermission("oraxen.command.gesture.others")) {
                                gestureManager.playGesture(secondPlayer, gesture);
                            } else Message.GESTURE_OTHERS_DENIED.send(sender);
                        } else Message.NOT_PLAYER.send(sender);
                    }
                });
    }
}
