package io.th0rgal.oraxen.utils.actions;

import io.th0rgal.oraxen.utils.actions.impl.command.ConsoleAction;
import io.th0rgal.oraxen.utils.actions.impl.command.PlayerAction;
import io.th0rgal.oraxen.utils.actions.impl.message.ActionBarAction;
import io.th0rgal.oraxen.utils.actions.impl.message.MessageAction;
import io.th0rgal.oraxen.utils.actions.impl.other.SoundAction;
import me.gabytm.util.actions.placeholders.PlaceholderProvider;
import me.gabytm.util.actions.spigot.actions.SpigotActionManager;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

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
