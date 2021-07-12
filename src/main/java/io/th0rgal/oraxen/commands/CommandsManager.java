package io.th0rgal.oraxen.commands;

import dev.jorel.commandapi.CommandAPICommand;
import org.bukkit.Bukkit;

public class CommandsManager {

    public void loadCommands() {
        new CommandAPICommand("oraxen")
                .withAliases("o", "oxn")
                .executes((sender, args) -> {
                    Bukkit.broadcastMessage("TEST");
                    sender.sendMessage("pong!");
                })
                .register();
    }

}
