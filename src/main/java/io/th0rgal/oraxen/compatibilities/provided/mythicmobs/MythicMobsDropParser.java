package io.th0rgal.oraxen.compatibilities.provided.mythicmobs;

import java.util.regex.Pattern;

final class MythicMobsDropParser {

    private static final Pattern AMOUNT_PATTERN = Pattern.compile(
            "-?(?:\\d+(?:\\.\\d+)?|\\.\\d+)(?:(?:-|to)-?(?:\\d+(?:\\.\\d+)?|\\.\\d+))?");

    private MythicMobsDropParser() {
    }

    static String getItemId(String[] lines) {
        return switch (lines.length) {
            case 4 -> lines[1];
            case 3 -> isAmount(lines[2]) ? lines[1] : lines[2];
            default -> "";
        };
    }

    private static boolean isAmount(String token) {
        return AMOUNT_PATTERN.matcher(token).matches();
    }
}
