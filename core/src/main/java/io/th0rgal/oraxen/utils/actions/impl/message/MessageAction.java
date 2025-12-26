package io.th0rgal.oraxen.utils.actions.impl.message;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.utils.AdventureUtils;
import me.gabytm.util.actions.actions.Action;
import me.gabytm.util.actions.actions.ActionMeta;
import me.gabytm.util.actions.actions.Context;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class MessageAction extends Action<Player> {

    public static final String IDENTIFIER = "message";

    public MessageAction(@NotNull ActionMeta<Player> meta) {
        super(meta);
    }

    @Override
    public void run(@NotNull Player player, @NotNull Context<Player> context) {
        final String text = getMeta().getParsedData(player, context);
        // Uses MiniMessage format (e.g., <gold>, <red>, <bold>)
        final Component message = AdventureUtils.MINI_MESSAGE.deserialize(text);
        OraxenPlugin.get().getAudience().player(player).sendMessage(message);
    }

}
