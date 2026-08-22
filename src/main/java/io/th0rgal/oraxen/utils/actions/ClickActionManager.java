package io.th0rgal.oraxen.utils.actions;

import io.th0rgal.oraxen.utils.SchedulerUtil;
import io.th0rgal.oraxen.utils.actions.impl.command.ConsoleAction;
import io.th0rgal.oraxen.utils.actions.impl.command.PlayerAction;
import io.th0rgal.oraxen.utils.actions.impl.message.ActionBarAction;
import io.th0rgal.oraxen.utils.actions.impl.message.MessageAction;
import io.th0rgal.oraxen.utils.actions.impl.other.SoundAction;
import me.gabytm.util.actions.actions.Action;
import me.gabytm.util.actions.actions.Context;
import me.gabytm.util.actions.placeholders.PlaceholderProvider;
import me.gabytm.util.actions.spigot.actions.SpigotActionManager;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

public class ClickActionManager extends SpigotActionManager {

    public ClickActionManager(@NotNull JavaPlugin plugin) {
        // The default SpigotTaskProcessor uses the legacy BukkitScheduler for delayed
        // and async actions, which throws UnsupportedOperationException on Folia.
        // Note: the (TaskProcessor, maxChance) super constructor already registers
        // the PlaceholderAPI provider when PlaceholderAPI is enabled.
        super(new RegionAwareTaskProcessor(), 100D);
        registerDefaults(Player.class);
        getComponentParser().registerDefaults(Player.class);

        // Placeholders
        getPlaceholderManager().register(new PlayerNamePlaceholderProvider());
        //-----

        // Commands
        register(Player.class, ConsoleAction.IDENTIFIER, ConsoleAction::new);
        register(Player.class, PlayerAction.IDENTIFIER, PlayerAction::new);
        //-----

        // Messages
        register(Player.class, ActionBarAction.IDENTIFIER, ActionBarAction::new);
        register(Player.class, MessageAction.IDENTIFIER, MessageAction::new);
        //-----

        // Other
        register(Player.class, SoundAction.IDENTIFIER, SoundAction::new);
        //-----
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> void run(T target, List<Action<T>> actions, boolean async, Map<String, Object> data) {
        if (target instanceof Player player && !async) {
            List<Action<Player>> playerActions = (List<Action<Player>>) (List<?>) actions;
            runOrdered(player, playerActions, new Context<>(playerActions, data), 0);
            return;
        }
        super.run(target, actions, async, data);
    }

    public void runOrdered(Player player, List<Action<Player>> actions) {
        runOrdered(player, actions, new Context<>(actions, Map.of()), 0);
    }

    private void runOrdered(Player player, List<Action<Player>> actions, Context<Player> context, int index) {
        if (index >= actions.size()) return;

        Action<Player> action = actions.get(index);
        if (action.getMeta().hasChance()
                && ThreadLocalRandom.current().nextDouble(100D) > action.getMeta().getChance()) {
            runOrdered(player, actions, context, index + 1);
            return;
        }

        Runnable execute = () -> {
            if (action instanceof ConsoleAction consoleAction) {
                String command = consoleAction.parseCommand(player, context);
                SchedulerUtil.runTask(() -> {
                    consoleAction.dispatch(command);
                    SchedulerUtil.runForEntity(player,
                            () -> runOrdered(player, actions, context, index + 1));
                });
                return;
            }
            action.run(player, context);
            runOrdered(player, actions, context, index + 1);
        };

        if (action.getMeta().hasDelay())
            SchedulerUtil.runForEntityLater(player, action.getMeta().getDelay(), execute);
        else if (org.bukkit.Bukkit.isOwnedByCurrentRegion(player)) execute.run();
        else SchedulerUtil.runForEntity(player, execute);
    }

    private static class PlayerNamePlaceholderProvider implements PlaceholderProvider<Player> {

        @Override
        public @NotNull Class<Player> getType() {
            return Player.class;
        }

        @Override
        public @NotNull String replace(@NotNull Player player, @NotNull String input) {
            return StringUtils.replace(input, "<player>", player.getName());
        }

    }

}
