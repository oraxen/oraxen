package io.th0rgal.oraxen.utils.actions;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.configs.Settings;
import io.th0rgal.oraxen.utils.logs.Logs;
import me.gabytm.util.actions.actions.Action;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.EvaluationException;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.ParseException;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.SimpleEvaluationContext;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;

public class ClickAction {

    private static final ExpressionParser PARSER = new SpelExpressionParser();

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
        if (conditions.isEmpty()) return true;
        if (actions.isEmpty()) return false;

        final EvaluationContext context = SimpleEvaluationContext.forReadOnlyDataBinding()
                .withInstanceMethods()
                .withRootObject(player)
                .build();
        context.setVariable("player", player);
        context.setVariable("server", org.bukkit.Bukkit.getServer());

        for (final String condition : conditions) {
            try {
                final Boolean result = PARSER.parseExpression(condition).getValue(context, Boolean.class);

                if (result == null || !result) {
                    return false;
                }
            } catch (ParseException | EvaluationException e) {
                Logs.logWarning("Failed to evaluate click action condition '" + condition + "' for player " + player.getName() + "; blocking action.");
                if (Settings.DEBUG.toBool()) e.printStackTrace();
                return false;
            }
        }

        return true;
    }

    public void performActions(final Player player) {
        OraxenPlugin.get().getClickActionManager().run(player, actions, false);
    }

}
