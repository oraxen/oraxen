package io.th0rgal.oraxen.utils.commands;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.List;

public class CommandsParser {

    private List<String> consoleCommands;
    private List<String> playerCommands;
    private List<String> oppedPlayerCommands;
    private boolean empty = false;

    public CommandsParser(ConfigurationSection section) {
        if (section == null) {
            empty = true;
            return;
        }

        if (section.isList("console"))
            this.consoleCommands = section.getStringList("console");

        if (section.isList("player"))
            this.playerCommands = section.getStringList("player");

        if (section.isList("opped_player"))
            this.oppedPlayerCommands = section.getStringList("opped_player");
    }

    public void perform(Player player) {
        if (empty)
            return;
        String playerName = player.getName();

        if (consoleCommands != null)
            for (String command : consoleCommands)
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command.replace("%p%", playerName));

        if (playerCommands != null)
            for (String command : playerCommands)
                Bukkit.dispatchCommand(player, command.replace("%p%", playerName));

        if (oppedPlayerCommands != null)
            for (String command : oppedPlayerCommands) {
                boolean wasOp = player.isOp();
                player.setOp(true);
                Bukkit.dispatchCommand(player, command.replace("%p%", playerName));
                player.setOp(wasOp);
            }
    }

}
