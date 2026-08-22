package io.th0rgal.oraxen.utils.actions.impl.command;

import io.th0rgal.oraxen.utils.SchedulerUtil;
import me.gabytm.util.actions.actions.Action;
import me.gabytm.util.actions.actions.ActionMeta;
import me.gabytm.util.actions.actions.Context;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class PlayerAction extends Action<Player> {

    public static final String IDENTIFIER = "player";

    public PlayerAction(@NotNull ActionMeta<Player> meta) {
        super(meta);
    }

    @Override
    public void run(@NotNull Player player, @NotNull Context<Player> context) {
        Runnable command = () -> player.chat('/' + getMeta().getParsedData(player, context));
        if (Bukkit.isOwnedByCurrentRegion(player)) command.run();
        else SchedulerUtil.runForEntity(player, command);
    }

}
