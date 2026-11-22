package io.th0rgal.oraxen.pack.generation;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.utils.logs.Logs;
import org.bukkit.configuration.file.YamlConfiguration;

import java.util.ArrayList;
import java.util.List;
import java.util.WeakHashMap;
import java.util.function.Consumer;
import java.util.regex.Pattern;

/** Immutable, platform-independent exclusions for one resource pack build. */
final class PackExclusions {
    private static final WeakHashMap<YamlConfiguration, Boolean> diagnosedConfigurations = new WeakHashMap<>();
    private final List<Pattern> rules;

    private PackExclusions(List<Pattern> rules) {
        this.rules = List.copyOf(rules);
    }

    static PackExclusions load() {
        YamlConfiguration settings = OraxenPlugin.get().getConfigsManager().getSettings();
        boolean diagnose;
        synchronized (diagnosedConfigurations) {
            diagnose = diagnosedConfigurations.put(settings, Boolean.TRUE) == null;
        }
        return compile(settings.get("pack.import.exclude"), message -> {
            if (diagnose) Logs.logWarning(message);
        });
    }

    static PackExclusions compile(Object configured, Consumer<String> warning) {
        List<Pattern> rules = new ArrayList<>();
        List<Object> invalid = new ArrayList<>();
        if (configured instanceof List<?> entries) {
            for (Object entry : entries) {
                if (!(entry instanceof String glob) || glob.isBlank() || glob.startsWith("!")
                        || glob.chars().anyMatch(c -> "{}[]\\".indexOf(c) >= 0)) {
                    invalid.add(entry);
                    continue;
                }
                StringBuilder regex = new StringBuilder();
                for (int i = 0; i < glob.length(); i++) {
                    char c = glob.charAt(i);
                    if (c == '*') {
                        if (i + 1 < glob.length() && glob.charAt(i + 1) == '*') {
                            i++;
                            if (i + 1 < glob.length() && glob.charAt(i + 1) == '/') {
                                regex.append("(?:.*/)?");
                                i++;
                            } else regex.append(".*");
                        } else regex.append("[^/]*");
                    } else if (c == '?') regex.append("[^/]");
                    else regex.append(Pattern.quote(String.valueOf(c)));
                }
                rules.add(Pattern.compile(regex.toString(), Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE));
            }
        } else if (configured != null) invalid.add(configured);
        if (!invalid.isEmpty()) warning.accept("Skipped invalid pack.import.exclude rules. Use / separators and only *, ? and ** wildcards. Invalid entries " + invalid);
        return new PackExclusions(rules);
    }

    boolean matches(String path) {
        String normalized = path.replace('\\', '/');
        return rules.stream().anyMatch(rule -> rule.matcher(normalized).matches());
    }
}
