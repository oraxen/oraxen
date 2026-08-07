package io.th0rgal.oraxen.configs;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

final class YamlCommentCopier {

    private YamlCommentCopier() {
    }

    static void setWithComments(@NotNull YamlConfiguration target, @NotNull YamlConfiguration source,
            @NotNull String path) {
        setWithComments(target, source, path, null);
    }

    static void setWithComments(@NotNull YamlConfiguration target, @NotNull YamlConfiguration source,
            @NotNull String path, @Nullable String sourceYaml) {
        target.set(path, source.get(path));
        copyComments(target, source, path, sourceYaml);
    }

    private static void copyComments(@NotNull YamlConfiguration target, @NotNull YamlConfiguration source,
            @NotNull String path, @Nullable String sourceYaml) {
        copyPathComments(target, source, path, sourceYaml);

        Object value = source.get(path);
        if (!(value instanceof ConfigurationSection section))
            return;

        for (String childKey : section.getKeys(true))
            copyPathComments(target, source, path + "." + childKey, sourceYaml);
    }

    private static void copyPathComments(@NotNull YamlConfiguration target, @NotNull YamlConfiguration source,
            @NotNull String path, @Nullable String sourceYaml) {
        RawPathComments rawComments = sourceYaml != null ? findRawPathComments(sourceYaml, path) : RawPathComments.EMPTY;

        List<String> comments = source.getComments(path);
        if (comments.isEmpty())
            comments = rawComments.comments();

        List<String> inlineComments = source.getInlineComments(path);
        if (inlineComments.isEmpty())
            inlineComments = rawComments.inlineComments();

        Object value = source.get(path);
        if (!inlineComments.isEmpty() && (value instanceof ConfigurationSection || value instanceof List<?>)) {
            List<String> blockComments = new ArrayList<>(comments);
            blockComments.addAll(inlineComments);
            target.setComments(path, blockComments);
            return;
        }

        if (!comments.isEmpty())
            target.setComments(path, comments);
        if (!inlineComments.isEmpty())
            target.setInlineComments(path, inlineComments);
    }

    private static RawPathComments findRawPathComments(@NotNull String yaml, @NotNull String path) {
        String[] lines = yaml.split("\\R", -1);
        Deque<PathElement> stack = new ArrayDeque<>();

        for (int index = 0; index < lines.length; index++) {
            String line = lines[index];
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("#") || trimmed.startsWith("-"))
                continue;

            int indent = countLeadingSpaces(line);
            int separatorIndex = findUnquoted(line, ':');
            if (separatorIndex < indent)
                continue;

            String key = unquote(line.substring(indent, separatorIndex).trim());
            if (key.isEmpty())
                continue;

            while (!stack.isEmpty() && stack.peekLast().indent() >= indent)
                stack.removeLast();
            stack.addLast(new PathElement(indent, key));

            if (currentPath(stack).equals(path))
                return new RawPathComments(extractBlockComments(lines, index), extractInlineComment(line));
        }

        return RawPathComments.EMPTY;
    }

    private static List<String> extractBlockComments(String[] lines, int lineIndex) {
        List<String> comments = new ArrayList<>();
        for (int index = lineIndex - 1; index >= 0; index--) {
            String line = lines[index];
            String trimmed = line.trim();
            if (trimmed.isEmpty()) {
                comments.add(0, "");
                continue;
            }
            if (!trimmed.startsWith("#"))
                break;

            String comment = line.substring(line.indexOf('#') + 1);
            if (comment.startsWith(" "))
                comment = comment.substring(1);
            comments.add(0, comment);
        }

        while (!comments.isEmpty() && comments.get(0).isEmpty())
            comments.remove(0);
        return comments;
    }

    private static List<String> extractInlineComment(@NotNull String line) {
        int commentIndex = findUnquoted(line, '#');
        if (commentIndex < 0)
            return List.of();

        String comment = line.substring(commentIndex + 1);
        if (comment.startsWith(" "))
            comment = comment.substring(1);
        return List.of(comment);
    }

    private static int findUnquoted(@NotNull String line, char searchedChar) {
        boolean inSingleQuotes = false;
        boolean inDoubleQuotes = false;
        for (int index = 0; index < line.length(); index++) {
            char currentChar = line.charAt(index);
            if (currentChar == '\\' && inDoubleQuotes) {
                index++;
                continue;
            }
            if (currentChar == '\'' && !inDoubleQuotes) {
                if (inSingleQuotes && index + 1 < line.length() && line.charAt(index + 1) == '\'') {
                    index++;
                    continue;
                }
                inSingleQuotes = !inSingleQuotes;
                continue;
            }
            if (currentChar == '"' && !inSingleQuotes) {
                inDoubleQuotes = !inDoubleQuotes;
                continue;
            }
            if (currentChar == searchedChar && !inSingleQuotes && !inDoubleQuotes)
                return index;
        }
        return -1;
    }

    private static int countLeadingSpaces(@NotNull String line) {
        int count = 0;
        while (count < line.length() && line.charAt(count) == ' ')
            count++;
        return count;
    }

    private static String unquote(@NotNull String key) {
        if (key.length() < 2)
            return key;
        if ((key.startsWith("\"") && key.endsWith("\"")) || (key.startsWith("'") && key.endsWith("'")))
            return key.substring(1, key.length() - 1);
        return key;
    }

    private static String currentPath(@NotNull Deque<PathElement> stack) {
        StringBuilder path = new StringBuilder();
        for (PathElement element : stack) {
            if (!path.isEmpty())
                path.append('.');
            path.append(element.key());
        }
        return path.toString();
    }

    private record PathElement(int indent, String key) {
    }

    private record RawPathComments(List<String> comments, List<String> inlineComments) {
        private static final RawPathComments EMPTY = new RawPathComments(List.of(), List.of());
    }
}
