package io.th0rgal.oraxen.pack.dispatch;

import io.th0rgal.oraxen.configs.Settings;
import io.th0rgal.oraxen.utils.MinecraftVersion;
import io.th0rgal.oraxen.utils.logs.Logs;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Applies {@code Pack.dispatch.exclude} client-version limits before sending the resource pack.
 */
public final class PackDispatchFilter {

    private static final Pattern RULE_PATTERN = Pattern.compile("^\\s*([<=>])\\s*([0-9]+(?:\\.[0-9]+)*)\\s*$");
    private static final Map<String, Integer> PROTOCOL_BY_VERSION = createProtocolMap();
    private static final NavigableMap<Integer, ClientVersion> VERSION_BY_PROTOCOL = createProtocolReverseMap();
    private static final Object cacheLock = new Object();
    private static volatile List<String> cachedRuleSource = null;
    private static volatile List<VersionLimit> cachedLimits = List.of();

    private PackDispatchFilter() {
    }

    /**
     * Returns {@code true} when the configured stop list does not exclude the player's client version.
     */
    public static boolean canSendPack(Player player) {
        List<VersionLimit> limits = getConfiguredLimits();
        if (limits.isEmpty()) return true;

        Integer protocolVersion = PlayerVersionDetector.getPlayerProtocolVersion(player);
        String fallbackVersion = protocolVersion != null
                ? getVersionStringForProtocol(protocolVersion)
                : MinecraftVersion.getCurrentVersion().getVersion();

        boolean canSend = canSendPack(protocolVersion, fallbackVersion, limits);
        if (!canSend && Settings.DEBUG.toBool()) {
            Logs.logInfo("Skipping resource pack for " + player.getName()
                    + " (client: " + describe(protocolVersion, fallbackVersion) + ") due to Pack.dispatch.exclude");
        }
        return canSend;
    }

    /**
     * Returns {@code true} when the configured stop list does not exclude a pre-join connection.
     */
    public static boolean canSendPackForConnection(Object connection) {
        List<VersionLimit> limits = getConfiguredLimits();
        if (limits.isEmpty()) return true;

        Integer protocolVersion = resolveProtocolVersion(connection);
        return canSendPack(protocolVersion, null, limits);
    }

    /**
     * Attempts to resolve a protocol version from a Paper pre-join/configuration connection.
     */
    @Nullable
    public static Integer resolveProtocolVersion(@Nullable Object connection) {
        if (connection == null) return null;
        return findProtocolVersion(connection, new IdentityHashMap<>(), 0);
    }

    private static List<VersionLimit> getConfiguredLimits() {
        List<String> rules = List.copyOf(Settings.DISPATCH_EXCLUDE.toStringList());
        List<String> currentSource = cachedRuleSource;
        if (Objects.equals(currentSource, rules)) {
            return cachedLimits;
        }

        synchronized (cacheLock) {
            if (!Objects.equals(cachedRuleSource, rules)) {
                cachedLimits = parseRules(rules, true);
                cachedRuleSource = rules;
            }
            return cachedLimits;
        }
    }

    static boolean canSendPack(@Nullable Integer protocolVersion, @Nullable String fallbackVersion,
                               Collection<String> stopRules) {
        return canSendPack(protocolVersion, fallbackVersion, parseRules(stopRules, false));
    }

    private static boolean canSendPack(@Nullable Integer protocolVersion, @Nullable String fallbackVersion,
                                       List<VersionLimit> limits) {
        if (limits.isEmpty()) return true;
        if (protocolVersion != null && protocolVersion <= 0) protocolVersion = null;

        ClientVersion clientVersion = protocolVersion != null
                ? getVersionForProtocol(protocolVersion).orElseGet(() -> ClientVersion.parse(fallbackVersion).orElse(null))
                : ClientVersion.parse(fallbackVersion).orElseGet(() -> currentServerVersion().orElse(null));

        for (VersionLimit limit : limits) {
            if (limit.matches(protocolVersion, clientVersion)) {
                return false;
            }
        }
        return true;
    }

