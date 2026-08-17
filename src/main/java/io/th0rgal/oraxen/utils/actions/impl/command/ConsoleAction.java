package io.th0rgal.oraxen.utils.actions.impl.command;

import io.th0rgal.oraxen.utils.SchedulerUtil;
import me.gabytm.util.actions.actions.Action;
import me.gabytm.util.actions.actions.ActionMeta;
import me.gabytm.util.actions.actions.Context;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class ConsoleAction extends Action<Player> {

    public static final String IDENTIFIER = "console";

    public ConsoleAction(@NotNull ActionMeta<Player> meta) {
        super(meta);
    }

    @Override
    public void run(@NotNull Player player, @NotNull Context<Player> context) {
        // Folia requires console commands to be dispatched from the global region
        // thread; parse placeholders on the calling thread, then hop.
        String command = getMeta().getParsedData(player, context);
        SchedulerUtil.runTask(() -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command));
    }

}
