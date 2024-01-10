package io.th0rgal.oraxen.commands;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import dev.jorel.commandapi.arguments.PlayerArgument;
import dev.jorel.commandapi.arguments.StringArgument;
import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.config.Message;
import io.th0rgal.oraxen.config.Settings;
import io.th0rgal.oraxen.gestures.GestureManager;
import io.th0rgal.oraxen.utils.AdventureUtils;
import io.th0rgal.oraxen.utils.VersionUtil;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;

public class GestureCommand {

    CommandAPICommand getGestureCommand() {
        GestureManager gestureManager = OraxenPlugin.get().getGesturesManager();

        return new CommandAPICommand("gesture")
                .withAliases("gestures", "g")
                .withPermission("oraxen.command.gesture")
                .withArguments(
                        new StringArgument("gesture").replaceSuggestions(ArgumentSuggestions.strings(GestureManager.getGestures())),
                        new PlayerArgument("player").setOptional(true)
                )
                .executes((sender, args) -> {
                    if (!Settings.GESTURES_ENABLED.toBool() || VersionUtil.atOrAbove("1.20.3")) {
                        OraxenPlugin.get().getAudience().sender(sender).sendMessage(AdventureUtils.MINI_MESSAGE.deserialize("<red>Gestures are not enabled!"));
                        return;
                    }
                    String gesture = (String) args.get("gesture");
                    if (!GestureManager.gestures.contains(gesture)) {
                        Message.GESTURE_NO_GESTURE.send(sender);
                        return;
                    }

                    if (args.count() == 1) {
                        if (sender instanceof Player player) {
                            gestureManager.playGesture(player, gesture);
                        } else if (sender instanceof ConsoleCommandSender console) {
                            Message.GESTURE_CONSOLE.send(console);
                        }
                    } else if (args.count() == 2) {
                        Player secondPlayer = (Player) args.get("player");
                        if (secondPlayer == null) secondPlayer = (Player) sender;
                        if (secondPlayer != null) {
                            if (sender.hasPermission("oraxen.command.gesture.others")) {
                                gestureManager.playGesture(secondPlayer, gesture);
                            } else Message.GESTURE_OTHERS_DENIED.send(sender);
                        } else Message.NOT_PLAYER.send(sender);
                    }
                });
    }
}