    private static List<VersionLimit> parseRules(Collection<String> rules, boolean logWarnings) {
        if (rules == null || rules.isEmpty()) return List.of();

        List<VersionLimit> limits = new ArrayList<>();
        for (String rule : rules) {
            if (rule == null || rule.isBlank()) continue;

            Matcher matcher = RULE_PATTERN.matcher(rule);
            if (!matcher.matches()) {
                logInvalidRule(rule, logWarnings);
                continue;
            }

            Operator operator = Operator.fromSymbol(matcher.group(1));
            String version = matcher.group(2);
            Optional<ClientVersion> targetVersion = ClientVersion.parse(version);
            if (operator == null || targetVersion.isEmpty()) {
                logInvalidRule(rule, logWarnings);
                continue;
            }

            limits.add(new VersionLimit(operator, targetVersion.get(), findProtocolForVersion(version)));
        }
        return List.copyOf(limits);
    }

    private static void logInvalidRule(String rule, boolean logWarnings) {
        if (!logWarnings) return;
        Logs.logWarning("Ignoring invalid Pack.dispatch.exclude rule '" + rule + "'. Use '< 1.12', '= 1.21.11' or '> 1.21.11'.");
    }

    @Nullable
    private static Integer findProtocolForVersion(String version) {
        ClientVersion clientVersion = ClientVersion.parse(version).orElse(null);
        if (clientVersion == null) return null;

        ClientVersion candidate = clientVersion;
        while (candidate != null) {
            Integer protocol = PROTOCOL_BY_VERSION.get(candidate.key());
            if (protocol != null) return protocol;
            candidate = candidate.withoutTrailingZero();
        }
        return null;
    }

    private static Optional<ClientVersion> getVersionForProtocol(int protocolVersion) {
        Map.Entry<Integer, ClientVersion> entry = VERSION_BY_PROTOCOL.floorEntry(protocolVersion);
        return entry != null ? Optional.of(entry.getValue()) : Optional.empty();
    }

    private static String getVersionStringForProtocol(int protocolVersion) {
        return getVersionForProtocol(protocolVersion)
                .map(ClientVersion::key)
                .orElseGet(() -> PlayerVersionDetector.protocolToVersionString(protocolVersion));
    }

    private static Optional<ClientVersion> currentServerVersion() {
        try {
            return ClientVersion.parse(MinecraftVersion.getCurrentVersion().getVersion());
        } catch (Throwable ignored) {
            return Optional.empty();
        }
    }

    private static String describe(@Nullable Integer protocolVersion, @Nullable String fallbackVersion) {
        if (protocolVersion != null) {
            return getVersionStringForProtocol(protocolVersion) + " / protocol " + protocolVersion;
        }
        return fallbackVersion != null ? fallbackVersion : "unknown";
    }

    @Nullable
    private static Integer findProtocolVersion(Object object, IdentityHashMap<Object, Boolean> visited, int depth) {
        if (object == null || depth > 4 || visited.containsKey(object)) return null;
        visited.put(object, Boolean.TRUE);

        if (object instanceof Number number) {
            return number.intValue();
        }

        Integer directMethodResult = tryProtocolMethods(object);
        if (directMethodResult != null) return directMethodResult;

        Integer directFieldResult = tryProtocolFields(object);
        if (directFieldResult != null) return directFieldResult;

        for (String fieldName : List.of("handle", "listener", "connection", "networkManager", "networkmanager", "network")) {
            Object nested = getFieldValue(object, fieldName);
            Integer nestedResult = findProtocolVersion(nested, visited, depth + 1);
            if (nestedResult != null) return nestedResult;
        }

        return null;
    }

    @Nullable
    private static Integer tryProtocolMethods(Object object) {
        for (String methodName : List.of("getProtocolVersion", "protocolVersion")) {
            Method method = getNoArgMethod(object.getClass(), methodName);
            Integer result = invokeIntegerMethod(object, method);
            if (result != null) return result;
        }
        return null;
    }

    @Nullable
    private static Integer tryProtocolFields(Object object) {
        Object value = getFieldValue(object, "protocolVersion");
        return value instanceof Number number ? number.intValue() : null;
    }

    @Nullable
    private static Method getNoArgMethod(Class<?> type, String name) {
        Class<?> current = type;
        while (current != null) {
            try {
                Method method = current.getDeclaredMethod(name);
                if (method.getParameterCount() == 0) {
                    method.setAccessible(true);
                    return method;
                }
            } catch (ReflectiveOperationException ignored) {
            }
            current = current.getSuperclass();
        }

        try {
            Method method = type.getMethod(name);
            if (method.getParameterCount() == 0) {
                method.setAccessible(true);
                return method;
            }
        } catch (ReflectiveOperationException ignored) {
        }
        return null;
    }

