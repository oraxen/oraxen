package io.th0rgal.oraxen.utils.actions;

import io.th0rgal.oraxen.configs.Settings;
import io.th0rgal.oraxen.utils.logs.Logs;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.springframework.expression.EvaluationException;
import org.springframework.expression.Expression;
import org.springframework.expression.ParseException;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.SimpleEvaluationContext;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ActionConditions {

    private static final SpelExpressionParser PARSER = new SpelExpressionParser();
    private static final Map<String, Expression> EXPRESSIONS = new ConcurrentHashMap<>();
    // SpEL string literals, single or double quoted, with doubled quotes as escapes.
    private static final Pattern STRING_LITERAL = Pattern.compile("'(?:[^']|'')*'|\"(?:[^\"]|\"\")*\"");
    private static final Pattern COMPARISON = Pattern.compile("==|!=|>=|<=|>|<");
    private static final Pattern LOGICAL_OPERATOR = Pattern.compile("&&|\\|\\||(?i)\\b(?:and|or)\\b");

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
                Expression expression = EXPRESSIONS.computeIfAbsent(condition, c -> PARSER.parseExpression(normalize(c)));
                Boolean result = expression.getValue(context, Boolean.class);
                if (!Boolean.TRUE.equals(result)) return false;
            } catch (ParseException | EvaluationException exception) {
                Logs.logWarning("Failed to evaluate action condition '" + condition + "' for player " + player.getName() + "; blocking action.");
                if (Settings.DEBUG.toBool()) exception.printStackTrace();
                return false;
            }
        }
        return true;
    }

    /**
     * Lets conditions use {@code player.} and {@code server.} without the SpEL {@code #} prefix,
     * and reads a leading {@code !} on a single comparison as negating the comparison.
     * Quoted strings are left as written.
     */
    static String normalize(String condition) {
        String expression = rewriteOutsideStrings(condition.trim());
        String code = STRING_LITERAL.matcher(expression).replaceAll("''");
        if (expression.startsWith("!") && COMPARISON.matcher(code).find() && !LOGICAL_OPERATOR.matcher(code).find())
            expression = "!(" + expression.substring(1) + ")";
        return expression;
    }

    private static String rewriteOutsideStrings(String expression) {
        StringBuilder result = new StringBuilder();
        Matcher literal = STRING_LITERAL.matcher(expression);
        int start = 0;
        while (literal.find()) {
            result.append(rewriteCode(expression.substring(start, literal.start()))).append(literal.group());
            start = literal.end();
        }
        return result.append(rewriteCode(expression.substring(start))).toString();
    }

    private static String rewriteCode(String code) {
        return code.replaceAll("(?<![#\\w.])player\\.", "#player.")
                .replaceAll("(?<![#\\w.])server\\.", "#server.")
                .replaceAll("(?i)(?<=\\.)gamemode\\b", "gameMode");
    }
}
