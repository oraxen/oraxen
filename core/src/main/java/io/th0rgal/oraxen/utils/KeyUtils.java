package io.th0rgal.oraxen.utils;

import io.th0rgal.oraxen.utils.logs.Logs;
import net.kyori.adventure.key.Key;

import java.util.Locale;

public class KeyUtils {

    public static final Key MALFORMED_KEY_PLACEHOLDER = Key.key("item/barrier");

    public static Key dropExtension(Key key) {
        return dropExtension(key.asString());
    }

    public static Key dropExtension(String key) {
        if (!Key.parseable(key)) return MALFORMED_KEY_PLACEHOLDER;
        int i = key.lastIndexOf(".");
        if (i == -1) return Key.key(key);
        else return Key.key(key.substring(0, i));
    }

    public static void parseKey(String namespace, String key, String prefix) {
        if (!Key.parseable(String.format("%s:%s", namespace, key))) {
            String oldNamespace = namespace, oldKey = key;
            namespace = namespace.toLowerCase(Locale.ENGLISH).replaceAll("[^a-z0-9._/-]", "_");
            key = key.toLowerCase(Locale.ENGLISH).replaceAll("[^a-z0-9._/-]", "_");
            Logs.logError(String.format("Invalid %s-key: %s:%s", prefix, namespace, key));
            Logs.logWarning("Keys must be all lower-case, without spaces and most special characters");
            Logs.logWarning(String.format("Example: %s:%s -> %s:%s", oldNamespace, oldKey, oldNamespace, key));
        }
    }
}