    @Nullable
    private static Integer invokeIntegerMethod(Object object, @Nullable Method method) {
        if (method == null) return null;
        try {
            Object value = method.invoke(object);
            return value instanceof Number number ? number.intValue() : null;
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            return null;
        }
    }

    @Nullable
    private static Object getFieldValue(Object object, String fieldName) {
        Class<?> current = object.getClass();
        while (current != null) {
            try {
                Field field = current.getDeclaredField(fieldName);
                field.setAccessible(true);
                return field.get(object);
            } catch (ReflectiveOperationException | RuntimeException ignored) {
            }
            current = current.getSuperclass();
        }
        return null;
    }

    private static Map<String, Integer> createProtocolMap() {
        Map<String, Integer> protocols = new HashMap<>();

        put(protocols, 776, "26.2", "26.2.0", "1.26.2", "1.26.2.0");
        put(protocols, 775, "26.1", "26.1.1", "26.1.2", "1.26.1", "1.26.1.1", "1.26.1.2");
        put(protocols, 774, "1.21.11");
        put(protocols, 773, "1.21.9", "1.21.10");
        put(protocols, 772, "1.21.7", "1.21.8");
        put(protocols, 771, "1.21.6");
        put(protocols, 770, "1.21.5");
        put(protocols, 769, "1.21.4");
        put(protocols, 768, "1.21.2", "1.21.3");
        put(protocols, 767, "1.21", "1.21.0", "1.21.1");
        put(protocols, 766, "1.20.5", "1.20.6");
        put(protocols, 765, "1.20.3", "1.20.4");
        put(protocols, 764, "1.20.2");
        put(protocols, 763, "1.20", "1.20.0", "1.20.1");
        put(protocols, 762, "1.19.4");
        put(protocols, 761, "1.19.3");
        put(protocols, 760, "1.19.1", "1.19.2");
        put(protocols, 759, "1.19", "1.19.0");
        put(protocols, 758, "1.18.2");
        put(protocols, 757, "1.18", "1.18.0", "1.18.1");
        put(protocols, 756, "1.17.1");
        put(protocols, 755, "1.17", "1.17.0");
        put(protocols, 754, "1.16.4", "1.16.5");
        put(protocols, 753, "1.16.3");
        put(protocols, 751, "1.16.2");
        put(protocols, 736, "1.16.1");
        put(protocols, 735, "1.16", "1.16.0");
        put(protocols, 578, "1.15.2");
        put(protocols, 575, "1.15.1");
        put(protocols, 573, "1.15", "1.15.0");
        put(protocols, 498, "1.14.4");
        put(protocols, 490, "1.14.3");
        put(protocols, 485, "1.14.2");
        put(protocols, 480, "1.14.1");
        put(protocols, 477, "1.14", "1.14.0");
        put(protocols, 404, "1.13.2");
        put(protocols, 401, "1.13.1");
        put(protocols, 393, "1.13", "1.13.0");
        put(protocols, 340, "1.12.2");
        put(protocols, 338, "1.12.1");
        put(protocols, 335, "1.12", "1.12.0");
        put(protocols, 316, "1.11.1", "1.11.2");
        put(protocols, 315, "1.11", "1.11.0");
        put(protocols, 210, "1.10", "1.10.0", "1.10.1", "1.10.2");
        put(protocols, 110, "1.9.3", "1.9.4");
        put(protocols, 109, "1.9.2");
        put(protocols, 108, "1.9.1");
        put(protocols, 107, "1.9", "1.9.0");
        put(protocols, 47, "1.8", "1.8.0", "1.8.1", "1.8.2", "1.8.3", "1.8.4", "1.8.5", "1.8.6", "1.8.7", "1.8.8", "1.8.9");
        put(protocols, 5, "1.7.6", "1.7.7", "1.7.8", "1.7.9", "1.7.10");
        put(protocols, 4, "1.7.2", "1.7.3", "1.7.4", "1.7.5");

        return Map.copyOf(protocols);
    }

    private static void put(Map<String, Integer> protocols, int protocol, String... versions) {
        for (String version : versions) {
            ClientVersion.parse(version).ifPresent(clientVersion -> protocols.put(clientVersion.key(), protocol));
        }
    }

