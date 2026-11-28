package io.th0rgal.oraxen.utils.actions;

import io.th0rgal.oraxen.OraxenPlugin;
import me.gabytm.util.actions.actions.Action;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;

public class ClickAction {

    private final List<String> conditions;
    private final List<Action<Player>> actions;

    private ClickAction(List<String> conditions, List<Action<Player>> actions) {
        this.conditions = conditions;
        this.actions = actions;
    }

    @SuppressWarnings("unchecked")
    private static ClickAction from(final LinkedHashMap<String, Object> config) {
        final List<String> conditions = (List<String>) config.getOrDefault("conditions", Collections.emptyList());

        final List<Action<Player>> actions =
                OraxenPlugin.get().getClickActionManager().parse(Player.class, (List<String>) config.getOrDefault("actions", Collections.emptyList()));

        // If the action doesn't have any actions, return null
        return actions.isEmpty() ? null : new ClickAction(conditions, actions);
    }

    public static ClickAction from(final ConfigurationSection config) {
        final List<String> conditions = config.getStringList("conditions");
        final List<Action<Player>> actions = OraxenPlugin.get().getClickActionManager().parse(Player.class, config.getStringList("actions"));

        // If the action doesn't have any actions, return null
        if (actions.isEmpty()) {
            return null;
        }

        return new ClickAction(conditions, actions);
    }

    @SuppressWarnings("unchecked")
    public static List<ClickAction> parseList(final ConfigurationSection section) {
        final List<LinkedHashMap<String, Object>> list = (List<LinkedHashMap<String, Object>>) section.getList("clickActions", Collections.emptyList());

        // Return an empty list if the clickActions list is null / empty
        if (list.isEmpty()) return Collections.emptyList();

        final List<ClickAction> clickActions = new ArrayList<>(list.size());

        // Parse each element of the list
        for (final LinkedHashMap<String, Object> actionConfig : list) {
            final ClickAction clickAction = ClickAction.from(actionConfig);

            if (clickAction != null) clickActions.add(clickAction);
        }

        return clickActions;
    }

    public boolean canRun(final Player player) {
        return ActionConditions.matches(player, conditions);
    }

    public void performActions(final Player player) {
        OraxenPlugin.get().getClickActionManager().runOrdered(player, actions);
    }

}
