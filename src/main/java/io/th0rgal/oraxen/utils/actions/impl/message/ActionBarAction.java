package io.th0rgal.oraxen.utils.actions.impl.message;

import io.th0rgal.oraxen.OraxenPlugin;
import me.gabytm.util.actions.actions.Action;
import me.gabytm.util.actions.actions.ActionMeta;
import me.gabytm.util.actions.actions.Context;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class ActionBarAction extends Action<Player> {

    public static final String IDENTIFIER = "actionbar";

    public ActionBarAction(@NotNull ActionMeta<Player> meta) {
        super(meta);
    }

    @Override
    public void run(@NotNull Player player, @NotNull Context<Player> context) {
        final Component message = LegacyComponentSerializer.legacySection().deserialize(getMeta().getParsedData(player, context));
        OraxenPlugin.get().getAudience().player(player).sendActionBar(message);
    }

}
