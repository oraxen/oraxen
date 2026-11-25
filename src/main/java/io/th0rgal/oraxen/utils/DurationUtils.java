package io.th0rgal.oraxen.utils;

import java.util.Locale;
import java.util.OptionalLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class DurationUtils {

    private static final Pattern DURATION = Pattern.compile("([0-9]+)\\s*(ms|s|m|h|d)", Pattern.CASE_INSENSITIVE);

    private DurationUtils() {
    }

    public static OptionalLong parseMillis(String value) {
        if (value == null) return OptionalLong.empty();
        Matcher matcher = DURATION.matcher(value.trim());
        if (!matcher.matches()) return OptionalLong.empty();

        long multiplier = switch (matcher.group(2).toLowerCase(Locale.ROOT)) {
            case "ms" -> 1L;
            case "s" -> 1_000L;
            case "m" -> 60_000L;
            case "h" -> 3_600_000L;
            case "d" -> 86_400_000L;
            default -> throw new IllegalStateException("Unknown duration unit");
        };
        try {
            return OptionalLong.of(Math.multiplyExact(Long.parseLong(matcher.group(1)), multiplier));
        } catch (ArithmeticException | NumberFormatException exception) {
            return OptionalLong.empty();
        }
    }
}