    private static NavigableMap<Integer, ClientVersion> createProtocolReverseMap() {
        Map<Integer, ClientVersion> bestVersions = new HashMap<>();
        for (Map.Entry<String, Integer> entry : PROTOCOL_BY_VERSION.entrySet()) {
            ClientVersion version = ClientVersion.parse(entry.getKey()).orElse(null);
            if (version == null) continue;
            bestVersions.merge(entry.getValue(), version, PackDispatchFilter::preferredCanonicalVersion);
        }

        TreeMap<Integer, ClientVersion> versions = new TreeMap<>();
        versions.putAll(bestVersions);
        return versions;
    }

    private static ClientVersion preferredCanonicalVersion(ClientVersion current, ClientVersion candidate) {
        int comparison = current.compareTo(candidate);
        if (comparison != 0) return comparison > 0 ? current : candidate;

        int lengthComparison = Integer.compare(current.parts().size(), candidate.parts().size());
        if (lengthComparison != 0) return lengthComparison < 0 ? current : candidate;

        return current.key().compareTo(candidate.key()) <= 0 ? current : candidate;
    }

    private enum Operator {
        LESS('<'),
        EQUAL('='),
        GREATER('>');

        private final char symbol;

        Operator(char symbol) {
            this.symbol = symbol;
        }

        @Nullable
        private static Operator fromSymbol(String symbol) {
            if (symbol == null || symbol.length() != 1) return null;
            char value = symbol.charAt(0);
            for (Operator operator : values()) {
                if (operator.symbol == value) return operator;
            }
            return null;
        }

        private boolean matches(int comparison) {
            return switch (this) {
                case LESS -> comparison < 0;
                case EQUAL -> comparison == 0;
                case GREATER -> comparison > 0;
            };
        }
    }

    private record VersionLimit(Operator operator, ClientVersion targetVersion, @Nullable Integer targetProtocol) {

        private boolean matches(@Nullable Integer clientProtocol, @Nullable ClientVersion clientVersion) {
            if (clientProtocol != null && targetProtocol != null) {
                return operator.matches(Integer.compare(clientProtocol, targetProtocol));
            }

            if (clientVersion == null) return false;
            return operator.matches(clientVersion.compareTo(targetVersion));
        }
    }

    private record ClientVersion(List<Integer> parts) implements Comparable<ClientVersion> {

        private static Optional<ClientVersion> parse(@Nullable String rawVersion) {
            if (rawVersion == null) return Optional.empty();

            String version = rawVersion.trim().toLowerCase(Locale.ROOT);
            if (version.startsWith("(mc:") && version.endsWith(")")) {
                version = version.substring(4, version.length() - 1).trim();
            }
            if (version.endsWith("+")) {
                version = version.substring(0, version.length() - 1).trim();
            }

            if (!version.matches("[0-9]+(?:\\.[0-9]+)*")) {
                return Optional.empty();
            }

            String[] split = version.split("\\.");
            List<Integer> parsedParts = new ArrayList<>(split.length);
            for (String part : split) {
                try {
                    parsedParts.add(Integer.parseInt(part));
                } catch (NumberFormatException ignored) {
                    return Optional.empty();
                }
            }
            return Optional.of(new ClientVersion(List.copyOf(parsedParts)));
        }

        @Override
        public int compareTo(@NotNull ClientVersion other) {
            int max = Math.max(parts.size(), other.parts.size());
            for (int i = 0; i < max; i++) {
                int left = i < parts.size() ? parts.get(i) : 0;
                int right = i < other.parts.size() ? other.parts.get(i) : 0;
                int comparison = Integer.compare(left, right);
                if (comparison != 0) return comparison;
            }
            return 0;
        }

        private String key() {
            StringBuilder builder = new StringBuilder();
            for (int i = 0; i < parts.size(); i++) {
                if (i > 0) builder.append('.');
                builder.append(parts.get(i));
            }
            return builder.toString();
        }

        @Nullable
        private ClientVersion withoutTrailingZero() {
            if (parts.size() <= 1 || parts.get(parts.size() - 1) != 0) return null;
            return new ClientVersion(List.copyOf(parts.subList(0, parts.size() - 1)));
        }
    }
}
