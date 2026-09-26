package io.th0rgal.oraxen.utils.actions;

import io.th0rgal.oraxen.configs.Settings;
import io.th0rgal.oraxen.utils.logs.Logs;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.springframework.expression.EvaluationException;
import org.springframework.expression.ParseException;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.SimpleEvaluationContext;

import java.util.List;

public final class ActionConditions {

    private static final SpelExpressionParser PARSER = new SpelExpressionParser();

    private ActionConditions() {
    }

    public static boolean matches(Player player, List<String> conditions) {
        if (conditions.isEmpty()) return true;

        var context = SimpleEvaluationContext.forReadOnlyDataBinding()
                .withInstanceMethods()
                .withRootObject(player)
                .build();
        context.setVariable("player", player);
        context.setVariable("server", Bukkit.getServer());

        for (String condition : conditions) {
            try {
                String expression = condition.trim().replaceAll("(?<![#\\w])player\\.", "#player.")
                        .replaceAll("(?<![#\\w])server\\.", "#server.")
                        .replaceAll("(?i)(?<=\\.)gamemode\\b", "gameMode");
                // Treat a leading ! on a comparison as negating the whole check.
                if (expression.startsWith("!") && expression.matches(".*(?:==|!=|>=|<=|>|<).*"))
                    expression = "!(" + expression.substring(1) + ")";
                Boolean result = PARSER.parseExpression(expression).getValue(context, Boolean.class);
                if (!Boolean.TRUE.equals(result)) return false;
            } catch (ParseException | EvaluationException exception) {
                Logs.logWarning("Failed to evaluate action condition '" + condition + "' for player " + player.getName() + "; blocking action.");
                if (Settings.DEBUG.toBool()) exception.printStackTrace();
                return false;
            }
        }
        return true;
    }
}
