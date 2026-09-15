package io.th0rgal.oraxen.utils.commands;

import io.th0rgal.oraxen.utils.AdventureUtils;
import io.th0rgal.oraxen.utils.SchedulerUtil;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.stream.Collectors;

public class CommandsParser {

    private List<String> consoleCommands;
    private List<String> playerCommands;
    private List<String> oppedPlayerCommands;
    private boolean empty = false;

    public CommandsParser(ConfigurationSection section, TagResolver tagResolver) {
        if (section == null) {
            empty = true;
            return;
        }

        if (section.isList("console"))
            this.consoleCommands = section.getStringList("console").stream().map(s -> AdventureUtils.parseMiniMessage(s, tagResolver)).collect(Collectors.toList());

        if (section.isList("player"))
            this.playerCommands = section.getStringList("player");

        if (section.isList("opped_player"))
            this.oppedPlayerCommands = section.getStringList("opped_player");
    }

    public void perform(Player player) {
        if (empty) return;
        Runnable start = () -> runConsoleCommands(player, player.getName(), 0);
        if (Bukkit.isOwnedByCurrentRegion(player)) start.run();
        else SchedulerUtil.runForEntity(player, start);
    }

    private void runConsoleCommands(Player player, String playerName, int index) {
        if (consoleCommands == null || index >= consoleCommands.size()) {
            runPlayerCommands(player, playerName);
            return;
        }

        String parsed = consoleCommands.get(index).replace("%p%", playerName);
        SchedulerUtil.runTask(() -> {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), parsed);
            SchedulerUtil.runForEntity(player, () -> runConsoleCommands(player, playerName, index + 1));
        });
    }

    private void runPlayerCommands(Player player, String playerName) {
        if (playerCommands != null)
            for (String command : playerCommands)
                Bukkit.dispatchCommand(player, command.replace("%p%", playerName));

        if (oppedPlayerCommands == null) return;
        for (String command : oppedPlayerCommands) {
            boolean wasOp = player.isOp();
            player.setOp(true);
            try {
                Bukkit.dispatchCommand(player, command.replace("%p%", playerName));
            } finally {
                // Never leave the player permanently opped if the command throws.
                player.setOp(wasOp);
            }
        }
    }

}
